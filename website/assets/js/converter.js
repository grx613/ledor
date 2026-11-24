// Онлайн-конвертер дат. Тексты — из <script id="i18n"> на странице (tools/site/i18n/<lang>.mjs → js).
import * as H from './hebrew-calendar.js';

const T = JSON.parse(document.getElementById('i18n').textContent);
const L = T.lang;
const MONTH_ORDER = ['TISHREI', 'CHESHVAN', 'KISLEV', 'TEVET', 'SHEVAT', 'ADAR', 'ADAR_I', 'ADAR_II', 'NISAN', 'IYAR', 'SIVAN', 'TAMMUZ', 'AV', 'ELUL'];

const num = (n) => (T.letters ? H.gematriya(n) : String(n));
const fill = (tpl, v) => tpl.replace(/\{(\w+)\}/g, (_, k) => v[k]);
const rules = new Intl.PluralRules(L);
const plural = (forms, n) => fill(forms[rules.select(n)] ?? forms.other, { n });
const date = (g) => new Date(Date.UTC(g.y, g.m - 1, g.d, 12));
const fmt = (opts) => { const f = new Intl.DateTimeFormat(L, { timeZone: 'UTC', ...opts }); return (g) => f.format(date(g)).replace(/\s?г\.$/, ''); };
const gregText = fmt({ day: 'numeric', month: 'long', year: 'numeric' });
const gregDayMonth = fmt({ day: 'numeric', month: 'long' });
const weekdayLong = fmt({ weekday: 'long' });
const gregShort = fmt({ weekday: 'short', day: 'numeric', month: 'long', year: 'numeric' });
const gregFull = fmt({ weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
const monthName = (m) => { const s = new Intl.DateTimeFormat(L, { month: 'long', timeZone: 'UTC' }).format(new Date(Date.UTC(2000, m - 1, 1, 12))); return s.charAt(0).toUpperCase() + s.slice(1); };
const cap = (s) => s.charAt(0).toUpperCase() + s.slice(1);

const hebText = (h) => `${num(h.day)} ${T.monthsGen[h.month]} ${num(h.year)}`;
const hebNative = (h) => `${H.gematriya(h.day)} ב${HE[h.month]} ${H.gematriya(h.year)}`;
const HE = { NISAN: 'ניסן', IYAR: 'אייר', SIVAN: 'סיוון', TAMMUZ: 'תמוז', AV: 'אב', ELUL: 'אלול', TISHREI: 'תשרי', CHESHVAN: 'חשוון', KISLEV: 'כסלו', TEVET: 'טבת', SHEVAT: 'שבט', ADAR: 'אדר', ADAR_I: 'אדר א׳', ADAR_II: 'אדר ב׳' };
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
  fillSelect($('g-month'), Array.from({ length: 12 }, (_, i) => [i + 1, monthName(i + 1)]), today.m);
  $('g-year').value = today.y;
  const h = H.gregorianToHebrew(today);
  fillSelect($('h-day'), Array.from({ length: 30 }, (_, i) => [i + 1, num(i + 1)]), h.day);
  $('h-year').value = h.year;
  refreshHebMonths(h.month);
}

function refreshHebMonths(keep) {
  const y = parseInt($('h-year').value, 10);
  const valid = Number.isInteger(y) && y > 3761 && y < 7000;
  const months = valid ? H.monthsOfYear(y) : MONTH_ORDER;
  let want = keep ?? $('h-month').value;
  if (valid && !months.includes(want)) want = want === 'ADAR' ? 'ADAR_II' : want?.startsWith('ADAR') ? 'ADAR' : months[0];
  fillSelect($('h-month'), months.map((c) => [c, T.monthsNom[c]]), want);
}

function readInput() {
  if (state.mode === 'g2h') {
    const g = { y: parseInt($('g-year').value, 10), m: parseInt($('g-month').value, 10), d: parseInt($('g-day').value, 10) };
    if (!(g.y >= 1600 && g.y <= 2200)) return { error: T.errGYear };
    const real = new Date(Date.UTC(g.y, g.m - 1, g.d));
    if (real.getUTCMonth() + 1 !== g.m) return { error: T.errNoDate };
    return { g, h: H.gregorianToHebrew(g, state.after) };
  }
  const h = { year: parseInt($('h-year').value, 10), month: $('h-month').value, day: parseInt($('h-day').value, 10) };
  if (!(h.year >= 5360 && h.year <= 5960)) return { error: T.errHYear };
  if (!H.isValid(h)) return { error: fill(T.errMonthDays, { month: T.monthsNom[h.month], year: num(h.year), n: H.daysInMonth(h.year, h.month) }) };
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
    out.innerHTML = `<div class="label">${T.hebLabel}</div>
      <div class="big">${hebText(r.h)}</div>
      ${T.letters ? '' : `<div class="he" dir="rtl" lang="he">${hebNative(r.h)}</div>`}
      <div class="note">${cap(weekdayLong(r.g))}${state.after ? T.evening : ''}</div>`;
  } else {
    out.innerHTML = `<div class="label">${T.gregLabel}</div>
      <div class="big">${cap(gregFull(r.g))}</div>
      <div class="note">${fill(T.eveNote, { date: gregDayMonth(prevDay(r.g)) })}</div>`;
  }

  const m = H.mitzvahDates(r.h);
  const bday = H.nextOccurrence('BIRTHDAY', r.h, today);
  const yz = H.nextOccurrence('YAHRZEIT', r.h, today);
  const rows = [];
  if (bday) rows.push(`<p>${fill(T.nextBday, { heb: hebText(bday.hebrew), greg: gregShort(bday.gregorian) })}${bday.years > 0 ? plural(T.turns, bday.years) : ''}.</p>`);
  rows.push(`<p>${T.bar}: <b>${hebText(m.bar.hebrew)}</b> — ${gregShort(m.bar.gregorian)}</p>`);
  rows.push(`<p>${T.bat}: <b>${hebText(m.bat.hebrew)}</b> — ${gregShort(m.bat.gregorian)}</p>`);
  $('birth').innerHTML = `<h3>${T.ifBirth}</h3>${rows.join('')}`;

  let yzHtml = `<h3>${T.ifDeath}</h3>`;
  if (yz) {
    const ty = yz.hebrew.year;
    yzHtml += `<p>${fill(T.nextYz, { heb: hebText(yz.hebrew), greg: gregShort(yz.gregorian) })}</p><table>`;
    for (let y = ty; y < ty + 10; y++) {
      const h = H.hebrewAnniversary('YAHRZEIT', r.h, y);
      yzHtml += `<tr><td>${hebText(h)}</td><td>${gregShort(H.hebrewToGregorian(h))}</td></tr>`;
    }
    yzHtml += '</table>';
  } else yzHtml += `<p>${T.yzNextYear}</p>`;
  $('yahrzeit').innerHTML = yzHtml;
  $('extras').hidden = false;

  const left = bday ? daysBetween(today, bday.gregorian) : null;
  $('cta').textContent = (left != null ? plural(T.ctaDays, left) + ' ' : '') + T.ctaApp;
}

document.querySelectorAll('[data-mode]').forEach((b) => b.addEventListener('click', () => { state.mode = b.dataset.mode; render(); }));
document.querySelectorAll('[data-after]').forEach((b) => b.addEventListener('click', () => { state.after = b.dataset.after === '1'; render(); }));
['g-day', 'g-month', 'g-year', 'h-day', 'h-month'].forEach((id) => $(id).addEventListener('input', render));
$('h-year').addEventListener('input', () => { refreshHebMonths(); render(); });

setupInputs();
render();
