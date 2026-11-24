#!/usr/bin/env bash
# Выкладка сайта: website/ → /var/www/kalendar (без sudo после setup-server.sh). Тесты конвертера — перед выкладкой.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
node --test "$ROOT"/website/tests/*.test.mjs >/dev/null
rsync -a --delete --exclude tests/ "$ROOT/website/" /var/www/kalendar/
echo "Опубликовано: https://kalendar.bothub.chat/"
