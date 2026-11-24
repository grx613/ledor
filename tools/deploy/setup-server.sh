#!/usr/bin/env bash
# Разовая настройка сервера под лендинг LeDor. Запускать с sudo:  sudo bash tools/deploy/setup-server.sh
# Что делает: создаёт /var/www/kalendar (владелец gr), добавляет блок kalendar.bothub.chat в Caddyfile
# (с резервной копией и проверкой конфигурации ДО применения) и перезагружает Caddy без простоя.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
CADDYFILE=/etc/caddy/Caddyfile
OWNER="${SUDO_USER:-gr}"

install -d -o "$OWNER" -g "$OWNER" -m 755 /var/www/kalendar

if grep -q '^kalendar\.bothub\.chat' "$CADDYFILE"; then
  echo "Блок kalendar.bothub.chat уже есть в $CADDYFILE — пропускаю."
else
  TMP="$(mktemp)"
  cat "$CADDYFILE" "$HERE/kalendar.caddy" > "$TMP"
  caddy validate --config "$TMP" --adapter caddyfile
  cp "$CADDYFILE" "$CADDYFILE.bak-$(date +%F-%H%M)"
  install -m 644 "$TMP" "$CADDYFILE"
  rm -f "$TMP"
  systemctl reload caddy
  echo "Caddy перезагружен с новым блоком kalendar.bothub.chat."
fi
