-- 0004: событие «каждый месяц» (B-41).
-- Применяется после 0003. Те же правила: таблицы в app, клиент — только через public.*, ошибки — конвертом.
-- Совместимо со старыми версиями приложения: они не знают поля monthly, не присылают его (значение тогда не меняется)
-- и игнорируют в ответе pull_changes — такое событие они показывают как ежегодное на дату первого раза.
-- Сигнатуры функций не меняются, права на них сохраняются (create or replace).

alter table app.events add column if not exists monthly boolean not null default false;

create or replace function app.event_json(e app.events) returns jsonb
language sql stable set search_path = '' as $$
  select jsonb_build_object(
    'id', e.id, 'deleted', e.deleted, 'rev', e.rev,
    'title', e.title, 'type', e.type, 'custom_label', e.custom_label, 'recurring', e.recurring,
    'observe_by', e.observe_by, 'input_calendar', e.input_calendar,
    'g_year', e.g_year, 'g_month', e.g_month, 'g_day', e.g_day, 'after_sunset', e.after_sunset,
    'h_year', e.h_year, 'h_month', e.h_month, 'h_day', e.h_day, 'note', e.note,
    'monthly', e.monthly)
$$;

create or replace function public.push_events(p_list_id uuid, p_token text, p_events jsonb) returns jsonb
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
                                     input_calendar, g_year, g_month, g_day, after_sunset, h_year, h_month, h_day, note,
                                     monthly, updated_at)
        values (v_id, p_list_id, v_rev, true, '', 'OTHER', null, false, 'GREGORIAN', 'GREGORIAN',
                null, null, null, false, null, null, null, null, false, now())
        on conflict (id) do update set
          rev = excluded.rev, deleted = true, title = '', type = 'OTHER', custom_label = null, recurring = false,
          observe_by = 'GREGORIAN', input_calendar = 'GREGORIAN', g_year = null, g_month = null, g_day = null,
          after_sunset = false, h_year = null, h_month = null, h_day = null, note = null, monthly = false,
          updated_at = now()
        where t.list_id = excluded.list_id;
      else
        insert into app.events as t (id, list_id, rev, deleted, title, type, custom_label, recurring, observe_by,
                                     input_calendar, g_year, g_month, g_day, after_sunset, h_year, h_month, h_day, note,
                                     monthly, updated_at)
        values (v_id, p_list_id, v_rev, false,
                coalesce(v_item->>'title', ''), v_item->>'type', v_item->>'custom_label',
                (v_item->>'recurring')::boolean, v_item->>'observe_by', v_item->>'input_calendar',
                (v_item->>'g_year')::smallint, (v_item->>'g_month')::smallint, (v_item->>'g_day')::smallint,
                coalesce((v_item->>'after_sunset')::boolean, false),
                (v_item->>'h_year')::smallint, v_item->>'h_month', (v_item->>'h_day')::smallint,
                v_item->>'note', coalesce((v_item->>'monthly')::boolean, false), now())
        on conflict (id) do update set
          rev = excluded.rev, deleted = false, title = excluded.title, type = excluded.type,
          custom_label = excluded.custom_label, recurring = excluded.recurring, observe_by = excluded.observe_by,
          input_calendar = excluded.input_calendar, g_year = excluded.g_year, g_month = excluded.g_month,
          g_day = excluded.g_day, after_sunset = excluded.after_sunset, h_year = excluded.h_year,
          h_month = excluded.h_month, h_day = excluded.h_day, note = excluded.note,
          -- Старые версии приложения поля не знают и не присылают — тогда значение не меняется.
          monthly = case when v_item ? 'monthly' then excluded.monthly else t.monthly end,
          updated_at = now()
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
