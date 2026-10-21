#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
: "${BUILD_NUMBER:?请在 Jenkins 构建任务内执行}"
: "${DEPLOY_ENV_FILE:?请绑定生产 Secret file 凭据}"
test -r "$DEPLOY_ENV_FILE"
export DEPLOY_IMAGE="cy-pregnancy-admin:${BUILD_NUMBER}"
export SERVICE_NETWORK="${SERVICE_NETWORK:-cy-pregnancy-internal}"
java -version
mvn --version
docker version
docker compose version
# 保留单元测试；数据库集成测试仅在单独测试环境显式启用。
mvn -B -ntp clean verify
docker build -t "$DEPLOY_IMAGE" .
if ! docker network inspect "$SERVICE_NETWORK" >/dev/null 2>&1; then
    docker network create "$SERVICE_NETWORK"
fi
# 不打印 config 展开结果，避免日志泄露生产凭据。
docker compose config --quiet
previous_image="$(docker inspect --format '{{.Config.Image}}' admin-api 2>/dev/null || true)"
if docker compose up -d --wait --wait-timeout 240; then
    echo "部署成功：$DEPLOY_IMAGE"
else
    echo "部署失败，请在受控服务器终端检查应用日志"
    if [ -n "$previous_image" ]; then
        echo "尝试恢复上一版镜像"
        DEPLOY_IMAGE="$previous_image" docker compose up -d --wait --wait-timeout 240 || true
    fi
    exit 1
fi
