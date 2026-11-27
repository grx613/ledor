// Интеграционный smoke-тест RPC-функций Supabase (docs/04-backend.md §7).
//
// Запуск (Node 22, без зависимостей):
//   SUPABASE_URL=https://<project>.supabase.co SUPABASE_KEY=<публичный ключ> node --test tools/rpc-smoke/smoke.mjs
//
// Тест создаёт два списка со случайными именами и удаляет их в конце, даже если сценарии упали.
// Лимит create_list — 10 в час с адреса: запускать не чаще 5 раз в час.

import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const URL_ = process.env.SUPABASE_URL;
const KEY = process.env.SUPABASE_KEY;
if (!URL_ || !KEY) {
  console.error('Задайте SUPABASE_URL и SUPABASE_KEY (публичный ключ, НЕ service_role).');
  process.exit(2);
}

async function rpc(fn, body) {
  const r = await fetch(`${URL_.replace(/\/$/, '')}/rest/v1/rpc/${fn}`, {
    method: 'POST',
    headers: { apikey: KEY, Authorization: `Bearer ${KEY}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const text = await r.text();
  assert.equal(r.status, 200, `${fn}: HTTP ${r.status} ${text}`);
  return JSON.parse(text);
}

const suffix = Math.random().toString(36).slice(2, 8);
const NAME = `Smoke Test ${suffix}`;
const OTHER = `Smoke Other ${suffix}`;
const READ_PIN = '4821';
const MASTER_PIN = '73920461';

const cleanup = []; // [listId, token, masterPin]
after(async () => {
  for (const [id, token, pin] of cleanup) {
    try { await rpc('delete_list', { p_list_id: id, p_token: token, p_master_pin: pin }); } catch { /* уже удалён */ }
  }
});

const ev = (over = {}) => ({
  id: randomUUID(), deleted: false, title: 'Сара', type: 'BIRTHDAY', custom_label: null, recurring: true,
  observe_by: 'HEBREW', input_calendar: 'GREGORIAN', g_year: 1990, g_month: 4, g_day: 23, after_sunset: false,
  h_year: 5750, h_month: 'NISAN', h_day: 28, note: null, ...over,
});

let list, editorToken, readerToken;

test('1. create_list и NAME_TAKEN без учёта регистра и пробелов', async () => {
  const r = await rpc('create_list', { p_name: NAME, p_read_pin: READ_PIN, p_master_pin: MASTER_PIN });
  assert.equal(r.ok, true, JSON.stringify(r));
  assert.equal(r.role, 'EDITOR');
  assert.equal(r.read_pin, READ_PIN);
  list = r.list; editorToken = r.token;
  cleanup.push([list.id, editorToken, MASTER_PIN]);
  const dup = await rpc('create_list', { p_name: `  ${NAME.toUpperCase().replace(' ', '   ')} `, p_read_pin: '1111', p_master_pin: '22222222' });
  assert.deepEqual([dup.ok, dup.error], [false, 'NAME_TAKEN']);
});

test('2. list_exists', async () => {
  assert.equal((await rpc('list_exists', { p_name: NAME.toLowerCase() })).exists, true);
  assert.equal((await rpc('list_exists', { p_name: `nope ${randomUUID()}` })).exists, false);
});

test('3. join_list: обычный ПИН → READER, Мастер-ПИН → EDITOR', async () => {
  const r = await rpc('join_list', { p_name: NAME, p_pin: READ_PIN });
  assert.equal(r.role, 'READER'); readerToken = r.token;
  const e = await rpc('join_list', { p_name: NAME, p_pin: MASTER_PIN });
  assert.equal(e.role, 'EDITOR');
});

test('4. читатель не может писать', async () => {
  const r = await rpc('push_events', { p_list_id: list.id, p_token: readerToken, p_events: [ev()] });
  assert.deepEqual([r.ok, r.error], [false, 'FORBIDDEN']);
});

let events;
test('5. редактор пишет 3 события, читатель их получает', async () => {
  events = [ev(), ev({ title: 'Дедушка', type: 'YAHRZEIT' }), ev({ title: 'Свадьба', type: 'WEDDING' })];
  const r = await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: events });
  assert.equal(r.ok, true, JSON.stringify(r));
  assert.equal(r.revision, 1);
  assert.equal(r.accepted.length, 3);
  const p = await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: 0 });
  assert.equal(p.events.length, 3);
  assert.equal(p.list.revision, 1);
});

test('6. изменение одного события → ровно одно в pull since=1', async () => {
  await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: [{ ...events[0], title: 'Сара Коэн' }] });
  const p = await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: 1 });
  assert.equal(p.events.length, 1);
  assert.equal(p.events[0].title, 'Сара Коэн');
});

test('7. удаление → «надгробие» без персональных данных', async () => {
  await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: [{ id: events[1].id, deleted: true }] });
  const p = await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: 2 });
  assert.equal(p.events.length, 1);
  assert.equal(p.events[0].deleted, true);
  assert.equal(p.events[0].title, '');
  assert.equal(p.events[0].g_year, null);
});

test('8. чужой id → FOREIGN_ID, чужое событие не изменено', async () => {
  const o = await rpc('create_list', { p_name: OTHER, p_read_pin: '5555', p_master_pin: '66666666' });
  assert.equal(o.ok, true);
  cleanup.push([o.list.id, o.token, '66666666']);
  const r = await rpc('push_events', { p_list_id: o.list.id, p_token: o.token, p_events: [{ ...events[2], title: 'Взлом' }] });
  assert.deepEqual(r.rejected.map((x) => x.reason), ['FOREIGN_ID']);
  const p = await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: 0 });
  assert.equal(p.events.find((e) => e.id === events[2].id).title, 'Свадьба');
});

test('9. 5 неверных ПИН → блокировка; старые токены работают', async () => {
  const left = [];
  for (let i = 0; i < 5; i++) {
    const r = await rpc('join_list', { p_name: NAME, p_pin: '000000' });
    assert.equal(r.error, 'WRONG_PIN');
    left.push(r.attempts_left);
  }
  assert.deepEqual(left, [4, 3, 2, 1, 0]);
  const locked = await rpc('join_list', { p_name: NAME, p_pin: MASTER_PIN });
  assert.equal(locked.error, 'LOCKED', 'счётчик не сохранился — вероятно, ошибка возвращается через RAISE');
  assert.ok(locked.retry_after_sec > 0);
  const p = await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: 0 });
  assert.equal(p.ok, true);
});

test('10. прямой доступ к таблицам закрыт', async () => {
  for (const t of ['lists', 'events', 'members']) {
    const r = await fetch(`${URL_.replace(/\/$/, '')}/rest/v1/${t}?select=*`, { headers: { apikey: KEY, Authorization: `Bearer ${KEY}` } });
    assert.notEqual(r.status, 200, `таблица ${t} доступна через API`);
  }
});

test('10b. observe_by = BOTH (миграция 0002)', async () => {
  const e = ev({ title: 'Оба календаря', observe_by: 'BOTH' });
  const r = await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: [e] });
  assert.equal(r.ok, true, JSON.stringify(r));
  const p = await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: r.revision - 1 });
  assert.equal(p.events.find((x) => x.id === e.id)?.observe_by, 'BOTH');
});

test('11. delete_list: неверный ПИН → WRONG_PIN / LOCKED; верный удаляет', async () => {
  // Основной список заблокирован после сценария 9 — проверяем на втором списке.
  const [id, token, pin] = cleanup[1];
  const bad = await rpc('delete_list', { p_list_id: id, p_token: token, p_master_pin: '99999999' });
  assert.equal(bad.error, 'WRONG_PIN');
  const ok = await rpc('delete_list', { p_list_id: id, p_token: token, p_master_pin: pin });
  assert.equal(ok.ok, true);
  const gone = await rpc('pull_changes', { p_list_id: id, p_token: token, p_since: 0 });
  assert.equal(gone.error, 'LIST_DELETED');
});

test('12. невалидные данные', async () => {
  assert.equal((await rpc('create_list', { p_name: 'ab', p_read_pin: '1234', p_master_pin: '123456' })).error, 'INVALID_NAME');
  assert.equal((await rpc('create_list', { p_name: `x${suffix}x`, p_read_pin: 'abcd', p_master_pin: '123456' })).error, 'INVALID_PIN');
  assert.equal((await rpc('create_list', { p_name: `x${suffix}x`, p_read_pin: '123456', p_master_pin: '123456' })).error, 'PINS_EQUAL');
  const bad = await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: [ev({ type: 'X' })] });
  assert.equal(bad.error, 'INVALID_EVENT');
});

test('13. submit_feedback (миграция 0002)', async () => {
  assert.equal((await rpc('submit_feedback', { p_message: '   ', p_contact: null, p_meta: {} })).error, 'FEEDBACK_EMPTY');
  assert.equal((await rpc('submit_feedback', { p_message: 'x'.repeat(2001), p_contact: null, p_meta: {} })).error, 'FEEDBACK_TOO_LONG');
  const r = await rpc('submit_feedback', { p_message: `Smoke-тест ${suffix}`, p_contact: null, p_meta: { app_version: 'smoke' } });
  assert.equal(r.ok, true, JSON.stringify(r));
});

test('14. change_pins (миграция 0003): только по Мастер-ПИНу; старые токены работают', async () => {
  const name = `Smoke Pins ${suffix}`;
  const c = await rpc('create_list', { p_name: name, p_read_pin: '493817', p_master_pin: '61803398' });
  assert.equal(c.ok, true, JSON.stringify(c));
  const entry = [c.list.id, c.token, '61803398'];
  cleanup.push(entry);
  const reader = (await rpc('join_list', { p_name: name, p_pin: '493817' })).token;
  const call = (token, master, read, newMaster) =>
    rpc('change_pins', { p_list_id: c.list.id, p_token: token, p_master_pin: master, p_new_read_pin: read, p_new_master_pin: newMaster });

  assert.equal((await call(reader, '61803398', '0770', '61803398')).error, 'FORBIDDEN');
  assert.equal((await call(c.token, '11111111', '0770', '61803398')).error, 'WRONG_PIN');
  assert.equal((await call(c.token, '61803398', '077', '61803398')).error, 'INVALID_PIN');
  assert.equal((await call(c.token, '61803398', '271828', '271828')).error, 'PINS_EQUAL');

  const ok = await call(c.token, '61803398', '0770', '27182818');
  assert.deepEqual([ok.ok, ok.read_pin], [true, '0770']);
  entry[2] = '27182818';
  assert.equal((await rpc('join_list', { p_name: name, p_pin: '493817' })).error, 'WRONG_PIN');
  assert.equal((await rpc('join_list', { p_name: name, p_pin: '0770' })).role, 'READER');
  assert.equal((await rpc('join_list', { p_name: name, p_pin: '27182818' })).role, 'EDITOR');
  assert.equal((await rpc('pull_changes', { p_list_id: c.list.id, p_token: reader, p_since: 0 })).read_pin, '0770');
});

test('15. monthly (миграция 0004): сохраняется; правка без поля (старое приложение) его не стирает', async () => {
  const e = ev({ title: 'Квартира', type: 'OTHER', custom_label: 'Квартплата', observe_by: 'GREGORIAN', g_year: 2026, g_month: 12, g_day: 10,
    h_year: 5787, h_month: 'KISLEV', h_day: 30, monthly: true });
  const r = await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: [e] });
  assert.equal(r.ok, true, JSON.stringify(r));
  const got = async () => (await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: 0 })).events.find((x) => x.id === e.id);
  assert.equal((await got()).monthly, true);
  const { monthly, ...old } = e;
  assert.equal((await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: [{ ...old, title: 'Квартира 2' }] })).ok, true);
  const after = await got();
  assert.equal(after.title, 'Квартира 2');
  assert.equal(after.monthly, true);
  assert.equal((await rpc('push_events', { p_list_id: list.id, p_token: editorToken, p_events: [{ ...e, monthly: false }] })).ok, true);
  assert.equal((await got()).monthly, false);
  // У старых событий поле есть и равно false.
  const p = await rpc('pull_changes', { p_list_id: list.id, p_token: readerToken, p_since: 0 });
  assert.ok(p.events.every((x) => typeof x.monthly === 'boolean'));
});
