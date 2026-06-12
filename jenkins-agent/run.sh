#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-jenkins-python-ssh}"
CONTAINER_NAME="${CONTAINER_NAME:-jenkins-agent}"
SSH_PORT="${SSH_PORT:-2222}"
PORT_8080="${PORT_8080:-8080}"
PORT_8081="${PORT_8081:-8081}"

# Remove existing container with the same name if present
if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  docker rm -f "${CONTAINER_NAME}"
fi

exec docker run -d \
  -p "${SSH_PORT}:22" \
  -p "${PORT_8080}:8080" \
  -p "${PORT_8081}:8081" \
  --name "${CONTAINER_NAME}" \
  "${IMAGE_NAME}"
