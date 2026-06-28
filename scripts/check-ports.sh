#!/usr/bin/env sh
set -eu

read_env_port() {
  name="$1"
  default="$2"
  eval "value=\${$name:-}"
  if [ -n "$value" ]; then
    echo "$value"
    return
  fi
  if [ -f .env ]; then
    value="$(grep -E "^$name=" .env | head -n 1 | cut -d= -f2- || true)"
    if [ -n "$value" ]; then
      echo "$value"
      return
    fi
  fi
  echo "$default"
}

check_port() {
  name="$1"
  default="$2"
  port="$(read_env_port "$name" "$default")"
  if command -v lsof >/dev/null 2>&1; then
    pid="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | tr '\n' ',' | sed 's/,$//')"
  else
    pid="$(netstat -anp 2>/dev/null | grep "[.:]$port " | awk '{print $7}' | cut -d/ -f1 | sort -u | tr '\n' ',' | sed 's/,$//' || true)"
  fi
  if [ -n "$pid" ]; then
    echo "$name=$port is occupied by PID $pid. Change $name in .env."
  else
    echo "$name=$port is free."
  fi
}

check_port MAIN_HOST_PORT 8088
check_port FRONTEND_HOST_PORT 5174
check_port MYSQL_HOST_PORT 13306
check_port REDIS_HOST_PORT 16379
check_port NACOS_HOST_PORT 18848
check_port NACOS_GRPC_HOST_PORT 19848
check_port AI_SERVICE_HOST_PORT 18090
