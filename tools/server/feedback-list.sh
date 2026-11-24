#!/usr/bin/env bash
# Последние отзывы LeDor:  sudo bash tools/server/feedback-list.sh [сколько, по умолчанию 20]
set -euo pipefail
N="${1:-20}"; [[ "$N" =~ ^[0-9]+$ ]] || { echo "Число, пожалуйста."; exit 1; }
sudo -u postgres psql -X -d ledor -P pager=off -x -c \
  "select id, created_at, message, contact, app_version, device, os_version, locale from app.feedback order by id desc limit $N"
