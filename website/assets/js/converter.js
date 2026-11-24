// Интерфейс онлайн-конвертера на главной странице (русская версия).
import * as H from './hebrew-calendar.js';

const NOM = { NISAN: 'Нисан', IYAR: 'Ияр', SIVAN: 'Сиван', TAMMUZ: 'Таммуз', AV: 'Ав', ELUL: 'Элул', TISHREI: 'Тишрей', CHESHVAN: 'Хешван', KISLEV: 'Кислев', TEVET: 'Тевет', SHEVAT: 'Шват', ADAR: 'Адар', ADAR_I: 'Адар I', ADAR_II: 'Адар II' };
const GEN = { NISAN: 'Нисана', IYAR: 'Ияра', SIVAN: 'Сивана', TAMMUZ: 'Таммуза', AV: 'Ава', ELUL: 'Элула', TISHREI: 'Тишрея', CHESHVAN: 'Хешвана', KISLEV: 'Кислева', TEVET: 'Тевета', SHEVAT: 'Швата', ADAR: 'Адара', ADAR_I: 'Адара I', ADAR_II: 'Адара II' };
const HE = { NISAN: 'ניסן', IYAR: 'אייר', SIVAN: 'סיוון', TAMMUZ: 'תמוז', AV: 'אב', ELUL: 'אלול', TISHREI: 'תשרי', CHESHVAN: 'חשוון', KISLEV: 'כסלו', TEVET: 'טבת', SHEVAT: 'שבט', ADAR: 'אדר', ADAR_I: 'אדר א׳', ADAR_II: 'אדר ב׳' };
const G_NOM = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
const G_GEN = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
const WD = ['воскресенье', 'понедельник', 'вторник', 'среда', 'четверг', 'пятница', 'суббота'];
const WD_SHORT = ['вс', 'пн', 'вт', 'ср', 'чт', 'пт', 'сб'];

const hebText = (h) => `${h.day} ${GEN[h.month]} ${h.year}`;
const hebNative = (h) => `${H.gematriya(h.day)} ב${HE[h.month]} ${H.gematriya(h.year)}`;
const gregText = (g) => `${g.d} ${G_GEN[g.m - 1]} ${g.y}`;
const gregShort = (g) => `${WD_SHORT[H.weekday(g)]}, ${gregText(g)}`;
const plural = (n, a, b, c) => { const m10 = n % 10, m100 = n % 100; return m10 === 1 && m100 !== 11 ? a : m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20) ? b : c; };
const prevDay = (g) => { const d = new Date(Date.UTC(g.y, g.m - 1, g.d - 1)); return { y: d.getUTCFullYear(), m: d.getUTCMonth() + 1, d: d.getUTCDate() }; };
const daysBetween = (a, b) => Math.round((Date.UTC(b.y, b.m - 1, b.d) - Date.UTC(a.y, a.m - 1, a.d)) / 86400000);

const $ = (id) => document.getElementById(id);
const now = new Date();
const today = { y: now.getFullYear(), m: now.getMonth() + 1, d: now.getDate() };

const state = { mode: 'g2h', after: false };

function fillSelect(sel, items, value) {
  sel.innerHTML = items.map(([v, t]) => `<option value="${v}">${t}</option>`).join('');
  if (value != null && items.some(([v]) => String(v) === String(value))) sel.value = value;
}

function setupInputs() {
  fillSelect($('g-day'), Array.from({ length: 31 }, (_, i) => [i + 1, i + 1]), today.d);
  fillSelect($('g-month'), G_NOM.map((n, i) => [i + 1, n]), today.m);
  $('g-year').value = today.y;
  const h = H.gregorianToHebrew(today);
  fillSelect($('h-day'), Array.from({ length: 30 }, (_, i) => [i + 1, i + 1]), h.day);
  $('h-year').value = h.year;
  refreshHebMonths(h.month);
}

function refreshHebMonths(keep) {
  const y = parseInt($('h-year').value, 10);
  const valid = Number.isInteger(y) && y > 3761 && y < 7000;
  const months = valid ? H.monthsOfYear(y) : ['TISHREI', 'CHESHVAN', 'KISLEV', 'TEVET', 'SHEVAT', 'ADAR', 'ADAR_I', 'ADAR_II', 'NISAN', 'IYAR', 'SIVAN', 'TAMMUZ', 'AV', 'ELUL'];
  let want = keep ?? $('h-month').value;
  if (valid && !months.includes(want)) want = want === 'ADAR' ? 'ADAR_II' : want?.startsWith('ADAR') ? 'ADAR' : months[0];
  fillSelect($('h-month'), months.map((c) => [c, NOM[c]]), want);
}

function readInput() {
  if (state.mode === 'g2h') {
    const g = { y: parseInt($('g-year').value, 10), m: parseInt($('g-month').value, 10), d: parseInt($('g-day').value, 10) };
    if (!(g.y >= 1600 && g.y <= 2200)) return { error: 'Введите год от 1600 до 2200.' };
    const real = new Date(Date.UTC(g.y, g.m - 1, g.d));
    if (real.getUTCMonth() + 1 !== g.m) return { error: 'Такой даты не существует.' };
    return { g, h: H.gregorianToHebrew(g, state.after) };
  }
  const h = { year: parseInt($('h-year').value, 10), month: $('h-month').value, day: parseInt($('h-day').value, 10) };
  if (!(h.year >= 5360 && h.year <= 5960)) return { error: 'Введите еврейский год от 5360 до 5960.' };
  if (!H.isValid(h)) return { error: `В месяце ${NOM[h.month]} ${h.year} года ${H.daysInMonth(h.year, h.month)} дней.` };
  return { h, g: H.hebrewToGregorian(h) };
}

function render() {
  $('g-fields').hidden = state.mode !== 'g2h';
  $('sunset').hidden = state.mode !== 'g2h';
  $('h-fields').hidden = state.mode !== 'h2g';
  document.querySelectorAll('[data-mode]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.mode === state.mode)));
  document.querySelectorAll('[data-after]').forEach((b) => b.setAttribute('aria-pressed', String((b.dataset.after === '1') === state.after)));

  const r = readInput();
  const out = $('result');
  if (r.error) {
    out.className = 'result error';
    out.innerHTML = `<div class="note">${r.error}</div>`;
    $('extras').hidden = true;
    return;
  }
  out.className = 'result';
  if (state.mode === 'g2h') {
    out.innerHTML = `<div class="label">Еврейская дата</div>
      <div class="big">${hebText(r.h)}</div>
      <div class="he" dir="rtl" lang="he">${hebNative(r.h)}</div>
      <div class="note">${WD[H.weekday(r.g)]}${state.after ? ' вечером — еврейский день уже наступил' : ''}</div>`;
  } else {
    out.innerHTML = `<div class="label">Григорианская дата</div>
      <div class="big">${WD[H.weekday(r.g)]}, ${gregText(r.g)}</div>
      <div class="note">Еврейский день начинается накануне вечером, с заходом солнца ${gregText(prevDay(r.g)).replace(/ \d+$/, '')}.</div>`;
  }

  // Дополнительно: бар/бат-мицва, ближайший день рождения и йорцайт
  const m = H.mitzvahDates(r.h);
  const bday = H.nextOccurrence('BIRTHDAY', r.h, today);
  const yz = H.nextOccurrence('YAHRZEIT', r.h, today);
  const rows = [];
  if (bday) rows.push(`<p>Ближайший еврейский день рождения: <b>${hebText(bday.hebrew)}</b> — ${gregShort(bday.gregorian)}${bday.years > 0 ? `, исполнится ${bday.years} ${plural(bday.years, 'год', 'года', 'лет')}` : ''}.</p>`);
  rows.push(`<p>Бар-мицва (13 лет): <b>${hebText(m.bar.hebrew)}</b> — ${gregShort(m.bar.gregorian)}</p>`);
  rows.push(`<p>Бат-мицва (12 лет): <b>${hebText(m.bat.hebrew)}</b> — ${gregShort(m.bat.gregorian)}</p>`);
  $('birth').innerHTML = `<h3>Если это дата рождения</h3>${rows.join('')}`;

  let yzHtml = '<h3>Если это дата смерти (йорцайт)</h3>';
  if (yz) {
    const ty = yz.hebrew.year;
    yzHtml += `<p>Ближайший йорцайт: <b>${hebText(yz.hebrew)}</b> — ${gregShort(yz.gregorian)}. Свечу зажигают накануне вечером.</p><table>`;
    for (let y = ty; y < ty + 10; y++) {
      const h = H.hebrewAnniversary('YAHRZEIT', r.h, y);
      yzHtml += `<tr><td>${hebText(h)}</td><td>${gregShort(H.hebrewToGregorian(h))}</td></tr>`;
    }
    yzHtml += '</table>';
  } else yzHtml += '<p>Йорцайт начинается со следующего года после даты.</p>';
  $('yahrzeit').innerHTML = yzHtml;
  $('extras').hidden = false;

  const left = bday ? daysBetween(today, bday.gregorian) : null;
  $('cta').textContent = left != null
    ? `До ближайшего еврейского дня рождения — ${left} ${plural(left, 'день', 'дня', 'дней')}. Приложение LeDor напомнит накануне вечером и в сам день.`
    : 'Приложение LeDor напомнит о дате каждый год — накануне вечером и в сам день.';
}

document.querySelectorAll('[data-mode]').forEach((b) => b.addEventListener('click', () => { state.mode = b.dataset.mode; render(); }));
document.querySelectorAll('[data-after]').forEach((b) => b.addEventListener('click', () => { state.after = b.dataset.after === '1'; render(); }));
['g-day', 'g-month', 'g-year', 'h-day', 'h-month'].forEach((id) => $(id).addEventListener('input', render));
$('h-year').addEventListener('input', () => { refreshHebMonths(); render(); });

setupInputs();
render();

// Галерея скриншотов: стрелки на компьютере
const shots = $('shots');
document.querySelectorAll('[data-scroll]').forEach((b) => b.addEventListener('click', () => shots.scrollBy({ left: Number(b.dataset.scroll) * 260, behavior: 'smooth' })));
