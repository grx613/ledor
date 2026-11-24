# Supabase: бэкенд облачных списков

Контракт и правила безопасности — [`docs/04-backend-supabase.md`](../docs/04-backend-supabase.md).

## 1. Создать проект

1. На supabase.com создать проект (регион — ближе к пользователям, например Frankfurt).
2. **Project Settings → API (Data API) → Exposed schemas:** оставить только `public`.
3. Скопировать **Project URL** и **публичный ключ** (`sb_publishable_…` или legacy `anon`).
   Секретный ключ (`service_role` / `sb_secret_…`) никуда не копировать.

## 2. Применить миграцию

Любой из способов:

- **SQL Editor** в панели Supabase → вставить содержимое `migrations/0001_init.sql` → Run.
- **CLI:** `supabase link --project-ref <ref>` → `supabase db push`.

Миграция создаёт схему `app` (таблицы, закрытые от API) и 7 RPC-функций в `public`.

## 3. Подключить приложение

В `android/local.properties` (файл не коммитится):

```
supabase.url=https://<ref>.supabase.co
supabase.key=<публичный ключ>
```

Пересобрать APK. Без этих строк приложение работает только с личным списком.

## 4. Проверка

```bash
SUPABASE_URL=https://<ref>.supabase.co SUPABASE_KEY=<публичный ключ> node --test tools/rpc-smoke/smoke.mjs
```

Тест создаёт два временных списка и удаляет их. Не запускать чаще 5 раз в час (лимит на создание списков).

Важнейший сценарий — №9: пять неверных ПИН-кодов должны привести к `LOCKED`. Если вместо этого снова `WRONG_PIN`,
значит ошибка где-то возвращается через `RAISE` и счётчик откатывается.

## 5. Удаление списка по жалобе (модерация)

В SQL Editor (под ролью владельца):

```sql
select id, name, created_at, last_activity_at from app.lists where name_key = app.normalize_name('Название списка');
delete from app.lists where id = '<uuid>';   -- события и участники удаляются каскадно
```

Участники увидят «Список удалён владельцем» при следующей синхронизации.

## 6. Бесплатный тариф

Проект на бесплатном тарифе приостанавливается после недели без запросов — перед проверкой убедиться, что он активен.
