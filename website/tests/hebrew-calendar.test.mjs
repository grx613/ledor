// Сверка JS-конвертера сайта с эталонными векторами spec/golden (те же, что у приложения).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import * as H from '../assets/js/hebrew-calendar.js';

const dir = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'spec', 'golden');
const rows = (f) => JSON.parse(readFileSync(join(dir, f), 'utf8')).rows;
const g = (iso) => { const [y, m, d] = iso.split('-').map(Number); return { y, m, d }; };
const iso = ({ y, m, d }) => `${String(y).padStart(4, '0')}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;

test('conversions.json: в обе стороны', () => {
  for (const [gi, hy, hm, hd] of rows('conversions.json')) {
    assert.deepEqual(H.gregorianToHebrew(g(gi)), { year: hy, month: hm, day: hd }, gi);
    assert.equal(iso(H.hebrewToGregorian({ year: hy, month: hm, day: hd })), gi);
  }
});

test('years.json', () => {
  for (const [y, leap, days, chesh, kis, tishrei] of rows('years.json')) {
    assert.equal(H.isLeapYear(y), leap, `${y}`);
    assert.equal(H.daysInYear(y), days, `${y}`);
    assert.equal(H.daysInMonth(y, 'CHESHVAN'), chesh);
    assert.equal(H.daysInMonth(y, 'KISLEV'), kis);
    assert.equal(iso(H.hebrewToGregorian({ year: y, month: 'TISHREI', day: 1 })), tishrei);
  }
});

test('anniversaries.json', () => {
  for (const r of rows('anniversaries.json')) {
    const [kind, oy, om, od, ty, rm, rd, gi] = r;
    const res = H.hebrewAnniversary(kind, { year: oy, month: om, day: od }, ty);
    assert.deepEqual([res.month, res.day], [rm, rd], JSON.stringify(r));
    assert.equal(iso(H.hebrewToGregorian(res)), gi, JSON.stringify(r));
  }
});

test('контрольные примеры из ТЗ', () => {
  assert.deepEqual(H.gregorianToHebrew({ y: 1990, m: 4, d: 23 }), { year: 5750, month: 'NISAN', day: 28 });
  assert.deepEqual(H.gregorianToHebrew({ y: 1990, m: 4, d: 23 }, true), { year: 5750, month: 'NISAN', day: 29 });
  const m = H.mitzvahDates(H.gregorianToHebrew({ y: 2015, m: 6, d: 2 }));
  assert.equal(iso(m.bar.gregorian), '2028-06-09');
  assert.equal(iso(m.bat.gregorian), '2027-06-20');
  const n = H.nextOccurrence('BIRTHDAY', { year: 5750, month: 'NISAN', day: 28 }, { y: 2026, m: 11, d: 24 });
  assert.equal(iso(n.gregorian), '2027-05-05');
  assert.equal(n.years, 37);
  assert.equal(H.gematriya(5750), 'תש״ן');
  assert.equal(H.gematriya(28), 'כ״ח');
  assert.equal(H.gematriya(15), 'ט״ו');
  assert.equal(H.gematriya(1), 'א׳');
  assert.equal(H.weekday({ y: 2026, m: 11, d: 24 }), 2);
});
