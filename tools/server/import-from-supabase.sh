#!/usr/bin/env bash
# Перенос облачных списков из Supabase на свой сервер (один раз, по желанию).
#   sudo bash tools/server/import-from-supabase.sh 'postgresql://postgres.<ref>:<пароль>@<host>:5432/postgres'
# Строку подключения взять в панели Supabase: Connect → Session pooler. Пароль в историю shell не сохраняется,
# если перед командой поставить пробел. Переносятся списки, участники и события; соль IP и лимиты — нет.
set -euo pipefail
[ "$(id -u)" = 0 ] || { echo "Запустите через sudo."; exit 1; }
SRC="${1:?Укажите строку подключения к Supabase}"
DUMP="$(mktemp)"; chmod 600 "$DUMP"; trap 'rm -f "$DUMP"' EXIT
pg_dump --data-only --no-owner --no-privileges -t app.lists -t app.members -t app.events "$SRC" > "$DUMP"
chown postgres "$DUMP"
sudo -u postgres psql -X -v ON_ERROR_STOP=1 -d ledor -1 \
  -c 'truncate app.events, app.members, app.lists cascade' -f "$DUMP"
sudo -u postgres psql -X -d ledor -Atc "select 'списков: ' || count(*) from app.lists"
