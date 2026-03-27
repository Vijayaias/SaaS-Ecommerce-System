#!/usr/bin/env bash

set -euo pipefail

APP_NAME="${APP_NAME:-stripe-payment}"
APP_JAR="${APP_JAR:-app.jar}"
APP_PORT="${APP_PORT:-8080}"
APP_DIR="${APP_DIR:-/opt/${APP_NAME}}"
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/deployments/${APP_NAME}}"
SERVICE_NAME="${APP_NAME}.service"
RUNTIME_USER="${SUDO_USER:-$(whoami)}"
ENV_FILE="${ENV_FILE:-/etc/${APP_NAME}.env}"
LOG_DIR="${LOG_DIR:-/var/log/${APP_NAME}}"

sudo mkdir -p "${APP_DIR}"
sudo chown -R "${RUNTIME_USER}:${RUNTIME_USER}" "${APP_DIR}"
sudo mkdir -p "${LOG_DIR}"
sudo chown -R "${RUNTIME_USER}:${RUNTIME_USER}" "${LOG_DIR}"

cp "${DEPLOY_DIR}/${APP_JAR}" "${APP_DIR}/app.jar"

cat <<EOF | sudo tee "/etc/systemd/system/${SERVICE_NAME}" >/dev/null
[Unit]
Description=${APP_NAME} Spring Boot Application
After=network.target

[Service]
Type=simple
User=${RUNTIME_USER}
WorkingDirectory=${APP_DIR}
EnvironmentFile=-${ENV_FILE}
Environment=SPRING_PROFILES_ACTIVE=prod
ExecStart=/usr/bin/java -jar ${APP_DIR}/app.jar --server.port=${APP_PORT}
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable "${SERVICE_NAME}"
sudo systemctl restart "${SERVICE_NAME}"
sudo systemctl status "${SERVICE_NAME}" --no-pager
