# Сервер: миграции и обслуживание

Контракт и правила безопасности — [`docs/04-backend.md`](../docs/04-backend.md).
Имя каталога историческое: сначала проект работал на Supabase, теперь сервер свой (PostgreSQL + PostgREST за Caddy),
а миграции те же.

## 1. Свой сервер (основной способ)

```bash
sudo bash tools/server/setup-backend.sh
```

Скрипт ставит PostgreSQL и PostgREST, применяет миграции из `migrations/` (каждую один раз), включает резервные копии
и пересылку отзывов в Telegram, обновляет блок Caddy. Повторный запуск безопасен. Подробности — `docs/04-backend.md` §7.

- Публичный ключ приложения — `/etc/ledor/public-api-key` (не секрет, но в репозиторий не кладётся).
- Токен Telegram-бота и chat_id для отзывов — `/etc/ledor/telegram.env`.
- Новое изменение схемы — новым файлом `migrations/000N_….sql`, затем снова запустить скрипт.

## 2. Подключить приложение

В `android/local.properties` (файл не коммитится):

```
supabase.url=https://kalendar.bothub.chat
supabase.key=<публичный ключ>
```

Пересобрать приложение. Без этих строк оно работает только с личным списком.

## 3. Проверка

```bash
SUPABASE_URL=https://kalendar.bothub.chat SUPABASE_KEY=<публичный ключ> node --test tools/rpc-smoke/smoke.mjs
```

14 сценариев на живом сервере. Не запускать чаще 5 раз в час (лимит на создание списков). После прогона остаются
заблокированный список «Smoke Test …» и тестовый отзыв — удалить их командами из §4.

Важнейший сценарий — №9: пять неверных ПИН-кодов должны привести к `LOCKED`. Если вместо этого снова `WRONG_PIN`,
значит ошибка где-то возвращается через `RAISE` и счётчик откатывается.

## 4. Обслуживание (на сервере)

```bash
sudo bash tools/server/feedback-list.sh 20                 # последние отзывы
sudo -u postgres psql -d ledor                             # консоль базы
```

Удаление списка по жалобе или тестового списка:

```sql
select id, name, created_at, last_activity_at from app.lists where name_key = app.normalize_name('Название списка');
delete from app.lists where id = '<uuid>';   -- события и участники удаляются каскадно
delete from app.feedback where id = <номер>; -- удалить отзыв
```

Участники увидят «Список удалён владельцем» при следующей синхронизации.

Резервные копии — `/var/backups/ledor/ledor-ГГГГ-ММ-ДД.dump` (14 дней); восстановление — `pg_restore -d ledor <файл>`.

## 5. Supabase (запасной вариант)

Те же миграции можно применить в проекте Supabase: SQL Editor → вставить файлы по порядку → Run; в настройках API
оставить выставленной только схему `public`. В `local.properties` тогда указываются адрес проекта и его публичный ключ
(`sb_publishable_…`); секретный ключ никуда не копируется. Перенос данных оттуда на свой сервер —
`tools/server/import-from-supabase.sh`.
