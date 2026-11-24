// Генератор эталонных тест-векторов для еврейского календаря.
// Источник истины — независимая реализация Hebcal (@hebcal/hdate), НЕ KosherJava:
// совпадение двух независимых реализаций и есть проверка корректности.
//
// Запуск:  cd tools/golden && npm ci && node generate.mjs
// Результат: spec/golden/*.json (коммитятся в репозиторий, руками не править).

import { writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import {
  abs2greg, abs2hebrew, greg2abs, hebrew2abs, daysInMonth, daysInYear,
  isLeapYear, getBirthdayHD, getYahrzeitHD,
} from '@hebcal/hdate';

const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'spec', 'golden');
mkdirSync(OUT, { recursive: true });

const NAMES = [null, 'NISAN', 'IYAR', 'SIVAN', 'TAMMUZ', 'AV', 'ELUL', 'TISHREI',
  'CHESHVAN', 'KISLEV', 'TEVET', 'SHEVAT'];

/** Hebcal/KosherJava month number (1..13, Nisan = 1) -> наш код месяца (14 значений). */
const code = (yy, mm) =>
  mm <= 11 ? NAMES[mm] : mm === 13 ? 'ADAR_II' : isLeapYear(yy) ? 'ADAR_I' : 'ADAR';

const iso = (d) =>
  `${String(d.getFullYear()).padStart(4, '0')}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const absToIso = (abs) => iso(abs2greg(abs));
const save = (name, obj) => {
  const rows = obj.rows.map((r) => '  ' + JSON.stringify(r)).join(',\n');
  const head = JSON.stringify({ ...obj, rows: undefined }, null, 2).replace(/\n}$/, '');
  writeFileSync(join(OUT, name), `${head},\n  "rows": [\n${rows}\n  ]\n}\n`);
  console.log(`${name}: ${obj.rows.length} rows`);
};

// ---------- 1. Конвертация григорианская <-> еврейская ----------
const convAbs = new Set();
let seed = 20261124;
const rnd = () => (seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648;
const absMin = greg2abs(new Date(1900, 0, 1));
const absMax = greg2abs(new Date(2100, 11, 31));
for (let i = 0; i < 500; i++) convAbs.add(absMin + Math.floor(rnd() * (absMax - absMin)));
for (let hy = 5775; hy <= 5800; hy++) {
  const last = isLeapYear(hy) ? 13 : 12;
  for (const mm of [7, 8, 9, 10, 11, 12, last, 1, 6]) {
    convAbs.add(hebrew2abs(hy, mm, 1));
    convAbs.add(hebrew2abs(hy, mm, daysInMonth(mm, hy)));
  }
}
for (let gy = 1996; gy <= 2032; gy++) {
  convAbs.add(greg2abs(new Date(gy, 1, 28)));
  convAbs.add(greg2abs(new Date(gy, 2, 1)) - 1); // 28 или 29 февраля
  convAbs.add(greg2abs(new Date(gy, 2, 1)));
  convAbs.add(greg2abs(new Date(gy, 11, 31)));
}
save('conversions.json', {
  description: 'Гражданская (дневная) дата <-> еврейская дата. Биекция: проверять в обе стороны.',
  source: '@hebcal/hdate 0.23.1',
  columns: ['gregorian ISO', 'hebrewYear', 'hebrewMonth code', 'hebrewDay', 'KosherJava month number (Nisan=1)'],
  rows: [...convAbs].sort((a, b) => a - b).map((abs) => {
    const h = abs2hebrew(abs);
    return [absToIso(abs), h.yy, code(h.yy, h.mm), h.dd, h.mm];
  }),
});

// ---------- 2. Свойства еврейских лет ----------
const years = [];
for (let hy = 5660; hy <= 5860; hy++) {
  years.push([hy, isLeapYear(hy), daysInYear(hy), daysInMonth(8, hy), daysInMonth(9, hy),
    absToIso(hebrew2abs(hy, 7, 1))]);
}
save('years.json', {
  description: 'Свойства еврейских лет 5660–5860.',
  source: '@hebcal/hdate 0.23.1',
  columns: ['hebrewYear', 'isLeap', 'daysInYear', 'daysInCheshvan', 'daysInKislev', '1 Tishrei (gregorian ISO)'],
  rows: years,
});

// ---------- 3. Ежегодные даты: день рождения / годовщина и йорцайт ----------
const annRows = [];
const stats = {};
for (let oy = 5779; oy <= 5785; oy++) {
  const leap = isLeapYear(oy);
  const origins = [[7, 1], [8, 29], [9, 29], [10, 1], [11, 29], [11, 30], [1, 1], [1, 28]];
  if (daysInMonth(8, oy) === 30) origins.push([8, 30]);
  if (daysInMonth(9, oy) === 30) origins.push([9, 30]);
  for (const dd of [1, 15, 29]) origins.push([12, dd]);
  if (leap) {
    origins.push([12, 30]);
    for (const dd of [1, 15, 29]) origins.push([13, dd]);
  }
  for (const [mm, dd] of origins) {
    for (let ty = oy + 1; ty <= oy + 19; ty++) {
      for (const kind of ['BIRTHDAY', 'YAHRZEIT']) {
        const fn = kind === 'BIRTHDAY' ? getBirthdayHD : getYahrzeitHD;
        const r = fn(ty, { yy: oy, mm, dd });
        const oc = code(oy, mm);
        const rc = code(r.yy, r.mm);
        annRows.push([kind, oy, oc, dd, ty, rc, r.dd, absToIso(hebrew2abs(r.yy, r.mm, r.dd))]);
        if (oc !== rc || dd !== r.dd) {
          const k = `${kind} ${oc} ${dd} -> ${rc} ${r.dd}`;
          stats[k] = (stats[k] || 0) + 1;
        }
      }
    }
  }
}
save('anniversaries.json', {
  description: 'Дата ежегодного события в целевом еврейском году. BIRTHDAY — правило дня рождения/годовщины '
    + '(также бар/бат-мицва: targetYear = originYear + 13 / + 12). YAHRZEIT — правило йорцайта, '
    + 'политика Адара = ADAR_I (ашкеназский обычай, как в Hebcal).',
  source: '@hebcal/hdate 0.23.1 (getBirthdayHD / getYahrzeitHD)',
  columns: ['kind', 'originYear', 'originMonth code', 'originDay', 'targetYear',
    'resultMonth code', 'resultDay', 'result gregorian ISO (дневная дата)'],
  rows: annRows,
});

console.log('\nСработавшие особые правила (origin -> result), число строк:');
for (const k of Object.keys(stats).sort()) console.log(`  ${k}: ${stats[k]}`);
