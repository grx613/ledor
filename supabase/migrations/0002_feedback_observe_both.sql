-- 0002: «по обоим календарям» (B-1) и отзывы из приложения (B-2).
-- Применяется после 0001_init.sql. Те же правила: таблицы в app, клиент — только через public.*, ошибки — конвертом.

-- ───────────── B-1: observe_by = 'BOTH' ─────────────
-- Основной календарь при BOTH — input_calendar; вторая дата вычисляется (год обязателен).
alter table app.events drop constraint if exists events_observe_by_check;
alter table app.events add constraint events_observe_by_check check (observe_by in ('HEBREW','GREGORIAN','BOTH'));

-- ───────────── B-2: отзывы ─────────────
create table app.feedback (
  id          bigint      generated always as identity primary key,
  created_at  timestamptz not null default now(),
  message     text        not null check (char_length(message) between 1 and 2000),
  contact     text        check (char_length(contact) <= 200),
  app_version text        check (char_length(app_version) <= 40),
  device      text        check (char_length(device) <= 120),
  os_version  text        check (char_length(os_version) <= 40),
  locale      text        check (char_length(locale) <= 20),
  notified    boolean     not null default false
);
create index feedback_unnotified_idx on app.feedback(id) where not notified;
alter table app.feedback enable row level security;
revoke all on app.feedback from public;
do $$
begin
  if exists (select 1 from pg_roles where rolname = 'anon') then
    execute 'revoke all on app.feedback from anon, authenticated';
  end if;
end $$;

/** Принять отзыв. p_meta — техданные (только с согласия пользователя): app_version, device, os_version, locale. */
create function public.submit_feedback(p_message text, p_contact text, p_meta jsonb) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_msg     text := btrim(coalesce(p_message, ''));
  v_contact text := nullif(btrim(coalesce(p_contact, '')), '');
  v_meta    jsonb := case when jsonb_typeof(p_meta) = 'object' then p_meta else '{}'::jsonb end;
begin
  if char_length(v_msg) = 0 then return app.err('FEEDBACK_EMPTY'); end if;
  if char_length(v_msg) > 2000 or char_length(coalesce(v_contact, '')) > 200 then return app.err('FEEDBACK_TOO_LONG'); end if;
  if not app.rate_ok('feedback', 5) then return app.err('RATE_LIMITED'); end if;
  insert into app.feedback (message, contact, app_version, device, os_version, locale)
  values (v_msg, v_contact,
          left(v_meta->>'app_version', 40), left(v_meta->>'device', 120),
          left(v_meta->>'os_version', 40), left(v_meta->>'locale', 20));
  return jsonb_build_object('ok', true);
end $$;

revoke all on function public.submit_feedback(text, text, jsonb) from public;
do $$
begin
  if exists (select 1 from pg_roles where rolname = 'anon') then
    execute 'grant execute on function public.submit_feedback(text, text, jsonb) to anon, authenticated';
  end if;
end $$;
