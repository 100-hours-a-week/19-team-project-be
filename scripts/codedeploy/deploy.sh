#!/usr/bin/env bash
set -e

DEPLOY_DIR="/home/ubuntu"
DEPLOY_ENV="$DEPLOY_DIR/.env"
RUNTIME_ENV_DST="/etc/refit/.env"
RUNTIME_ENV_SRC="$DEPLOY_DIR/refit.env"

log_info() { echo "[INFO] $*"; }
log_ok()   { echo "[OK] $*"; }
log_err()  { echo "[ERROR] $*" >&2; }

run_before_install() {
  log_info "BeforeInstall 시작"
  if ! command -v aws &>/dev/null; then
    curl -sSfL "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o /tmp/awscliv2.zip
    unzip -q -o /tmp/awscliv2.zip -d /tmp && /tmp/aws/install
    rm -rf /tmp/awscliv2.zip /tmp/aws
  fi
  if ! command -v jq &>/dev/null; then
    apt-get update -qq && apt-get install -y -qq jq
  fi
  log_ok "BeforeInstall 완료"
}

run_after_install() {
  log_info "AfterInstall 시작"

  if [ -f "$RUNTIME_ENV_SRC" ]; then
    local dst_dir
    dst_dir="$(dirname "$RUNTIME_ENV_DST")"
    mkdir -p "$dst_dir"
    chown root:docker "$dst_dir"
    chmod 710 "$dst_dir"
    cp "$RUNTIME_ENV_SRC" "$RUNTIME_ENV_DST"
    chown root:docker "$RUNTIME_ENV_DST"
    chmod 640 "$RUNTIME_ENV_DST"
    rm -f "$RUNTIME_ENV_SRC"
  fi

  [ -f "$DEPLOY_ENV" ] && chmod 600 "$DEPLOY_ENV"

  log_ok "AfterInstall 완료"
}

run_application_start() {
  log_info "ApplicationStart 시작"

  [ ! -f "$DEPLOY_DIR/docker-compose.yml" ] && { log_err "compose 파일 없음"; exit 1; }

  if [ -f "$DEPLOY_ENV" ]; then
    set -a
    # shellcheck source=/dev/null
    source "$DEPLOY_ENV"
    set +a
  fi

  ECR_REGISTRY="${ECR_REGISTRY:?필수 설정 누락}"
  AWS_REGION="${AWS_REGION:?필수 설정 누락}"

  aws ecr get-login-password --region "$AWS_REGION" | \
    docker login --username AWS --password-stdin "$ECR_REGISTRY" || { log_err "레지스트리 인증 실패"; exit 1; }

  cd "$DEPLOY_DIR"
  docker compose pull backend
  docker compose up -d --remove-orphans
  log_ok "ApplicationStart 완료"
}

run_validate_service() {
  log_info "ValidateService 시작"
  local retries=5 delay=5
  for i in $(seq 1 "$retries"); do
    status=$(timeout 5 curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health" 2>/dev/null || echo "000")
    [ "$status" = "200" ] && { log_ok "서비스 정상"; return 0; }
    log_info "헬스체크 $i/$retries (HTTP $status)"
    [ "$i" -lt "$retries" ] && sleep "$delay"
  done
  log_err "서비스 응답 없음"
  return 1
}

case "${LIFECYCLE_EVENT:-}" in
  BeforeInstall)    run_before_install ;;
  AfterInstall)     run_after_install ;;
  ApplicationStart) run_application_start ;;
  ValidateService)  run_validate_service ;;
  *) log_err "알 수 없는 이벤트: ${LIFECYCLE_EVENT}"; exit 1 ;;
esac
