#!/bin/bash
set -e

SERVER="root@116.203.133.236"
ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "==> Upload frontend..."
ssh -p 22 "$SERVER" "find /opt/gdrsheet/frontend/dist -mindepth 1 -delete"
scp -P 22 -r "$ROOT/frontend/dist/." "$SERVER:/opt/gdrsheet/frontend/dist/"
ssh -p 22 "$SERVER" "find /opt/gdrsheet/frontend/dist -type d -exec chmod 755 {} +"

echo "==> Upload backend + restart..."
scp -P 22 "$ROOT/backend/target/backend-0.0.1-SNAPSHOT.jar" "$SERVER:/opt/gdrsheet/backend/target/app.jar"
ssh -p 22 "$SERVER" "DOCKER_API_VERSION=1.43 docker restart gdr_java"

echo "Deploy completato!"
