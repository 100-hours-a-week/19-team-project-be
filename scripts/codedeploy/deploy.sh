#!/usr/bin/env bash
# CodeDeploy 백엔드 배포 스크립트 (ECR + docker-compose)
# CodeDeploy가 훅마다 이 스크립트를 호출하며 LIFECYCLE_EVENT 로 구분함.

set -e

CONFIG_DIR="/etc/refit"
DEPLOY_DIR="/home/ubuntu"
COMPOSE_ENV_FILE="/home/ubuntu/.env"

log_info()  { echo "[INFO] $*"; }
log_ok()    { echo "[OK] $*"; }
log_err()   { echo "[ERROR] $*" >&2; }

# ----- BeforeInstall: 의존성 확인 -----
run_before_install() {
  log_info "BeforeInstall: 의존성 확인 중..."
  if ! command -v aws &>/dev/null; then
    log_info "AWS CLI 설치 중..."
    curl -sSfL "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o /tmp/awscliv2.zip
    unzip -q -o /tmp/awscliv2.zip -d /tmp && sudo /tmp/aws/install
    rm -rf /tmp/awscliv2.zip /tmp/aws
  fi
  if ! command -v jq &>/dev/null; then
    sudo apt-get update -qq && sudo apt-get install -y -qq jq
  fi
  log_ok "BeforeInstall 완료"
}

# ----- AfterInstall: 리비전의 .env 파일을 서버에 복사 -----
run_after_install() {
  log_info "AfterInstall: 배포 설정 반영 중..."
  
  # 리비전에 포함된 .env 파일을 docker-compose용 경로로 복사
  # - docker-compose.yml에서는 상대경로 ./.env 를 사용 (경로 하드코딩 최소화)
  if [ -f "$DEPLOY_DIR/.env" ]; then
    ENV_FILE="$COMPOSE_ENV_FILE"
    sudo cp "$DEPLOY_DIR/.env" "$ENV_FILE"
    sudo chown ubuntu:ubuntu "$ENV_FILE" 2>/dev/null || true
    sudo chmod 600 "$ENV_FILE" 2>/dev/null || true
    log_ok ".env 파일 복사 완료: $ENV_FILE"
  else
    log_info "리비전에 .env 파일 없음, 기존 .env 유지"
  fi
  log_ok "AfterInstall 완료"
}

# ----- ApplicationStart: ECR 로그인 후 docker compose 기동 -----
run_application_start() {
  log_info "ApplicationStart: Docker Compose 기동 중..."

  if [ ! -f "$DEPLOY_DIR/docker-compose.yml" ]; then
    log_err "docker-compose.yml 없음: $DEPLOY_DIR/docker-compose.yml"
    exit 1
  fi

  # ECR 로그인 (compose .env에서 레지스트리/리전 읽음)
  if [ -f "$COMPOSE_ENV_FILE" ]; then
    set -a
    # shellcheck source=/dev/null
    source "$COMPOSE_ENV_FILE"
    set +a
  fi
  ECR_REGISTRY="${ECR_REGISTRY:?ECR_REGISTRY가 .env에 없습니다}"
  AWS_REGION="${AWS_REGION:?AWS_REGION이 .env에 없습니다}"

  log_info "ECR 로그인: $ECR_REGISTRY"
  aws ecr get-login-password --region "$AWS_REGION" | \
    docker login --username AWS --password-stdin "$ECR_REGISTRY" || { log_err "ECR 로그인 실패"; exit 1; }

  cd "$DEPLOY_DIR"
  export CONFIG_DIR
  docker compose pull backend
  docker compose up -d
  log_ok "ApplicationStart 완료"
}

# ----- ValidateService: 헬스체크 -----
run_validate_service() {
  log_info "ValidateService: 헬스체크 중..."
  
  HEALTH_URL="http://localhost:8080/actuator/health"
  RETRIES=5
  DELAY=5
  
  for i in $(seq 1 $RETRIES); do
    HTTP_STATUS=$(timeout 5 curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
    if [ "$HTTP_STATUS" = "200" ]; then
      log_ok "헬스체크 성공! (HTTP $HTTP_STATUS)"
      return 0
    fi
    log_info "시도 $i/$RETRIES: HTTP $HTTP_STATUS"
    [ $i -lt $RETRIES ] && sleep $DELAY
  done
  
  log_err "헬스체크 실패"
  return 1
}

# ----- LIFECYCLE_EVENT에 따라 해당 단계만 실행 -----
case "${LIFECYCLE_EVENT:-}" in
  BeforeInstall)   run_before_install ;;
  AfterInstall)    run_after_install ;;
  ApplicationStart) run_application_start ;;
  ValidateService) run_validate_service ;;
  *) log_err "알 수 없는 LIFECYCLE_EVENT: $LIFECYCLE_EVENT"; exit 1 ;;
esac
