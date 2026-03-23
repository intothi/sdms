#!/bin/bash
# ============================================================
# SDMS Raspberry Pi Setup Script
# Voraussetzung: Raspberry Pi OS Lite, SSH aktiviert, User "pi"
# ============================================================

set -e  # Abbruch bei Fehler

echo "===== SDMS Setup startet ====="

# ── System updaten ───────────────────────────────────────────
echo "[1/6] System aktualisieren..."
sudo apt update -q
sudo apt dist-upgrade -y -q

# ── Java installieren ────────────────────────────────────────
echo "[2/6] Java installieren..."
sudo apt install -y -q default-jdk

# ── MariaDB installieren und konfigurieren ───────────────────
echo "[3/6] MariaDB installieren..."
sudo apt install -y -q mariadb-server

echo "MariaDB absichern (mysql_secure_installation)..."
# Automatisierte sichere Grundkonfiguration
sudo mysql -e "DELETE FROM mysql.user WHERE User='';"
sudo mysql -e "DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');"
sudo mysql -e "DROP DATABASE IF EXISTS test;"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "Datenbank und Benutzer anlegen..."
sudo mysql -e "CREATE USER IF NOT EXISTS 'dmsUser'@'localhost' IDENTIFIED BY '12345';"
sudo mysql -e "CREATE DATABASE IF NOT EXISTS dmsDb;"
sudo mysql -e "GRANT ALL PRIVILEGES ON dmsDb.* TO 'dmsUser'@'localhost';"

# Remote-Zugriff für Entwicklung (DataGrip/DBeaver vom PC)
sudo mysql -e "CREATE USER IF NOT EXISTS 'dmsUserRemote'@'%' IDENTIFIED BY '12345';"
sudo mysql -e "GRANT ALL PRIVILEGES ON dmsDb.* TO 'dmsUserRemote'@'%';"
sudo mysql -e "FLUSH PRIVILEGES;"

# MariaDB remote-fähig machen
echo "[mysqld]" | sudo tee -a /etc/mysql/my.cnf
echo "bind-address = ::" | sudo tee -a /etc/mysql/my.cnf
sudo systemctl restart mariadb

# ── Apache2 installieren ─────────────────────────────────────
echo "[4/6] Apache2 installieren..."
sudo apt install -y -q apache2

# Mod Rewrite aktivieren (für SPA falls nötig)
sudo a2enmod rewrite

# SDMS Frontend-Verzeichnis vorbereiten
sudo mkdir -p /var/www/html/sdms

# Apache Berechtigungen
sudo chown -R www-data:www-data /var/www/html/sdms

# Apache beim Booten aktivieren
sudo systemctl enable --now apache2

# ── SDMS Backend Verzeichnis vorbereiten ─────────────────────
echo "[5/6] Backend-Verzeichnis vorbereiten..."
mkdir -p /home/pi/sdms

# ── systemd Service für Backend ──────────────────────────────
echo "[6/6] systemd Service einrichten..."
sudo tee /etc/systemd/system/sdms.service > /dev/null << 'SERVICE'
[Unit]
Description=SDMS Backend
After=network.target mariadb.service

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/sdms
ExecStart=/usr/bin/java -Xmx256m -jar /home/pi/sdms/sdms.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
SERVICE

sudo systemctl daemon-reload
sudo systemctl enable sdms

echo ""
echo "===== Setup abgeschlossen ====="
echo ""
echo "Backend starten:  sudo systemctl start sdms"
echo "Backend Status:   sudo systemctl status sdms"
echo "Backend Logs:     journalctl -u sdms -f"
