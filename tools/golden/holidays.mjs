// Эталонные векторы праздников и времени зажигания свечей.
// Источник истины — открытый API Hebcal (https://www.hebcal.com/home/195/jewish-calendar-rest-api),
// независимая от приложения реализация. Нужен интернет; зависимостей нет.
//
// Запуск:  node tools/golden/holidays.mjs
// Результат: spec/golden/holidays.json, spec/golden/candles.json (коммитятся, руками не править).

import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = join(ROOT, 'spec', 'golden');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function hebcal(params) {
  const url = 'https://www.hebcal.com/hebcal?v=1&cfg=json&lg=s&' + new URLSearchParams(params);
  for (let attempt = 1; ; attempt++) {
    const res = await fetch(url);
    if (res.ok) { await sleep(250); return (await res.json()).items; }
    if (attempt === 3) throw new Error(`${res.status} ${url}`);
    await sleep(2000);
  }
}

const save = (name, obj) => {
  const rows = obj.rows.map((r) => '  ' + JSON.stringify(r)).join(',\n');
  const head = JSON.stringify({ ...obj, rows: undefined }, null, 2).replace(/\n}$/, '');
  writeFileSync(join(OUT, name), `${head},\n  "rows": [\n${rows}\n  ]\n}\n`);
  console.log(`${name}: ${obj.rows.length} rows`);
};

const days = (a, b) => Math.round((Date.parse(b) - Date.parse(a)) / 86400000) + 1;
const norm = (s) => s.replace(/[’']/g, "'").replace(/ \(observed\)$/, '');   // «(observed)» — перенесённый день

// ---------- 1. Праздники: первый день и число дней ----------
// Ключ = имя в enum Holiday. [начало, конец] — названия событий Hebcal; конец необязателен.
const MAP = {
  ROSH_HASHANA: [/^Rosh Hashana \d+$/, /^Rosh Hashana II$/],
  TZOM_GEDALIAH: [/^Tzom Gedaliah$/],
  YOM_KIPPUR: [/^Yom Kippur$/],
  SUKKOT: [/^Sukkot I$/, /^Sukkot VII/],
  SHMINI_ATZERET: [/^Shmini Atzeret$/, /^Simchat Torah$/],
  CHANUKAH: [/^Chanukah: 2 Candles$/, /^Chanukah: 8th Day$/],   // «1 Candle» — вечер 24 Кислева
  ASARA_BETEVET: [/^Asara B'Tevet$/],
  TU_BISHVAT: [/^Tu BiShvat$/],
  TAANIT_ESTHER: [/^Ta'anit Esther$/],
  PURIM: [/^Purim$/],
  SHUSHAN_PURIM: [/^Shushan Purim$/],
  PESACH: [/^Pesach I$/, /^Pesach VIII$|^Pesach VII$/],
  PESACH_VII: [/^Pesach VII$/, /^Pesach VIII$/],
  YOM_HASHOAH: [/^Yom HaShoah$/],
  YOM_HAZIKARON: [/^Yom HaZikaron$/],
  YOM_HAATZMAUT: [/^Yom HaAtzma'ut$/],
  LAG_BAOMER: [/^Lag BaOmer$/],
  YOM_YERUSHALAYIM: [/^Yom Yerushalayim$/],
  SHAVUOT: [/^Shavuot$|^Shavuot I$/, /^Shavuot II$/],
  TZOM_TAMMUZ: [/^Tzom Tammuz$/],
  TISHA_BAV: [/^Tish'a B'Av$/],
  TU_BAV: [/^Tu B'Av$/],
};

const rows = [];
for (let year = 5785; year <= 5800; year++) {
  for (const il of [false, true]) {
    const items = (await hebcal({ year, yt: 'H', maj: 'on', min: 'on', mod: 'on', mf: 'on', nx: 'off', ss: 'off', s: 'off', c: 'off', i: il ? 'on' : 'off' }))
      .map((i) => ({ title: norm(i.title), date: i.date.slice(0, 10) }));
    for (const [holiday, [startRe, endRe]] of Object.entries(MAP)) {
      const start = items.find((i) => startRe.test(i.title));
      if (!start) throw new Error(`${year} il=${il}: нет ${holiday}`);
      const ends = endRe ? items.filter((i) => endRe.test(i.title)).map((i) => i.date).sort() : [];
      const end = ends.length ? ends[ends.length - 1] : start.date;
      rows.push({ year, il, holiday, start: start.date, days: days(start.date, end) });
    }
  }
}
save('holidays.json', {
  description: 'Праздники по еврейским годам: первый день (гражданская дата; праздник начинается накануне вечером) и число дней. il — Израиль.',
  source: 'Hebcal REST API',
  rows,
});

// ---------- 2. Зажигание свечей ----------
// first=true — канун Шабата или первого дня праздника (свечи до захода); в субботу вечером свечи зажигают после выхода звёзд.
const cities = JSON.parse(readFileSync(join(ROOT, 'android/domain/src/main/resources/cities.json'), 'utf8'));
const CITY_IDS = ['il-jerusalem', 'il-haifa', 'il-tel-aviv', 'us-new-york', 'ru-moscow', 'fr-paris', 'ar-buenos-aires'];
const EVE = /^Erev (Rosh Hashana|Yom Kippur|Sukkot|Pesach|Shavuot)$|^Sukkot VII|^Pesach VI /;
const candleRows = [];
for (const id of CITY_IDS) {
  const c = cities.find((x) => x.id === id);
  if (!c) throw new Error(`нет города ${id}`);
  const b = id === 'il-jerusalem' ? 40 : id === 'il-haifa' ? 30 : 18;
  const items = await hebcal({ year: 2027, maj: 'on', min: 'off', mod: 'off', mf: 'off', nx: 'off', ss: 'off', s: 'off', c: 'on', M: 'on', b,
    geo: 'pos', latitude: c.lat, longitude: c.lon, tzid: c.zoneId, i: c.countryCode === 'IL' ? 'on' : 'off' });
  const eves = new Set(items.filter((i) => EVE.test(norm(i.title))).map((i) => i.date.slice(0, 10)));
  for (const i of items.filter((x) => x.category === 'candles')) {
    const date = i.date.slice(0, 10);
    const dow = new Date(date + 'T00:00:00Z').getUTCDay();
    if (dow !== 5 && dow !== 6 && !eves.has(date)) continue;   // вторые вечера праздника приложение не считает
    candleRows.push({ city: id, eve: date, time: i.date.slice(11, 16), minutes: b });
  }
}
save('candles.json', {
  description: 'Зажигание свечей накануне Шабата и первых дней праздников, 2027 год: местное время. minutes — за сколько минут до захода (в субботу вечером — после выхода звёзд, 8,5°).',
  source: 'Hebcal REST API, координаты из cities.json',
  rows: candleRows,
});
