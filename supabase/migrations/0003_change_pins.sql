-- 0003: смена ПИН-кодов у существующего списка (B-40).
-- Применяется после 0002. Те же правила: таблицы в app, клиент — только через public.*, ошибки — конвертом.
-- Новая функция, старые не меняются — приложения без неё работают как раньше.

/**
 * Сменить ПИН-код для чтения и Мастер-ПИН. Нужны токен редактора и действующий Мастер-ПИН (с той же защитой от подбора,
 * что у delete_list). Новые коды — по правилам create_list. Уже подключённые устройства продолжают работать по своим
 * токенам; новый ПИН-код для чтения они получат при следующей синхронизации (pull_changes возвращает read_pin).
 * Если меняется Мастер-ПИН, все остальные редакторы списка становятся читателями: на случай, если старый Мастер-ПИН
 * утёк и кто-то уже подключился с ним. Чтобы снова редактировать, им нужен новый Мастер-ПИН. Вызвавший остаётся
 * редактором. В ответе — сколько редакторов стали читателями (demoted).
 */
create function public.change_pins(p_list_id uuid, p_token text, p_master_pin text, p_new_read_pin text, p_new_master_pin text)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_role text := app.auth(p_list_id, p_token);
  v_list app.lists;
  v_lock jsonb;
  v_demoted integer := 0;
begin
  if v_role not in ('READER', 'EDITOR') then return app.err(v_role); end if;
  if v_role <> 'EDITOR' then return app.err('FORBIDDEN'); end if;
  if coalesce(p_new_read_pin, '') !~ '^[0-9]{4,8}$' or coalesce(p_new_master_pin, '') !~ '^[0-9]{6,12}$' then
    return app.err('INVALID_PIN');
  end if;
  if p_new_read_pin = p_new_master_pin then return app.err('PINS_EQUAL'); end if;

  select * into v_list from app.lists where id = p_list_id for update;
  v_lock := app.check_lock(p_list_id);
  if v_lock is not null then return v_lock; end if;
  if extensions.crypt(coalesce(p_master_pin, ''), v_list.master_pin_hash) <> v_list.master_pin_hash then
    return app.register_failure(p_list_id);
  end if;

  update app.lists
     set read_pin = p_new_read_pin,
         master_pin_hash = case when p_new_master_pin = p_master_pin then master_pin_hash
                                else extensions.crypt(p_new_master_pin, extensions.gen_salt('bf', 10)) end,
         failed_attempts = 0, last_failed_at = null
   where id = p_list_id;
  if p_new_master_pin <> p_master_pin then
    update app.members set role = 'READER'
     where list_id = p_list_id and role = 'EDITOR' and token_hash <> app.token_hash(p_token);
    get diagnostics v_demoted = row_count;
  end if;
  return jsonb_build_object('ok', true, 'read_pin', p_new_read_pin, 'demoted', v_demoted);
end $$;

revoke all on function public.change_pins(uuid, text, text, text, text) from public;
do $$
begin
  if exists (select 1 from pg_roles where rolname = 'anon') then
    execute 'grant execute on function public.change_pins(uuid, text, text, text, text) to anon, authenticated';
  end if;
end $$;
