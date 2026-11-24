#!/usr/bin/env bash
# Свой бэкенд LeDor вместо Supabase (B-11): PostgreSQL + PostgREST за Caddy.
# Запуск (один раз):  sudo bash tools/server/setup-backend.sh
# Повторный запуск безопасен: уже сделанные шаги пропускаются, миграции применяются только новые.
#
# Итог: https://kalendar.bothub.chat/rest/v1/rpc/<функция> — тот же адрес и контракт, что у Supabase,
# поэтому приложение меняет только supabase.url / supabase.key в local.properties.
# Наружу открыт только POST /rest/v1/rpc/*; Postgres и PostgREST слушают лишь 127.0.0.1.
set -euo pipefail
[ "$(id -u)" = 0 ] || { echo "Запустите через sudo."; exit 1; }

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
DB=ledor
PGRST_VERSION=v16.5
PGRST_SHA256=5099e7ada38f9f6846cd87c14c5f158b7c165f1e9959665ad412e61f4dceba95
PGRST_PORT=3300
ETC=/etc/ledor
CADDYFILE=/etc/caddy/Caddyfile

say() { printf '\n== %s\n' "$*"; }
psql_db() { sudo -u postgres psql -v ON_ERROR_STOP=1 -X -q "$@"; }

# ── 1. PostgreSQL ──
say "PostgreSQL"
if ! command -v pg_ctlcluster >/dev/null; then
  apt-get update -q
  DEBIAN_FRONTEND=noninteractive apt-get install -y -q postgresql
fi
systemctl enable --now postgresql
# Только локальные подключения (по умолчанию так и есть — проверяем явно).
PGCONF="$(sudo -u postgres psql -XAtc 'show config_file')"
if grep -Eq "^\s*listen_addresses\s*=\s*'(\*|0\.0\.0\.0)'" "$PGCONF"; then
  echo "ВНИМАНИЕ: в $PGCONF Postgres слушает все адреса. Для LeDor это не нужно — проверьте firewall." >&2
fi

install -d -m 750 -o root -g root "$ETC"
if [ ! -f "$ETC/authenticator.pass" ]; then
  head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 40 > "$ETC/authenticator.pass"
  chmod 600 "$ETC/authenticator.pass"
fi
if [ ! -f "$ETC/public-api-key" ] && [ -f "$HERE/public-api-key" ]; then
  # Ключ, с которым уже собрано приложение (tools/server/public-api-key, не в git).
  install -m 644 "$HERE/public-api-key" "$ETC/public-api-key"
fi
if [ ! -f "$ETC/public-api-key" ]; then
  # Публичный ключ приложения (аналог sb_publishable): не секрет, лишь отсекает случайные запросы.
  printf 'ledor_pub_%s' "$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 32)" > "$ETC/public-api-key"
  chmod 644 "$ETC/public-api-key"
fi
AUTH_PASS="$(cat "$ETC/authenticator.pass")"
API_KEY="$(cat "$ETC/public-api-key")"

sudo -u postgres psql -XAtc "select 1 from pg_database where datname='$DB'" | grep -q 1 || sudo -u postgres createdb "$DB"

# Роли как в Supabase: authenticator входит и переключается в anon; authenticated — для совместимости грантов.
psql_db -d "$DB" <<SQL
do \$\$
begin
  if not exists (select 1 from pg_roles where rolname = 'anon') then create role anon nologin; end if;
  if not exists (select 1 from pg_roles where rolname = 'authenticated') then create role authenticated nologin; end if;
  if not exists (select 1 from pg_roles where rolname = 'authenticator') then
    create role authenticator login noinherit;
  end if;
end \$\$;
alter role authenticator password '$AUTH_PASS';
grant anon, authenticated to authenticator;
create schema if not exists extensions;
revoke create on schema public from public;
create table if not exists public.ledor_migrations (name text primary key, applied_at timestamptz not null default now());
revoke all on public.ledor_migrations from public;
SQL

# ── 2. Миграции (по порядку, каждая — один раз, в транзакции) ──
say "Миграции"
for f in "$ROOT"/supabase/migrations/*.sql; do
  name="$(basename "$f")"
  if psql_db -d "$DB" -Atc "select 1 from public.ledor_migrations where name='$name'" | grep -q 1; then
    echo "  $name — уже применена"
  else
    { echo 'begin;'; cat "$f"; echo "insert into public.ledor_migrations(name) values ('$name');"; echo 'commit;'; } \
      | psql_db -d "$DB"
    echo "  $name — применена"
  fi
done

# ── 3. PostgREST ──
say "PostgREST $PGRST_VERSION"
if ! /usr/local/bin/postgrest --version 2>/dev/null | grep -q "${PGRST_VERSION#v}"; then
  TMP="$(mktemp -d)"
  curl -fsSL -o "$TMP/p.tar.xz" "https://github.com/PostgREST/postgrest/releases/download/$PGRST_VERSION/postgrest-$PGRST_VERSION-linux-static-x86-64.tar.xz"
  echo "$PGRST_SHA256  $TMP/p.tar.xz" | sha256sum -c -
  tar -xJf "$TMP/p.tar.xz" -C "$TMP"
  install -m 755 "$TMP/postgrest" /usr/local/bin/postgrest
  rm -rf "$TMP"
fi
id postgrest >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin postgrest

cat > "$ETC/postgrest.conf" <<CONF
db-uri = "postgres://authenticator:$AUTH_PASS@127.0.0.1:5432/$DB"
db-schemas = "public"
db-anon-role = "anon"
db-pool = 5
server-host = "127.0.0.1"
server-port = $PGRST_PORT
openapi-mode = "disabled"
log-level = "error"
CONF
chown root:postgrest "$ETC/postgrest.conf"; chmod 640 "$ETC/postgrest.conf"
chgrp postgrest "$ETC"; chmod 750 "$ETC"

cat > /etc/systemd/system/ledor-postgrest.service <<UNIT
[Unit]
Description=LeDor API (PostgREST)
After=network.target postgresql.service
Requires=postgresql.service

[Service]
User=postgrest
ExecStart=/usr/local/bin/postgrest $ETC/postgrest.conf
ExecReload=/bin/kill -SIGUSR1 \$MAINPID
Restart=always
RestartSec=3
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable ledor-postgrest >/dev/null
systemctl restart ledor-postgrest
# После миграций PostgREST должен перечитать схему.
sleep 2; systemctl kill -s SIGUSR1 ledor-postgrest || true

# ── 4. Ежедневная резервная копия ──
say "Резервные копии"
install -d -m 750 -o postgres -g postgres /var/backups/ledor
cat > /etc/cron.d/ledor-backup <<'CRON'
# LeDor: копия БД каждый день в 03:15, храним 14 дней. Копировать и за пределы сервера!
15 3 * * * postgres pg_dump -Fc ledor > /var/backups/ledor/ledor-$(date +\%F).dump && find /var/backups/ledor -name 'ledor-*.dump' -mtime +14 -delete
# Отзывы храним не дольше 2 лет (Политика конфиденциальности).
30 3 * * * postgres psql -X -q -d ledor -c "delete from app.feedback where created_at < now() - interval '2 years'"
CRON
chmod 644 /etc/cron.d/ledor-backup

# ── 5. Уведомления об отзывах в Telegram (B-2) ──
say "Уведомления об отзывах"
install -m 755 "$HERE/feedback-notify.py" /usr/local/bin/ledor-feedback-notify
if [ ! -f "$ETC/telegram.env" ]; then
  printf '# Заполнить и сохранить: токен от @BotFather и chat_id, куда слать отзывы.\nTELEGRAM_BOT_TOKEN=\nTELEGRAM_CHAT_ID=\n' > "$ETC/telegram.env"
fi
chown root:postgres "$ETC/telegram.env"; chmod 640 "$ETC/telegram.env"
cat > /etc/systemd/system/ledor-feedback-notify.service <<UNIT
[Unit]
Description=LeDor: отправить новые отзывы в Telegram
After=postgresql.service

[Service]
Type=oneshot
User=postgres
EnvironmentFile=$ETC/telegram.env
ExecStart=/usr/local/bin/ledor-feedback-notify
UNIT
cat > /etc/systemd/system/ledor-feedback-notify.timer <<'UNIT'
[Unit]
Description=LeDor: проверять новые отзывы раз в 2 минуты

[Timer]
OnBootSec=2min
OnUnitActiveSec=2min

[Install]
WantedBy=timers.target
UNIT
systemctl daemon-reload
systemctl enable --now ledor-feedback-notify.timer >/dev/null

# ── 6. Caddy: /rest/v1/rpc/* → PostgREST ──
say "Caddy"
TMP="$(mktemp)"
python3 - "$CADDYFILE" "$HERE/../deploy/kalendar.caddy" "$API_KEY" > "$TMP" <<'PY'
import sys
caddyfile, block_path, key = sys.argv[1:]
out, skip = [], False
for line in open(caddyfile).read().splitlines(keepends=True):
    if line.startswith('# LeDor') or line.startswith('# Файлы: /var/www/kalendar'):
        continue                      # старые комментарии блока
    if line.startswith('kalendar.bothub.chat'):
        skip = True                   # старый блок целиком — до закрывающей скобки в первой колонке
        continue
    if skip:
        skip = line.rstrip() != '}'
        continue
    out.append(line)
block = open(block_path).read().replace('LEDOR_API_KEY', key)
sys.stdout.write(''.join(out).rstrip('\n') + '\n\n' + block)
PY
caddy validate --config "$TMP" --adapter caddyfile
cp "$CADDYFILE" "$CADDYFILE.bak-$(date +%F-%H%M%S)"
install -m 644 "$TMP" "$CADDYFILE"
rm -f "$TMP"
systemctl reload caddy

say "Готово"
echo "API:  https://kalendar.bothub.chat/rest/v1/rpc/…"
echo "Ключ приложения (не секрет): $API_KEY  — лежит в $ETC/public-api-key"
echo "Отзывы в Telegram: заполните $ETC/telegram.env (sudo nano $ETC/telegram.env)."
