#!/bin/bash
# ============================================================
# Author Christoph Thum
#
# AI Usage Disclosure: This document was created with assistance from AI tools.
# The content has been reviewed and edited by a human.
# For more information on the extent and nature of AI usage, please contact the author.
#
# SDMS Deploy Script
# Aufruf: ./deploy.sh
# Voraussetzung: SSH-Key auf dem Pi hinterlegt
# ============================================================

set -e

PI_USER="pi"
PI_HOST="192.168.178.47"
PI_BACKEND_DIR="/home/pi/sdms"
PI_FRONTEND_DIR="/var/www/html/sdms"
BACKEND_DIR="/home/intothi/dev/sdms"
FRONTEND_DIR="/home/intothi/dev/sdms-frontend-vue-claude" # Pfad zum Frontend anpassen
JAR_NAME="sdms.jar"

echo "===== SDMS Deploy startet ====="

# ── Backend bauen ────────────────────────────────────────────
echo "[1/3] Backend bauen..."
cd $BACKEND_DIR
mvn clean package -q -DskipTests
JAR_PATH="target/sdms-1.0-SNAPSHOT-jar-with-dependencies.jar"
echo "JAR: $JAR_PATH"
cd -

# ── Backend übertragen ───────────────────────────────────────
echo "[2/3] Backend übertragen..."
scp $BACKEND_DIR/$JAR_PATH $PI_USER@$PI_HOST:$PI_BACKEND_DIR/$JAR_NAME

# ── Frontend übertragen ──────────────────────────────────────
echo "[3/3] Frontend übertragen..."
scp -r $FRONTEND_DIR/* $PI_USER@$PI_HOST:$PI_FRONTEND_DIR/

# ── Backend neu starten ──────────────────────────────────────
echo "Backend neu starten..."
ssh $PI_USER@$PI_HOST "sudo systemctl restart sdms"
ssh $PI_USER@$PI_HOST "sudo systemctl status sdms --no-pager"

echo ""
echo "===== Deploy abgeschlossen ====="
