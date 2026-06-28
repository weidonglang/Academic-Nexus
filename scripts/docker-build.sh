#!/usr/bin/env sh
set -eu

MAVEN_MIRROR="${MAVEN_MIRROR:-central}"
SERVICE="${SERVICE:-}"
NO_CACHE="${NO_CACHE:-false}"
CUSTOM_MIRROR_URL="${CUSTOM_MIRROR_URL:-}"

case "$MAVEN_MIRROR" in
  central) MAVEN_MIRROR_URL="https://repo.maven.apache.org/maven2" ;;
  aliyun) MAVEN_MIRROR_URL="https://maven.aliyun.com/repository/public" ;;
  tencent) MAVEN_MIRROR_URL="https://mirrors.cloud.tencent.com/nexus/repository/maven-public/" ;;
  huawei) MAVEN_MIRROR_URL="https://repo.huaweicloud.com/repository/maven/" ;;
  custom)
    if [ -z "$CUSTOM_MIRROR_URL" ]; then
      echo "CUSTOM_MIRROR_URL is required when MAVEN_MIRROR=custom" >&2
      exit 1
    fi
    MAVEN_MIRROR_URL="$CUSTOM_MIRROR_URL"
    ;;
  *) echo "Unsupported MAVEN_MIRROR: $MAVEN_MIRROR" >&2; exit 1 ;;
esac

docker info >/dev/null
docker compose version >/dev/null

if [ ! -f .env ]; then
  echo ".env not found. Copy it first: cp .env.example .env" >&2
fi

export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
export MAVEN_MIRROR_URL

set -- compose build "$@"
if [ "$NO_CACHE" = "true" ]; then
  set -- "$@" --no-cache
fi
if [ -n "$SERVICE" ]; then
  set -- "$@" "$SERVICE"
fi

echo "Using Maven mirror: $MAVEN_MIRROR_URL"
if ! docker "$@"; then
  echo "Docker build failed. For Maven bad_record_mac / Central transfer failures try:" >&2
  echo "  MAVEN_MIRROR=aliyun ./scripts/docker-build.sh" >&2
  echo "  docker builder prune" >&2
  echo "  docker compose build --no-cache academic-main" >&2
  exit 1
fi
