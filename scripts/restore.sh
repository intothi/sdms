#!/bin/bash
# ============================================================
# Author Christoph Thum
#
# AI Usage Disclosure: This document was created with assistance from AI tools.
# The content has been reviewed and edited by a human.
# For more information on the extent and nature of AI usage, please contact the author.
#
# SDMS Restore Script
# Verwendung: ./restore.sh [backup_datei.tar.gz]
# Ohne Argument: neuestes Backup im selben Verzeichnis
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="$SCRIPT_DIR/sdmsBackup"
DB_NAME="dmsDb"
DB_USER="dmsUser"
DB_PW="12345"
DOCS_DIR="/home/pi/sdms"
SERVICE_NAME="sdms"
TEMP_DIR="/tmp/sdms_restore_$$"

# ── Backup-Datei bestimmen ───────────────────────────────────
if [ -n "$1" ]; then
    BACKUP_FILE="$1"
else
    BACKUP_FILE=$(ls -t "$BACKUP_DIR"/sdms_backup_*.tar.gz 2>/dev/null | head -n 1)
    if [ -z "$BACKUP_FILE" ]; then
        echo "FEHLER: Kein Backup gefunden in $BACKUP_DIR"
        exit 1
    fi
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "FEHLER: Datei nicht gefunden: $BACKUP_FILE"
    exit 1
fi

echo "===== SDMS Restore startet ====="
echo "Backup:  $BACKUP_FILE"
echo "Ziel DB: $DB_NAME"
echo "Ziel FS: $DOCS_DIR"
echo ""
read -p "Fortfahren? (j/N): " CONFIRM
if [ "$CONFIRM" != "j" ] && [ "$CONFIRM" != "J" ]; then
    echo "Abgebrochen."
    exit 0
fi

# ── Service stoppen ──────────────────────────────────────────
echo "[1/5] SDMS Service stoppen..."
sudo systemctl stop "$SERVICE_NAME" || true

# ── Entpacken ────────────────────────────────────────────────
echo "[2/5] Backup entpacken..."
mkdir -p "$TEMP_DIR"
tar -xzf "$BACKUP_FILE" -C "$TEMP_DIR"

# ── Datenbank einspielen ─────────────────────────────────────
echo "[3/5] Datenbank wiederherstellen..."
if [ ! -f "$TEMP_DIR/sdms_dump.sql" ]; then
    echo "FEHLER: sdms_dump.sql nicht im Archiv gefunden."
    rm -rf "$TEMP_DIR"
    sudo systemctl start "$SERVICE_NAME"
    exit 1
fi
mysql -u "$DB_USER" -p"$DB_PW" "$DB_NAME" < "$TEMP_DIR/sdms_dump.sql"
echo "Datenbank eingespielt."

# ── Dokumente wiederherstellen ───────────────────────────────
echo "[4/5] Dokumente wiederherstellen..."
if [ -d "$TEMP_DIR/documents" ]; then
    mkdir -p "$DOCS_DIR"
    cp -f "$TEMP_DIR"/documents/* "$DOCS_DIR"/
    COUNT=$(ls "$TEMP_DIR/documents" | wc -l)
    echo "$COUNT Datei(en) kopiert nach $DOCS_DIR"
else
    echo "Keine Dokumente im Archiv gefunden, übersprungen."
fi

# ── Aufräumen ────────────────────────────────────────────────
echo "[5/5] Temporäre Dateien löschen..."
rm -rf "$TEMP_DIR"

# ── Service starten ──────────────────────────────────────────
sudo systemctl start "$SERVICE_NAME"
echo ""
echo "===== Restore abgeschlossen ====="
echo "Service Status: sudo systemctl status $SERVICE_NAME"
