-- Kalendar: облачные списки дней рождения и событий.
-- Контракт и правила безопасности: docs/04-backend-supabase.md.
--
-- Главное:
--  * таблицы лежат в схеме app, которая НЕ выставлена в Data API; RLS включён без политик;
--  * клиент вызывает только функции public.* (security definer, search_path = '');
--  * бизнес-ошибки возвращаются JSON-конвертом {"ok":false,"error":"…"}, а НЕ через RAISE:
--    иначе транзакция откатится и счётчик неверных ПИН-кодов не сохранится.

create extension if not exists pgcrypto with schema extensions;

create schema if not exists app;

-- ───────────────────────────── Таблицы ─────────────────────────────

create table app.lists (
  id               uuid        primary key default gen_random_uuid(),
  name             text        not null check (char_length(name) between 3 and 60),
  name_key         text        not null unique,
  read_pin         text        not null check (read_pin ~ '^[0-9]{4,8}$'),
  master_pin_hash  text        not null,
  revision         bigint      not null default 0,
  failed_attempts  integer     not null default 0,
  last_failed_at   timestamptz,
  locked_until     timestamptz,
  created_at       timestamptz not null default now(),
  last_activity_at timestamptz not null default now()
);

create table app.members (
  token_hash   bytea       primary key,
  list_id      uuid        not null references app.lists(id) on delete cascade,
  role         text        not null check (role in ('READER','EDITOR')),
  created_at   timestamptz not null default now(),
  last_seen_at timestamptz not null default now()
);
create index members_list_idx on app.members(list_id);

create table app.events (
  id             uuid        primary key,
  list_id        uuid        not null references app.lists(id) on delete cascade,
  rev            bigint      not null,
  deleted        boolean     not null default false,
  title          text        not null default '' check (char_length(title) <= 120),
  type           text        not null check (type in ('BIRTHDAY','YAHRZEIT','BAR_MITZVAH','BAT_MITZVAH','WEDDING','OTHER')),
  custom_label   text        check (char_length(custom_label) <= 60),
  recurring      boolean     not null,
  observe_by     text        not null check (observe_by in ('HEBREW','GREGORIAN')),
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

create table app.rate_limits (
  bucket       text        not null,
  window_start timestamptz not null,
  count        integer     not null default 0,
  primary key (bucket, window_start)
);

create table app.config (key text primary key, value text not null);
insert into app.config (key, value) values ('ip_salt', encode(extensions.gen_random_bytes(16), 'hex'));

alter table app.lists       enable row level security;
alter table app.members     enable row level security;
alter table app.events      enable row level security;
alter table app.rate_limits enable row level security;
alter table app.config      enable row level security;

revoke all on schema app from public;
revoke all on all tables in schema app from public;
do $$
begin
  if exists (select 1 from pg_roles where rolname = 'anon') then
    execute 'revoke all on schema app from anon, authenticated';
    execute 'revoke all on all tables in schema app from anon, authenticated';
  end if;
end $$;

-- ───────────────────────────── Вспомогательные функции ─────────────────────────────

create function app.err(p_code text) returns jsonb
language sql immutable set search_path = '' as $$
  select jsonb_build_object('ok', false, 'error', p_code)
$$;

create function app.normalize_name(p_name text) returns text
language sql immutable set search_path = '' as $$
  select lower(regexp_replace(btrim(normalize(coalesce(p_name, ''), NFKC)), '\s+', ' ', 'g'))
$$;

create function app.clean_name(p_name text) returns text
language sql immutable set search_path = '' as $$
  select regexp_replace(btrim(coalesce(p_name, '')), '\s+', ' ', 'g')
$$;

create function app.token_hash(p_token text) returns bytea
language sql immutable set search_path = '' as $$
  select extensions.digest(coalesce(p_token, ''), 'sha256')
$$;

/** Лимит запросов с одного IP в час. Хранится только хэш IP с солью. */
create function app.rate_ok(p_action text, p_limit integer) returns boolean
language plpgsql set search_path = '' as $$
declare
  v_headers json;
  v_ip      text;
  v_bucket  text;
  v_count   integer;
begin
  begin
    v_headers := current_setting('request.headers', true)::json;
  exception when others then
    v_headers := null;
  end;
  v_ip := coalesce(v_headers->>'cf-connecting-ip',
                   nullif(btrim(split_part(coalesce(v_headers->>'x-forwarded-for', ''), ',', 1)), ''),
                   'unknown');
  v_bucket := p_action || ':' || encode(extensions.digest(
                v_ip || (select value from app.config where key = 'ip_salt'), 'sha256'), 'hex');

  delete from app.rate_limits where window_start < now() - interval '1 day';

  insert into app.rate_limits as r (bucket, window_start, count)
  values (v_bucket, date_trunc('hour', now()), 1)
  on conflict (bucket, window_start) do update set count = r.count + 1
  returning r.count into v_count;

  return v_count <= p_limit;
end $$;

/** Роль владельца токена: 'READER' / 'EDITOR', либо код ошибки 'LIST_DELETED' / 'UNAUTHORIZED'. */
create function app.auth(p_list_id uuid, p_token text) returns text
language plpgsql set search_path = '' as $$
declare
  v_role text;
begin
  if p_list_id is null or not exists (select 1 from app.lists where id = p_list_id) then
    return 'LIST_DELETED';
  end if;
  update app.members set last_seen_at = now()
   where token_hash = app.token_hash(p_token) and list_id = p_list_id
  returning role into v_role;
  return coalesce(v_role, 'UNAUTHORIZED');
end $$;

/** Неверный ПИН: счётчик, блокировка 15 мин × 4^(n/5−1), не более 24 ч. Строка списка уже заблокирована вызывающим. */
create function app.register_failure(p_list_id uuid) returns jsonb
language plpgsql set search_path = '' as $$
declare
  v_attempts integer;
  v_lock     interval;
begin
  update app.lists
     set failed_attempts = failed_attempts + 1, last_failed_at = now()
   where id = p_list_id
  returning failed_attempts into v_attempts;

  if v_attempts % 5 = 0 then
    v_lock := least(interval '15 minutes' * power(4, v_attempts / 5 - 1), interval '24 hours');
    update app.lists set locked_until = now() + v_lock where id = p_list_id;
    return jsonb_build_object('ok', false, 'error', 'WRONG_PIN', 'attempts_left', 0,
                              'retry_after_sec', ceil(extract(epoch from v_lock))::int);
  end if;
  return jsonb_build_object('ok', false, 'error', 'WRONG_PIN', 'attempts_left', 5 - (v_attempts % 5));
end $$;

/** Подготовка списка к проверке ПИН: сброс устаревшего счётчика; LOCKED или null. */
create function app.check_lock(p_list_id uuid) returns jsonb
language plpgsql set search_path = '' as $$
declare
  v_list app.lists;
begin
  select * into v_list from app.lists where id = p_list_id;
  if v_list.last_failed_at is not null and v_list.last_failed_at < now() - interval '24 hours' then
    update app.lists set failed_attempts = 0 where id = p_list_id;
  end if;
  if v_list.locked_until is not null and v_list.locked_until > now() then
    return jsonb_build_object('ok', false, 'error', 'LOCKED',
                              'retry_after_sec', ceil(extract(epoch from v_list.locked_until - now()))::int);
  end if;
  return null;
end $$;

create function app.issue_token(p_list_id uuid, p_role text) returns text
language plpgsql set search_path = '' as $$
declare
  v_token text := encode(extensions.gen_random_bytes(32), 'hex');
begin
  insert into app.members (token_hash, list_id, role) values (app.token_hash(v_token), p_list_id, p_role);
  return v_token;
end $$;

create function app.event_json(e app.events) returns jsonb
language sql stable set search_path = '' as $$
  select jsonb_build_object(
    'id', e.id, 'deleted', e.deleted, 'rev', e.rev,
    'title', e.title, 'type', e.type, 'custom_label', e.custom_label, 'recurring', e.recurring,
    'observe_by', e.observe_by, 'input_calendar', e.input_calendar,
    'g_year', e.g_year, 'g_month', e.g_month, 'g_day', e.g_day, 'after_sunset', e.after_sunset,
    'h_year', e.h_year, 'h_month', e.h_month, 'h_day', e.h_day, 'note', e.note)
$$;

revoke all on all functions in schema app from public;

-- ───────────────────────────── Публичные RPC ─────────────────────────────

create function public.list_exists(p_name text) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
  if not app.rate_ok('exists', 60) then return app.err('RATE_LIMITED'); end if;
  return jsonb_build_object('ok', true, 'exists',
    exists (select 1 from app.lists where name_key = app.normalize_name(p_name)));
end $$;

create function public.create_list(p_name text, p_read_pin text, p_master_pin text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_name  text := app.clean_name(p_name);
  v_id    uuid;
  v_token text;
begin
  if not app.rate_ok('create', 10) then return app.err('RATE_LIMITED'); end if;
  if char_length(v_name) not between 3 and 60 then return app.err('INVALID_NAME'); end if;
  if coalesce(p_read_pin, '') !~ '^[0-9]{4,8}$' or coalesce(p_master_pin, '') !~ '^[0-9]{6,12}$' then
    return app.err('INVALID_PIN');
  end if;
  if p_read_pin = p_master_pin then return app.err('PINS_EQUAL'); end if;

  insert into app.lists (name, name_key, read_pin, master_pin_hash)
  values (v_name, app.normalize_name(v_name), p_read_pin, extensions.crypt(p_master_pin, extensions.gen_salt('bf', 10)))
  on conflict (name_key) do nothing
  returning id into v_id;
  if v_id is null then return app.err('NAME_TAKEN'); end if;

  v_token := app.issue_token(v_id, 'EDITOR');
  return jsonb_build_object('ok', true,
    'list', jsonb_build_object('id', v_id, 'name', v_name, 'revision', 0),
    'role', 'EDITOR', 'token', v_token, 'read_pin', p_read_pin);
end $$;

create function public.join_list(p_name text, p_pin text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_list  app.lists;
  v_lock  jsonb;
  v_role  text;
  v_token text;
begin
  if not app.rate_ok('join', 30) then return app.err('RATE_LIMITED'); end if;
  select * into v_list from app.lists where name_key = app.normalize_name(p_name) for update;
  if not found then return app.err('NOT_FOUND'); end if;

  v_lock := app.check_lock(v_list.id);
  if v_lock is not null then return v_lock; end if;

  if coalesce(p_pin, '') = v_list.read_pin then
    v_role := 'READER';
  elsif extensions.crypt(coalesce(p_pin, ''), v_list.master_pin_hash) = v_list.master_pin_hash then
    v_role := 'EDITOR';
    update app.lists set failed_attempts = 0, last_failed_at = null where id = v_list.id;
  else
    return app.register_failure(v_list.id);
  end if;

  v_token := app.issue_token(v_list.id, v_role);
  update app.lists set last_activity_at = now() where id = v_list.id;
  return jsonb_build_object('ok', true,
    'list', jsonb_build_object('id', v_list.id, 'name', v_list.name, 'revision', v_list.revision),
    'role', v_role, 'token', v_token, 'read_pin', v_list.read_pin);
end $$;

create function public.pull_changes(p_list_id uuid, p_token text, p_since bigint) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_role text := app.auth(p_list_id, p_token);
  v_list app.lists;
begin
  if v_role not in ('READER', 'EDITOR') then return app.err(v_role); end if;
  update app.lists set last_activity_at = now() where id = p_list_id returning * into v_list;
  return jsonb_build_object('ok', true,
    'list', jsonb_build_object('id', v_list.id, 'name', v_list.name, 'revision', v_list.revision),
    'role', v_role, 'read_pin', v_list.read_pin,
    'events', coalesce((select jsonb_agg(app.event_json(e) order by e.rev)
                          from app.events e
                         where e.list_id = p_list_id and e.rev > coalesce(p_since, 0)), '[]'::jsonb));
end $$;

create function public.push_events(p_list_id uuid, p_token text, p_events jsonb) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_role     text := app.auth(p_list_id, p_token);
  v_rev      bigint;
  v_item     jsonb;
  v_id       uuid;
  v_owner    uuid;
  v_deleted  boolean;
  v_accepted jsonb := '[]'::jsonb;
  v_rejected jsonb := '[]'::jsonb;
begin
  if v_role not in ('READER', 'EDITOR') then return app.err(v_role); end if;
  if v_role <> 'EDITOR' then return app.err('FORBIDDEN'); end if;
  if jsonb_typeof(p_events) is distinct from 'array' then return app.err('INVALID_EVENT'); end if;
  if jsonb_array_length(p_events) > 500 then return app.err('BATCH_TOO_LARGE'); end if;

  -- Весь пакет — во вложенном блоке: любая ошибка откатывает его целиком (включая ревизию).
  begin
    update app.lists set revision = revision + 1, last_activity_at = now()
     where id = p_list_id returning revision into v_rev;

    for v_item in select value from jsonb_array_elements(p_events) loop
      v_id := (v_item->>'id')::uuid;
      v_deleted := coalesce((v_item->>'deleted')::boolean, false);

      select list_id into v_owner from app.events where id = v_id;
      if found and v_owner <> p_list_id then
        v_rejected := v_rejected || jsonb_build_object('id', v_id, 'reason', 'FOREIGN_ID');
        continue;
      end if;

      if v_deleted then
        -- «Надгробие»: персональные данные обнуляются.
        insert into app.events as t (id, list_id, rev, deleted, title, type, custom_label, recurring, observe_by,
                                     input_calendar, g_year, g_month, g_day, after_sunset, h_year, h_month, h_day, note, updated_at)
        values (v_id, p_list_id, v_rev, true, '', 'OTHER', null, false, 'GREGORIAN', 'GREGORIAN',
                null, null, null, false, null, null, null, null, now())
        on conflict (id) do update set
          rev = excluded.rev, deleted = true, title = '', type = 'OTHER', custom_label = null, recurring = false,
          observe_by = 'GREGORIAN', input_calendar = 'GREGORIAN', g_year = null, g_month = null, g_day = null,
          after_sunset = false, h_year = null, h_month = null, h_day = null, note = null, updated_at = now()
        where t.list_id = excluded.list_id;
      else
        insert into app.events as t (id, list_id, rev, deleted, title, type, custom_label, recurring, observe_by,
                                     input_calendar, g_year, g_month, g_day, after_sunset, h_year, h_month, h_day, note, updated_at)
        values (v_id, p_list_id, v_rev, false,
                coalesce(v_item->>'title', ''), v_item->>'type', v_item->>'custom_label',
                (v_item->>'recurring')::boolean, v_item->>'observe_by', v_item->>'input_calendar',
                (v_item->>'g_year')::smallint, (v_item->>'g_month')::smallint, (v_item->>'g_day')::smallint,
                coalesce((v_item->>'after_sunset')::boolean, false),
                (v_item->>'h_year')::smallint, v_item->>'h_month', (v_item->>'h_day')::smallint,
                v_item->>'note', now())
        on conflict (id) do update set
          rev = excluded.rev, deleted = false, title = excluded.title, type = excluded.type,
          custom_label = excluded.custom_label, recurring = excluded.recurring, observe_by = excluded.observe_by,
          input_calendar = excluded.input_calendar, g_year = excluded.g_year, g_month = excluded.g_month,
          g_day = excluded.g_day, after_sunset = excluded.after_sunset, h_year = excluded.h_year,
          h_month = excluded.h_month, h_day = excluded.h_day, note = excluded.note, updated_at = now()
        where t.list_id = excluded.list_id;
      end if;
      v_accepted := v_accepted || to_jsonb(v_id);
    end loop;

    if (select count(*) from app.events where list_id = p_list_id and not deleted) > 2000 then
      raise exception using errcode = 'P0001', message = 'TOO_MANY_EVENTS';
    end if;
  exception
    when sqlstate 'P0001' then return app.err(sqlerrm);
    when others then return app.err('INVALID_EVENT');
  end;

  return jsonb_build_object('ok', true, 'revision', v_rev, 'accepted', v_accepted, 'rejected', v_rejected);
end $$;

create function public.leave_list(p_list_id uuid, p_token text) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
  delete from app.members where token_hash = app.token_hash(p_token) and list_id = p_list_id;
  return jsonb_build_object('ok', true);
end $$;

create function public.delete_list(p_list_id uuid, p_token text, p_master_pin text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_role text := app.auth(p_list_id, p_token);
  v_list app.lists;
  v_lock jsonb;
begin
  if v_role not in ('READER', 'EDITOR') then return app.err(v_role); end if;
  if v_role <> 'EDITOR' then return app.err('FORBIDDEN'); end if;
  select * into v_list from app.lists where id = p_list_id for update;

  v_lock := app.check_lock(p_list_id);
  if v_lock is not null then return v_lock; end if;
  if extensions.crypt(coalesce(p_master_pin, ''), v_list.master_pin_hash) <> v_list.master_pin_hash then
    return app.register_failure(p_list_id);
  end if;

  delete from app.lists where id = p_list_id;
  return jsonb_build_object('ok', true);
end $$;

-- ───────────────────────────── Права ─────────────────────────────

revoke all on function public.list_exists(text)                 from public;
revoke all on function public.create_list(text, text, text)     from public;
revoke all on function public.join_list(text, text)             from public;
revoke all on function public.pull_changes(uuid, text, bigint)  from public;
revoke all on function public.push_events(uuid, text, jsonb)    from public;
revoke all on function public.leave_list(uuid, text)            from public;
revoke all on function public.delete_list(uuid, text, text)     from public;

do $$
begin
  if exists (select 1 from pg_roles where rolname = 'anon') then
    execute 'grant execute on function public.list_exists(text), public.create_list(text, text, text),
             public.join_list(text, text), public.pull_changes(uuid, text, bigint),
             public.push_events(uuid, text, jsonb), public.leave_list(uuid, text),
             public.delete_list(uuid, text, text) to anon, authenticated';
  end if;
end $$;
