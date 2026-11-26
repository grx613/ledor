# 04. Сервер

Задача сервера — хранить общие списки, отдавать изменения и принимать отзывы. Учётных записей пользователей нет.
Доступ к списку даёт знание его **названия и ПИН-кода**.

Сервер свой: **PostgreSQL + PostgREST за Caddy** на `https://kalendar.bothub.chat` (обновлено 2026-11-24).
Сначала проект работал на Supabase; контракт остался тем же (Supabase внутри — те же Postgres и PostgREST), поэтому
миграции лежат в `supabase/migrations/`, а клиент в Android называется `SupabaseRpc`. Те же миграции можно применить
и в проекте Supabase — см. `supabase/README.md`.

## 1. Принципы

1. **Клиент никогда не обращается к таблицам напрямую.** Только вызовы RPC-функций
   `POST https://kalendar.bothub.chat/rest/v1/rpc/<имя>` с публичным ключом приложения в заголовке `apikey`.
   Caddy пропускает к PostgREST только этот путь и только POST; всё остальное под `/rest/` → 404/405.
2. Таблицы лежат в схеме `app`, которая **не выставлена** в PostgREST (`db-schemas = "public"`).
   На всех таблицах включён RLS без единой политики; у ролей `anon`/`authenticated` нет прав на схему `app`.
3. Функции в схеме `public`: `language plpgsql`, `security definer`, `set search_path = ''`
   (все имена — с указанием схемы: `app.lists`, `extensions.crypt(...)`).
   `revoke execute ... from public; grant execute ... to anon, authenticated;`
4. **Бизнес-ошибки не через `RAISE`.** Функция всегда возвращает JSON-конверт:
   `{"ok": true, ...}` или `{"ok": false, "error": "КОД", ...}`.
   Причина: `RAISE` откатывает транзакцию, и счётчик неудачных попыток ПИН-кода не сохранится — защита от
   перебора перестанет работать. Это самая вероятная ошибка реализации, на неё есть отдельный тест.
5. Никаких push-уведомлений, постоянных соединений и сторонних сервисов. Только Postgres + PostgREST.
6. Сервер ничего не знает о еврейском календаре: хранит поля события как есть, проверяет только формат и размеры.

## 2. Схема данных

Миграции: `supabase/migrations/0001_init.sql` (списки, участники, события, лимиты) и `0002_feedback_observe_both.sql`
(отзывы, `observe_by = 'BOTH'`). Новые изменения схемы — только новыми файлами `000N_….sql`: установочный скрипт применяет
каждый файл один раз (таблица `public.ledor_migrations`). Расширение `pgcrypto` ставится в схему `extensions`.

```sql
create schema if not exists app;

-- Списки
create table app.lists (
  id               uuid primary key default gen_random_uuid(),
  name             text        not null check (char_length(name) between 3 and 60),
  name_key         text        not null unique,            -- нормализованное имя для поиска
  read_pin         text        not null check (read_pin ~ '^[0-9]{4,8}$'),   -- открытым текстом, см. §2.1
  master_pin_hash  text        not null,                    -- extensions.crypt(pin, extensions.gen_salt('bf', 10))
  revision         bigint      not null default 0,          -- растёт на 1 при каждой записи в список
  failed_attempts  integer     not null default 0,
  last_failed_at   timestamptz,
  locked_until     timestamptz,
  created_at       timestamptz not null default now(),
  last_activity_at timestamptz not null default now()
);

-- Устройства, подключённые к списку (вместо учётных записей)
create table app.members (
  token_hash   bytea       primary key,                     -- sha256(token); сам токен хранится только на устройстве
  list_id      uuid        not null references app.lists(id) on delete cascade,
  role         text        not null check (role in ('READER','EDITOR')),
  created_at   timestamptz not null default now(),
  last_seen_at timestamptz not null default now()
);
create index members_list_idx on app.members(list_id);

-- События
create table app.events (
  id             uuid        primary key,                   -- генерирует клиент
  list_id        uuid        not null references app.lists(id) on delete cascade,
  rev            bigint      not null,                      -- revision списка в момент последнего изменения
  deleted        boolean     not null default false,        -- «надгробие» для синхронизации удалений
  title          text        not null default '' check (char_length(title) <= 120),
  type           text        not null check (type in ('BIRTHDAY','YAHRZEIT','BAR_MITZVAH','BAT_MITZVAH','WEDDING','OTHER')),
  custom_label   text        check (char_length(custom_label) <= 60),
  recurring      boolean     not null,
  observe_by     text        not null check (observe_by in ('HEBREW','GREGORIAN','BOTH')),  -- BOTH: основной календарь = input_calendar
  input_calendar text        not null check (input_calendar in ('HEBREW','GREGORIAN')),
  g_year         smallint    check (g_year between 1600 and 2200),
  g_month        smallint    check (g_month between 1 and 12),
  g_day          smallint    check (g_day between 1 and 31),
  after_sunset   boolean     not null default false,
  h_year         smallint    check (h_year between 5360 and 5960),
  h_month        text        check (h_month in ('NISAN','IYAR','SIVAN','TAMMUZ','AV','ELUL','TISHREI','CHESHVAN',
                                                'KISLEV','TEVET','SHEVAT','ADAR','ADAR_I','ADAR_II')),
  h_day          smallint    check (h_day between 1 and 30),
  note           text        check (char_length(note) <= 500),
  updated_at     timestamptz not null default now()
);
create index events_list_rev_idx on app.events(list_id, rev);

-- Ограничение частоты запросов по IP
create table app.rate_limits (
  bucket       text        not null,                        -- '<действие>:<sha256(ip + соль)>'
  window_start timestamptz not null,                        -- date_trunc('hour', now())
  count        integer     not null default 0,
  primary key (bucket, window_start)
);

-- Служебные значения (соль для хэша IP генерируется миграцией)
create table app.config (key text primary key, value text not null);

-- Отзывы из приложения (миграция 0002)
create table app.feedback (
  id          bigint      generated always as identity primary key,
  created_at  timestamptz not null default now(),
  message     text        not null check (char_length(message) between 1 and 2000),
  contact     text        check (char_length(contact) <= 200),
  app_version text, device text, os_version text, locale text,   -- только если пользователь разрешил
  notified    boolean     not null default false                 -- переслано ли автору в Telegram
);

alter table app.lists       enable row level security;
alter table app.members     enable row level security;
alter table app.events      enable row level security;
alter table app.rate_limits enable row level security;
alter table app.config      enable row level security;
revoke all on schema app from anon, authenticated;
```

### 2.1. Почему обычный ПИН хранится открытым текстом, а Мастер-ПИН — хэшем

- Обычный ПИН — это «код приглашения только для чтения». Редактор, подключившийся по Мастер-ПИНу, должен
  видеть его, чтобы делиться списком с роднёй (требование «Поделиться» и «посмотреть сохранённые ПИН-коды»).
  Из хэша вернуть его невозможно. Таблица при этом недоступна через API.
- Мастер-ПИН даёт право изменять и удалять — хранится только `bcrypt`-хэш и никогда не возвращается клиенту.

### 2.2. Правила значений

| Значение | Правило |
|---|---|
| Название списка | после `btrim` 3–60 символов. Уникально по `name_key`. |
| `name_key` | `lower(regexp_replace(btrim(normalize(name, NFKC)), '\s+', ' ', 'g'))` — регистр и лишние пробелы не важны |
| Обычный ПИН | 4–8 цифр |
| Мастер-ПИН | 6–12 цифр, не равен обычному |
| Токен устройства | 32 случайных байта, hex (64 символа): `encode(extensions.gen_random_bytes(32), 'hex')` |
| Событий в списке | не более 2000 «живых» (не `deleted`) |
| Пакет `push_events` | не более 500 событий |

Клиент повторяет эти проверки у себя (`:domain` → `ListNameRules`, `PinRules`), но авторитет — сервер.

## 3. RPC-контракт

Все параметры именованные (JSON-тело запроса). Все ответы — JSON-конверт.

### Коды ошибок

| Код | Смысл | Реакция клиента |
|---|---|---|
| `INVALID_NAME`, `INVALID_PIN`, `PINS_EQUAL` | не прошла валидация | показать у поля |
| `NAME_TAKEN` | список с таким названием уже есть | «Название занято» |
| `NOT_FOUND` | списка с таким названием нет | «Список не найден» |
| `WRONG_PIN` | неверный ПИН; в ответе `attempts_left` | показать, сколько попыток осталось |
| `LOCKED` | ввод ПИН временно заблокирован; в ответе `retry_after_sec` | «Попробуйте через N минут» |
| `RATE_LIMITED` | слишком много запросов с адреса | «Слишком много попыток, попробуйте позже» |
| `LIST_DELETED` | списка с таким `id` больше нет | удалить список с устройства + уведомить |
| `UNAUTHORIZED` | список есть, но токен неизвестен | пометить список «доступ потерян» |
| `FORBIDDEN` | нужна роль `EDITOR` | понизить роль, перечитать список |
| `TOO_MANY_EVENTS`, `BATCH_TOO_LARGE`, `INVALID_EVENT` | ограничения | показать ошибку, оставить изменения в очереди |
| `FEEDBACK_EMPTY`, `FEEDBACK_TOO_LONG` | отзыв пуст или длиннее 2000 символов (контакт — 200) | показать у поля |

### Функции

**1. `list_exists(p_name text)`** → `{"ok":true,"exists":true|false}`
Лимит: 60 вызовов/час с IP.

**2. `create_list(p_name text, p_read_pin text, p_master_pin text)`**
→ `{"ok":true,"list":{"id","name","revision":0},"role":"EDITOR","token":"…","read_pin":"…"}`
Ошибки: `INVALID_NAME`, `INVALID_PIN`, `PINS_EQUAL`, `NAME_TAKEN`, `RATE_LIMITED`. Лимит: 10/час с IP.
Создаёт список и запись `members` с ролью `EDITOR`.

**3. `join_list(p_name text, p_pin text)`**
→ `{"ok":true,"list":{"id","name","revision"},"role":"READER"|"EDITOR","token":"…","read_pin":"…"}`
Ошибки: `NOT_FOUND`, `WRONG_PIN`, `LOCKED`, `RATE_LIMITED`. Лимит: 30/час с IP.
Роль определяется тем, какой ПИН подошёл. Каждый успешный вызов выдаёт новый токен.

**4. `pull_changes(p_list_id uuid, p_token text, p_since bigint)`**
→ `{"ok":true,"list":{"id","name","revision"},"role":"…","read_pin":"…","events":[ … ]}`
Возвращает все события списка с `rev > p_since` (включая «надгробия»), по возрастанию `rev`.
`p_since = 0` — полная выгрузка. Обновляет `members.last_seen_at`, `lists.last_activity_at`.
Ошибки: `LIST_DELETED`, `UNAUTHORIZED`.

Формат события (одинаков в `pull_changes` и `push_events`):

```json
{ "id": "uuid", "deleted": false, "rev": 17,
  "title": "Сара Коэн", "type": "BIRTHDAY", "custom_label": null, "recurring": true,
  "observe_by": "HEBREW", "input_calendar": "GREGORIAN",          // observe_by: HEBREW | GREGORIAN | BOTH
  "g_year": 1990, "g_month": 4, "g_day": 23, "after_sunset": false,
  "h_year": 5750, "h_month": "NISAN", "h_day": 28, "note": null }
```

**5. `push_events(p_list_id uuid, p_token text, p_events jsonb)`** — только `EDITOR`
→ `{"ok":true,"revision":18,"accepted":["uuid",…],"rejected":[{"id":"uuid","reason":"FOREIGN_ID"}]}`
Алгоритм:
1. Проверить токен и роль; размер пакета; лимит событий.
2. `update app.lists set revision = revision + 1, last_activity_at = now() where id = … returning revision`
   (блокировка строки списка сериализует параллельные записи). Все события пакета получают этот `rev`.
3. Для каждого события — `insert … on conflict (id) do update … where app.events.list_id = excluded.list_id`.
   Событие с `id`, принадлежащим **другому** списку, не трогается и попадает в `rejected` (`FOREIGN_ID`).
4. Для `deleted = true`: строка превращается в «надгробие» с **обнулёнными персональными данными**
   (`title = ''`, `custom_label`, `note`, все поля даты → `null`/значения по умолчанию). Остаются только `id`, `rev`, `deleted`.
5. Любое нарушение ограничений → весь пакет отклоняется: `INVALID_EVENT` (вставку обернуть в блок
   `begin … exception when others then return …`).

Разрешение конфликтов: «побеждает последняя запись, дошедшая до сервера».

**6. `leave_list(p_list_id uuid, p_token text)`** → `{"ok":true}` — удаляет запись `members` для токена. Идемпотентна.

**7. `delete_list(p_list_id uuid, p_token text, p_master_pin text)`** → `{"ok":true}`
Требует одновременно токен с ролью `EDITOR` **и** верный Мастер-ПИН (повторный ввод в интерфейсе).
Удаляет список каскадно — события и участников, безвозвратно. Неверный ПИН учитывается тем же счётчиком блокировки.
Ошибки: `LIST_DELETED`, `UNAUTHORIZED`, `FORBIDDEN`, `WRONG_PIN`, `LOCKED`.

**8. `submit_feedback(p_message text, p_contact text, p_meta jsonb)`** → `{"ok":true}`
`p_meta` — объект с необязательными `app_version`, `device`, `os_version`, `locale` (пустой объект, если пользователь не
разрешил техданные). Ошибки: `FEEDBACK_EMPTY`, `FEEDBACK_TOO_LONG`, `RATE_LIMITED`. Лимит: 5/час с IP.
Запись попадает в `app.feedback`; раз в 2 минуты скрипт на сервере пересылает новые отзывы автору в Telegram.

## 4. Защита от перебора ПИН-кода (`join_list`, `delete_list`)

```
если last_failed_at старше 24 часов → failed_attempts := 0
если locked_until > now()           → вернуть LOCKED (ПИН НЕ проверять)
если p_pin = read_pin               → роль READER
иначе если crypt(p_pin, master_pin_hash) = master_pin_hash → роль EDITOR; failed_attempts := 0
иначе:
    failed_attempts += 1; last_failed_at := now()
    если failed_attempts кратно 5:
        locked_until := now() + (15 минут × 4^(failed_attempts/5 − 1)), но не более 24 часов
        вернуть WRONG_PIN, attempts_left = 0, retry_after_sec
    иначе вернуть WRONG_PIN, attempts_left = 5 − (failed_attempts mod 5)
```

Длительности блокировок подряд: 15 минут → 1 час → 4 часа → 16 часов → 24 часа (далее всегда 24 часа).

- Счётчик сбрасывается только успешным вводом **Мастер-ПИНа** (иначе, зная обычный ПИН, можно было бы
  сбрасывать счётчик и бесконечно подбирать мастер).
- Блокировка действует только на **ввод ПИН-кода**. Уже подключённые устройства работают по токенам и её не замечают.
- Итог: 4-значный ПИН при такой схеме перебирается годами. Известный компромисс: злоумышленник может
  держать список заблокированным для новых подключений (решение D-07).

Ограничение по IP — вспомогательная функция `app.rate_ok(p_action text, p_limit int) returns boolean`:
IP берётся из заголовков запроса
`coalesce(h->>'cf-connecting-ip', split_part(h->>'x-forwarded-for', ',', 1), 'unknown')`,
где `h = current_setting('request.headers', true)::json`; хранится только `sha256(ip + соль)` с окном в 1 час.
Строки старше суток удаляются при каждом вызове (`delete … where window_start < now() - interval '1 day'`).

## 5. Клиент

Собственный тонкий клиент без SDK: на Android — OkHttp + kotlinx.serialization (`SupabaseRpc`), на iOS — `URLSession`.

```
POST https://kalendar.bothub.chat/rest/v1/rpc/{function}
apikey: {публичный ключ}
Content-Type: application/json

{"p_name":"Семья Коэн","p_pin":"123456"}
```

- Публичный ключ приложения (`ledor_pub_…`) не секрет — он зашит в приложение и лишь отсекает случайные запросы;
  его проверяет Caddy. В репозиторий он не кладётся: на сервере — `/etc/ledor/public-api-key`.
  Заголовок `Authorization` сервер отбрасывает (PostgREST работает без JWT, под ролью `anon`).
- Android: адрес и ключ — в `android/local.properties` (`supabase.url=https://kalendar.bothub.chat`, `supabase.key=…`) →
  `BuildConfig`. Файл не коммитится; образец — `local.properties.example`. Если значения пустые — приложение собирается
  и работает, облачные функции показывают «Облако не настроено».
- Таймауты: соединение 10 с, чтение 20 с. Повторы — на уровне `SyncEngine`/WorkManager, не в HTTP-клиенте.
- HTTP ≠ 200, сетевые ошибки, невалидный JSON → `RpcResult.Failure(NETWORK | SERVER)`; конверт с `ok:false` → `RpcResult.Error(code, …)`.
- Секретов уровня администратора (пароль базы, токен Telegram-бота) в приложениях и репозитории нет: они живут только
  на сервере в `/etc/ledor/`.

## 6. Алгоритм синхронизации (клиент)

`SyncEngine.syncAll()` — под `Mutex`, для каждого облачного списка независимо (ошибка одного не мешает остальным):

1. **Отправка** (если роль `EDITOR` и есть строки `PENDING_*`): `push_events` пакетами до 200.
   Успех → `PENDING_UPSERT` становятся `SYNCED` с новым `rev`; `PENDING_DELETE` физически удаляются.
2. **Получение**: `pull_changes(since = lastSyncedRev)`. Для каждого события:
   `deleted` → удалить локально; иначе вставить/обновить, **кроме** строк, у которых локально `PENDING_*`
   (локальная правка ждёт отправки). Обновить название, роль, `readPin`, `lastSyncedRev = revision`, `lastSyncAt`.
3. Ошибки: `LIST_DELETED` → удалить список и его события локально, положить сообщение в очередь внутренних
   уведомлений («Список „X“ удалён владельцем»); `UNAUTHORIZED` → состояние списка `ACCESS_LOST`
   (события остаются, только чтение, значок «нет доступа»); `FORBIDDEN` → роль `READER`, отбросить локальные
   правки и перечитать список с `since = 0`; сеть → оставить как есть до следующего раза.
4. После синхронизации — `ReminderScheduler.rearm()`.

Когда запускается: при холодном старте приложения (если прошло > 15 минут с прошлой синхронизации), по кнопке
«Синхронизировать», сразу после локальной правки облачного списка (разовая задача WorkManager с условием «есть сеть»),
и раз в сутки в фоне (см. `05-reminders-battery.md`).

Полная пересинхронизация (`since = 0`): заменить все `SYNCED`-события списка пришедшими. «Надгробия» на сервере в v1
не чистятся (они почти ничего не весят и уже не содержат персональных данных).

## 7. Развёртывание и проверка

Сервер ставится одной командой на машине с Caddy (Ubuntu): `sudo bash tools/server/setup-backend.sh`.
Скрипт можно запускать повторно — сделанные шаги пропускаются, применяются только новые миграции. Он:

1. ставит PostgreSQL (слушает только 127.0.0.1), создаёт базу `ledor` и роли `anon`, `authenticated`, `authenticator`;
2. применяет миграции из `supabase/migrations/` по порядку, каждую один раз;
3. ставит PostgREST (версия и контрольная сумма зафиксированы в скрипте) как службу `ledor-postgrest` на 127.0.0.1:3300;
4. включает ежедневную копию базы (`/var/backups/ledor`, 14 дней) и удаление отзывов старше 2 лет;
5. ставит пересылку отзывов в Telegram (`ledor-feedback-notify.timer`; токен бота и chat_id — в `/etc/ledor/telegram.env`);
6. заменяет блок `kalendar.bothub.chat` в Caddyfile (`tools/deploy/kalendar.caddy`): API + статический сайт.

Вспомогательное: `tools/server/feedback-list.sh` — последние отзывы; `tools/server/import-from-supabase.sh` — разовый перенос
списков из Supabase.

Проверка — интеграционным скриптом `tools/rpc-smoke/smoke.mjs` (Node 22, встроенные `fetch` и `node:test`):

```bash
SUPABASE_URL=https://kalendar.bothub.chat SUPABASE_KEY=<публичный ключ> node --test tools/rpc-smoke/smoke.mjs
```

Он ходит на живой сервер, создаёт списки со случайными именами и в конце удаляет их (заблокированный сценарием 9 список и
тестовый отзыв остаются — их удаляет администратор).

Обязательные сценарии smoke-теста:

1. `create_list` → успех; повтор с тем же именем в другом регистре и с лишними пробелами → `NAME_TAKEN`.
2. `list_exists` — `true` для существующего (в любом регистре), `false` для случайного.
3. `join_list` обычным ПИНом → `READER`; Мастер-ПИНом → `EDITOR`.
4. Читатель: `push_events` → `FORBIDDEN`.
5. Редактор: `push_events` (3 события) → `revision = 1`; `pull_changes(since = 0)` у читателя → 3 события.
6. Изменение одного события → `pull_changes(since = 1)` возвращает ровно одно.
7. Удаление → в `pull_changes` приходит «надгробие» с пустым `title`.
8. Событие с `id`, уже существующим в другом списке → `rejected: FOREIGN_ID`, чужое событие не изменилось.
9. **Неверный ПИН 5 раз подряд → `WRONG_PIN` ×5 с убывающим `attempts_left`, затем верный ПИН → `LOCKED`.**
   (Именно этот тест ловит ошибку «счётчик откатился из-за RAISE».) Токены, выданные раньше, продолжают работать.
10. Прямой доступ к таблицам: `GET /rest/v1/lists`, `GET /rest/v1/events` → 404/отказ.
11. `delete_list` с неверным Мастер-ПИНом → `WRONG_PIN`; с верным → успех; затем `pull_changes` → `LIST_DELETED`.
12. Невалидные данные: ПИН из букв, имя из 2 символов, событие с `type = "X"` → соответствующие коды ошибок.
13. (10b в скрипте) Событие с `observe_by = "BOTH"` принимается и возвращается как `BOTH`.
14. `submit_feedback`: пустой текст → `FEEDBACK_EMPTY`, 2001 символ → `FEEDBACK_TOO_LONG`, обычный отзыв → успех.

Один прогон создаёт два списка (основной и «чужой» для сценария 8), а лимит `create_list` — 10 в час с адреса:
запускать smoke-тест не чаще 5 раз в час, иначе он упадёт с `RATE_LIMITED` (это не ошибка бэкенда).
Оба списка удаляются в блоке завершения теста даже при падении сценариев.

## 8. Эксплуатационные заметки

- Персональные данные в базе: названия списков, имена/названия событий, даты, заметки, тексты и контакты отзывов.
  Это отражено в Политике конфиденциальности (`07-website.md`): отзывы — до 2 лет, хэш IP — до суток, копии базы — 14 дней.
- Резервные копии лежат на том же сервере — нужна копия наружу (не настроено).
- Лимиты по IP берут адрес из `cf-connecting-ip` / `x-forwarded-for`. Прокси-сервер ставит `CF-Connecting-IP` сам из
  адреса соединения. При обращении к основному серверу напрямую, в обход прокси, заголовок можно подделать — это
  ослабляет только лимиты, но не защиту ПИН-кодов (она считается по списку).
- Сайт и API обслуживает один блок Caddy на основном сервере.
- **Путь запросов (D-58, с 26.11.2026):** DNS `kalendar.bothub.chat` (без прокси CDN) → прокси-сервер в России → Caddy там
  пересылает всё на основной сервер по адресу, с `Host` и SNI `kalendar.bothub.chat` (блок —
  `tools/deploy/kalendar-mirror.caddy`, адрес основного сервера там заменён на `ORIGIN_IP`). Причина: из России CDN и
  хостинг основного сервера блокируются, а с прокси-сервера основной доступен. Данных на прокси нет, база одна.
  Сертификат у прокси свой; сертификат основного сервера (Let's Encrypt, до 22.02.2027) продлевается через проверку на
  порту 80, которую прокси пересылает на основной сервер, — проверить, что продление прошло, в конце января 2027.
- **Названия хостингов и адреса серверов в репозитории не пишутся** (он открытый, D-59). Они — в файле
  `~/.config/ledor/infra.md` на основном сервере, там же — как откатить DNS.
- Отложено: смена ПИН-кодов и отзыв токенов, переименование списка, автоудаление заброшенных списков,
  очистка «надгробий», очередь отзывов без сети.
