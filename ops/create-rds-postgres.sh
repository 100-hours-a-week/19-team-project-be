#!/usr/bin/env bash
#
# Private Subnet에 PostgreSQL RDS 생성
# - 기존 private subnet 자동 탐색 (이름에 'private' 포함 또는 태그)
# - DB Subnet Group, Security Group 생성 후 RDS 인스턴스 생성
#
# 사용법:
#   export AWS_PROFILE=your-profile   # 필요시
#   ./create-rds-postgres.sh
#
# 옵션 (환경변수):
#   SUBNET_IDS     - 사용할 subnet ID (공백 구분, 최소 2개 권장). 미설정 시 private subnet 자동 탐색
#   VPC_ID         - VPC ID. 미설정 시 subnet에서 자동 추론
#   DB_INSTANCE    - RDS 식별자 (기본: refit-postgres)
#   DB_NAME        - 초기 DB 이름 (기본: refit)
#   MASTER_USER    - 마스터 사용자 (기본: refit_admin)
#   MASTER_PASS    - 마스터 비밀번호 (미설정 시 스크립트가 생성해 출력)
#   REGION         - 리전 (기본: ap-northeast-2)
#
set -euo pipefail

REGION="${REGION:-ap-northeast-2}"
DB_INSTANCE_ID="${DB_INSTANCE:-refit-postgres}"
DB_NAME="${DB_NAME:-refit}"
MASTER_USER="${MASTER_USER:-refit_admin}"
# 비밀번호는 20자 이상, 영대소문자+숫자 조합 권장
MASTER_PASS="${MASTER_PASS:-$(openssl rand -base64 24 | tr -dc 'a-zA-Z0-9' | head -c 24)}"
DB_SUBNET_GROUP_NAME="${DB_INSTANCE_ID}-subnet-group"
RDS_SG_NAME="${DB_INSTANCE_ID}-sg"

echo "=== RDS PostgreSQL 생성 (리전: ${REGION}) ==="

# 1) Subnet 결정
if [ -n "${SUBNET_IDS:-}" ]; then
  echo "Using provided SUBNET_IDS: $SUBNET_IDS"
  SUBNET_ARR=($SUBNET_IDS)
else
  echo "Discovering private subnets (Name tag contains 'private' or Type=Private)..."
  # 이름/Type 태그에 private 포함된 서브넷만 사용 (대소문자 무시)
  RAW=$(aws ec2 describe-subnets --region "$REGION" --output text \
    --query "Subnets[].[SubnetId,Tags[?Key=='Name'].Value | [0], Tags[?Key=='Type'].Value | [0]]" 2>/dev/null || true)
  SUBNET_ARR=()
  echo "$RAW" | while read -r sid name type; do
    name_lower=$(echo "${name:-}" | tr '[:upper:]' '[:lower:]')
    type_lower=$(echo "${type:-}" | tr '[:upper:]' '[:lower:]')
    if [[ "$name_lower" == *"private"* ]] || [[ "$type_lower" == "private" ]]; then
      echo "$sid"
    fi
  done > /tmp/refit_rds_subnets_$$.txt
  while IFS= read -r line; do
    [ -n "$line" ] && SUBNET_ARR+=("$line")
  done < /tmp/refit_rds_subnets_$$.txt 2>/dev/null || true
  rm -f /tmp/refit_rds_subnets_$$.txt
  if [ ${#SUBNET_ARR[@]} -eq 0 ]; then
    echo "No private subnets found. Listing all subnets:"
    aws ec2 describe-subnets --region "$REGION" --query "Subnets[].[SubnetId,AvailabilityZone,CidrBlock,Tags[?Key=='Name'].Value|[0]]" --output table
    echo "Set SUBNET_IDS (space-separated, at least 2 for HA) and re-run. Example:"
    echo "  export SUBNET_IDS='subnet-xxx subnet-yyy'"
    exit 1
  fi
  echo "Found subnets: ${SUBNET_ARR[*]}"
fi

if [ ${#SUBNET_ARR[@]} -lt 1 ]; then
  echo "At least 1 subnet required."
  exit 1
fi

# VPC ID (첫 서브넷 기준)
VPC_ID="${VPC_ID:-}"
if [ -z "$VPC_ID" ]; then
  VPC_ID=$(aws ec2 describe-subnets --region "$REGION" --subnet-ids "${SUBNET_ARR[0]}" --query "Subnets[0].VpcId" --output text)
  echo "Using VPC: $VPC_ID"
fi

# 2) DB Subnet Group 생성 (이미 있으면 스킵)
if aws rds describe-db-subnet-groups --region "$REGION" --db-subnet-group-name "$DB_SUBNET_GROUP_NAME" --query "DBSubnetGroups[0].DBSubnetGroupName" --output text 2>/dev/null | grep -q "$DB_SUBNET_GROUP_NAME"; then
  echo "DB Subnet Group already exists: $DB_SUBNET_GROUP_NAME"
else
  echo "Creating DB Subnet Group: $DB_SUBNET_GROUP_NAME"
  aws rds create-db-subnet-group \
    --region "$REGION" \
    --db-subnet-group-name "$DB_SUBNET_GROUP_NAME" \
    --db-subnet-group-description "Subnet group for $DB_INSTANCE_ID" \
    --subnet-ids "${SUBNET_ARR[@]}"
fi

# 3) RDS용 Security Group (5432 허용, VPC CIDR 또는 같은 SG)
RDS_SG_ID=""
if aws ec2 describe-security-groups --region "$REGION" --filters "Name=group-name,Values=$RDS_SG_NAME" "Name=vpc-id,Values=$VPC_ID" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null | grep -q sg-; then
  RDS_SG_ID=$(aws ec2 describe-security-groups --region "$REGION" --filters "Name=group-name,Values=$RDS_SG_NAME" "Name=vpc-id,Values=$VPC_ID" --query "SecurityGroups[0].GroupId" --output text)
  echo "Security Group already exists: $RDS_SG_ID"
else
  echo "Creating Security Group: $RDS_SG_NAME"
  RDS_SG_ID=$(aws ec2 create-security-group \
    --region "$REGION" \
    --vpc-id "$VPC_ID" \
    --group-name "$RDS_SG_NAME" \
    --description "RDS PostgreSQL for $DB_INSTANCE_ID" \
    --output text --query "GroupId")
  # VPC CIDR에서 5432 허용 (같은 VPC 내 EC2 등 접근)
  VPC_CIDR=$(aws ec2 describe-vpcs --region "$REGION" --vpc-ids "$VPC_ID" --query "Vpcs[0].CidrBlock" --output text)
  aws ec2 authorize-security-group-ingress \
    --region "$REGION" \
    --group-id "$RDS_SG_ID" \
    --protocol tcp --port 5432 --cidr "$VPC_CIDR"
  echo "Ingress rule added: 5432 from $VPC_CIDR"
fi

# 4) RDS 인스턴스 생성 (이미 있으면 스킵)
if aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$DB_INSTANCE_ID" --query "DBInstances[0].DBInstanceIdentifier" --output text 2>/dev/null | grep -q "$DB_INSTANCE_ID"; then
  echo "RDS instance already exists: $DB_INSTANCE_ID"
  echo "Endpoint: $(aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$DB_INSTANCE_ID" --query "DBInstances[0].Endpoint.Address" --output text 2>/dev/null || echo 'N/A')"
  echo "Saved password was set at creation. If you need to rotate, do it in AWS Console."
  exit 0
fi

echo "Creating RDS PostgreSQL instance: $DB_INSTANCE_ID"
echo "Instance class: db.t4g.micro (1 vCPU, 1GB – 적당한 소규모)"
echo "Engine: PostgreSQL 16"

aws rds create-db-instance \
  --region "$REGION" \
  --db-instance-identifier "$DB_INSTANCE_ID" \
  --db-instance-class "db.t4g.micro" \
  --engine "postgres" \
  --engine-version "16.4" \
  --master-username "$MASTER_USER" \
  --master-user-password "$MASTER_PASS" \
  --allocated-storage 20 \
  --storage-type gp3 \
  --db-name "$DB_NAME" \
  --vpc-security-group-ids "$RDS_SG_ID" \
  --db-subnet-group-name "$DB_SUBNET_GROUP_NAME" \
  --no-publicly-accessible \
  --storage-encrypted \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "sun:04:00-sun:05:00" \
  --no-multi-az

echo ""
echo "=== RDS 생성 요청 완료 (생성에는 수 분 소요) ==="
echo "DB Instance Identifier: $DB_INSTANCE_ID"
echo "Master Username:        $MASTER_USER"
echo "Master Password:       $MASTER_PASS"
echo "Initial DB Name:       $DB_NAME"
echo ""
echo "Endpoint (생성 후 확인):"
echo "  aws rds describe-db-instances --region $REGION --db-instance-identifier $DB_INSTANCE_ID --query 'DBInstances[0].Endpoint.Address' --output text"
echo ""
echo "JDBC URL (엔드포인트 확정 후):"
echo "  jdbc:postgresql://<Endpoint>:5432/$DB_NAME"
echo ""
echo "비밀번호는 위 출력만 저장해 두세요. 재실행 시 MASTER_PASS 미설정이면 새 값이 생성됩니다."
