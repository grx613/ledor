// Арифметика еврейского календаря для онлайн-конвертера (без сторонних библиотек).
// Правила годовщин — строго по docs/03-calendar-domain.md §5 (B1–B6, Y1–Y7).
// Проверка: node --test website/tests/ (все строки spec/golden/*.json).

// Внутренняя нумерация месяцев: 1 = Нисан … 12 = Адар (Адар I в високосный год), 13 = Адар II.
const NISAN = 1, IYAR = 2, TAMMUZ = 4, ELUL = 6, TISHREI = 7, CHESHVAN = 8, KISLEV = 9, TEVET = 10, SHEVAT = 11, ADAR = 12, ADAR_II = 13;
const EPOCH = -1373428; // R.D. начала еврейской эры
const NAMES = [null, 'NISAN', 'IYAR', 'SIVAN', 'TAMMUZ', 'AV', 'ELUL', 'TISHREI', 'CHESHVAN', 'KISLEV', 'TEVET', 'SHEVAT'];

export const isLeapYear = (y) => (1 + y * 7) % 19 < 7;
export const monthsInYear = (y) => (isLeapYear(y) ? 13 : 12);

const elapsedCache = new Map();
function elapsedDays(year) {
  if (elapsedCache.has(year)) return elapsedCache.get(year);
  const prev = year - 1;
  const mElapsed = 235 * Math.floor(prev / 19) + 12 * (prev % 19) + Math.floor(((prev % 19) * 7 + 1) / 19);
  const pElapsed = 204 + 793 * (mElapsed % 1080);
  const hElapsed = 5 + 12 * mElapsed + 793 * Math.floor(mElapsed / 1080) + Math.floor(pElapsed / 1080);
  const parts = (pElapsed % 1080) + 1080 * (hElapsed % 24);
  const day = 1 + 29 * mElapsed + Math.floor(hElapsed / 24);
  const alt = day + (parts >= 19440 || (day % 7 === 2 && parts >= 9924 && !isLeapYear(year)) ||
    (day % 7 === 1 && parts >= 16789 && isLeapYear(prev)) ? 1 : 0);
  const r = alt + (alt % 7 === 0 || alt % 7 === 3 || alt % 7 === 5 ? 1 : 0);
  elapsedCache.set(year, r);
  return r;
}

export const daysInYear = (y) => elapsedDays(y + 1) - elapsedDays(y);
export const isCheshvanLong = (y) => daysInYear(y) % 10 === 5;
export const isKislevShort = (y) => daysInYear(y) % 10 === 3;

function daysInMonthNum(m, y) {
  if (m === IYAR || m === TAMMUZ || m === ELUL || m === TEVET || m === ADAR_II) return 29;
  if (m === ADAR && !isLeapYear(y)) return 29;
  if (m === CHESHVAN && !isCheshvanLong(y)) return 29;
  if (m === KISLEV && isKislevShort(y)) return 29;
  return 30;
}

// ── Коды месяцев (как в приложении и БД) ──
export function codeOf(y, m) {
  if (m <= 11) return NAMES[m];
  if (m === 13) return 'ADAR_II';
  return isLeapYear(y) ? 'ADAR_I' : 'ADAR';
}
export function numOf(code) {
  if (code === 'ADAR' || code === 'ADAR_I') return ADAR;
  if (code === 'ADAR_II') return ADAR_II;
  const i = NAMES.indexOf(code);
  if (i < 1) throw new Error(`month ${code}`);
  return i;
}
export function isMonthInYear(code, y) {
  if (code === 'ADAR') return !isLeapYear(y);
  if (code === 'ADAR_I' || code === 'ADAR_II') return isLeapYear(y);
  return NAMES.includes(code);
}
export function daysInMonth(y, code) { return daysInMonthNum(numOf(code), y); }
export function monthsOfYear(y) {
  const adar = isLeapYear(y) ? ['ADAR_I', 'ADAR_II'] : ['ADAR'];
  return ['TISHREI', 'CHESHVAN', 'KISLEV', 'TEVET', 'SHEVAT', ...adar, 'NISAN', 'IYAR', 'SIVAN', 'TAMMUZ', 'AV', 'ELUL'];
}
export function isValid(h) {
  return Number.isInteger(h.year) && h.year >= 3762 && isMonthInYear(h.month, h.year) &&
    Number.isInteger(h.day) && h.day >= 1 && h.day <= daysInMonth(h.year, h.month);
}

// ── Абсолютные дни (R.D.) ──
function hebrewToAbs(y, m, d) {
  let t = d;
  if (m < TISHREI) {
    for (let mm = TISHREI; mm <= monthsInYear(y); mm++) t += daysInMonthNum(mm, y);
    for (let mm = NISAN; mm < m; mm++) t += daysInMonthNum(mm, y);
  } else {
    for (let mm = TISHREI; mm < m; mm++) t += daysInMonthNum(mm, y);
  }
  return EPOCH + elapsedDays(y) + t - 1;
}

function absToHebrew(abs) {
  let year = Math.floor((abs - EPOCH) / 365.24682220597794);
  while (EPOCH + elapsedDays(year + 1) <= abs) year++;
  let month = abs < hebrewToAbs(year, NISAN, 1) ? TISHREI : NISAN;
  while (abs > hebrewToAbs(year, month, daysInMonthNum(month, year))) month = (month % monthsInYear(year)) + 1;
  return { year, month: codeOf(year, month), day: 1 + abs - hebrewToAbs(year, month, 1) };
}

const isGregLeap = (y) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
function gregToAbs({ y, m, d }) {
  const p = y - 1;
  return 365 * p + Math.floor(p / 4) - Math.floor(p / 100) + Math.floor(p / 400) +
    Math.floor((367 * m - 362) / 12) + (m <= 2 ? 0 : isGregLeap(y) ? -1 : -2) + d;
}
function absToGreg(abs) {
  const d0 = abs - 1;
  const n400 = Math.floor(d0 / 146097), d1 = d0 % 146097;
  const n100 = Math.floor(d1 / 36524), d2 = d1 % 36524;
  const n4 = Math.floor(d2 / 1461), d3 = d2 % 1461;
  const n1 = Math.floor(d3 / 365);
  let y = 400 * n400 + 100 * n100 + 4 * n4 + n1;
  if (!(n100 === 4 || n1 === 4)) y += 1;
  const prior = abs - gregToAbs({ y, m: 1, d: 1 });
  const corr = abs < gregToAbs({ y, m: 3, d: 1 }) ? 0 : isGregLeap(y) ? 1 : 2;
  const m = Math.floor((12 * (prior + corr) + 373) / 367);
  return { y, m, d: abs - gregToAbs({ y, m, d: 1 }) + 1 };
}

// ── Публичный API ──
export function gregorianToHebrew(g, afterSunset = false) {
  return absToHebrew(gregToAbs(g) + (afterSunset ? 1 : 0));
}
export function hebrewToGregorian(h) {
  return absToGreg(hebrewToAbs(h.year, numOf(h.month), h.day));
}
export function weekday(g) { return ((gregToAbs(g) % 7) + 7) % 7; } // 0 = воскресенье

const lastAdar = (y) => (isLeapYear(y) ? 'ADAR_II' : 'ADAR');

/** Дата ежегодного события в еврейском году targetYear. origin.year может быть null. */
export function hebrewAnniversary(rule, origin, y, adarPolicy = 'ADAR_I') {
  const leap = isLeapYear(y);
  const { month: m, day: d } = origin;
  if (rule === 'BIRTHDAY') {
    if (m === 'ADAR' || m === 'ADAR_II') return { year: y, month: lastAdar(y), day: d };
    if (m === 'ADAR_I' && d === 30 && !leap) return { year: y, month: 'NISAN', day: 1 };
    if (m === 'ADAR_I') return { year: y, month: leap ? 'ADAR_I' : 'ADAR', day: d };
    if (m === 'CHESHVAN' && d === 30 && !isCheshvanLong(y)) return { year: y, month: 'KISLEV', day: 1 };
    if (m === 'KISLEV' && d === 30 && isKislevShort(y)) return { year: y, month: 'TEVET', day: 1 };
    return { year: y, month: m, day: d };
  }
  // YAHRZEIT
  const oy = origin.year;
  if (m === 'CHESHVAN' && d === 30 && oy != null && !isCheshvanLong(oy + 1)) return { year: y, month: 'CHESHVAN', day: daysInMonth(y, 'CHESHVAN') };
  if (m === 'KISLEV' && d === 30 && oy != null && isKislevShort(oy + 1)) return { year: y, month: 'KISLEV', day: daysInMonth(y, 'KISLEV') };
  let base;
  if (m === 'ADAR_II') base = { year: y, month: lastAdar(y), day: d };
  else if (m === 'ADAR_I' && d === 30 && !leap) base = { year: y, month: 'SHEVAT', day: 30 };
  else if (m === 'ADAR_I') base = { year: y, month: leap ? 'ADAR_I' : 'ADAR', day: d };
  else if (m === 'ADAR') base = { year: y, month: !leap ? 'ADAR' : adarPolicy === 'ADAR_II' ? 'ADAR_II' : 'ADAR_I', day: d };
  else base = { year: y, month: m, day: d };
  if (base.month === 'CHESHVAN' && base.day === 30 && !isCheshvanLong(y)) return { year: y, month: 'KISLEV', day: 1 };
  if (base.month === 'KISLEV' && base.day === 30 && isKislevShort(y)) return { year: y, month: 'TEVET', day: 1 };
  return base;
}

const cmp = (a, b) => gregToAbs(a) - gregToAbs(b);

/** Ближайшее наступление (дневная гражданская дата >= today); firstOffset: 1 — день рождения/йорцайт. */
export function nextOccurrence(rule, origin, today, firstOffset = 1) {
  const ty = gregorianToHebrew(today).year;
  for (let y = ty; y <= ty + 2; y++) {
    if (origin.year != null && y < origin.year + firstOffset) continue;
    const h = hebrewAnniversary(rule, origin, y);
    const g = hebrewToGregorian(h);
    if (cmp(g, today) >= 0) return { hebrew: h, gregorian: g, years: origin.year != null ? y - origin.year : null };
  }
  return null;
}

/** Бар-мицва (13) и бат-мицва (12) по правилу дня рождения. */
export function mitzvahDates(birth) {
  const f = (n) => { const h = hebrewAnniversary('BIRTHDAY', birth, birth.year + n); return { hebrew: h, gregorian: hebrewToGregorian(h) }; };
  return { bar: f(13), bat: f(12) };
}

// ── Иврит: числа буквами (гематрия) ──
const ONES = ['', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט'];
const TENS = ['', 'י', 'כ', 'ל', 'מ', 'נ', 'ס', 'ע', 'פ', 'צ'];
const HUNDREDS = ['', 'ק', 'ר', 'ש', 'ת'];
export function gematriya(n) {
  n %= 1000;
  let s = '';
  let h = Math.floor(n / 100);
  while (h > 4) { s += 'ת'; h -= 4; }
  s += HUNDREDS[h];
  let r = n % 100;
  if (r === 15) s += 'טו';
  else if (r === 16) s += 'טז';
  else s += TENS[Math.floor(r / 10)] + ONES[r % 10];
  if (s.length === 1) return s + '׳';
  const FINAL = { 'כ': 'ך', 'מ': 'ם', 'נ': 'ן', 'פ': 'ף', 'צ': 'ץ' };
  const last = FINAL[s.slice(-1)] || s.slice(-1);
  return s.slice(0, -1) + '״' + last;
}
