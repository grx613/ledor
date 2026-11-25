// Материалы для страницы LeDor в Google Play из tools/store/i18n/<lang>.mjs.
//   node tools/store/build.mjs <каталог>
// Пишет в <каталог>: <lang>/listing.txt (тексты для копирования в консоль), HTML-заготовки графики
// и render.sh, который снимает их headless Chromium в PNG: icon-512.png, <lang>/feature-1024x500.png,
// <lang>/shot-N.png (1080×1920). Снимки экранов берутся из android/app/build/screens/<lang>/
// (сначала ./gradlew :app:recordRoborazziDebug).
import { writeFileSync, mkdirSync, readFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const out = process.argv[2];
if (!out) { console.error('Укажите каталог для результата.'); process.exit(2); }

const LANGS = ['en', 'ru', 'he', 'yi', 'fr', 'es'];
const SCREENS = { en: 'en', ru: 'ru', he: 'he', yi: 'yi', fr: 'fr', es: 'es' };
const LIMITS = { title: 30, short: 80, full: 4000 };
const font = join(root, 'website/assets/fonts/rubik.ttf');
const mark = readFileSync(join(root, 'website/assets/img/mark.svg'), 'utf8');
const markSquare = mark.replace('rx="16"', 'rx="0"');   // Google сам скругляет значок — нужен полный квадрат

const css = `@font-face{font-family:Rubik;src:url("file://${font}");font-weight:300 900}
html,body{margin:0;overflow:hidden}body{font-family:Rubik,sans-serif;color:#fff;box-sizing:border-box;
background:radial-gradient(900px 600px at 85% 0%,rgba(74,128,255,.55),transparent 68%),linear-gradient(175deg,#0B3FAE,#061F5C)}`;

const jobs = [];
const page = (file, w, h, body, extra = '', lang = 'en', dir = 'ltr') => {
  writeFileSync(join(out, file), `<!doctype html><html lang="${lang}" dir="${dir}"><head><meta charset="utf-8"><style>${css}
body{width:${w}px;height:${h}px}${extra}</style></head><body>${body}</body></html>`);
  jobs.push({ file, w, h, png: file.replace(/\.html$/, '.png') });
};

mkdirSync(out, { recursive: true });
writeFileSync(join(out, 'icon-512.svg'), markSquare);
page('icon-512.html', 512, 512, `<img src="icon-512.svg" style="width:512px;height:512px;display:block">`);

let problems = 0;
for (const lang of LANGS) {
  const t = (await import(`./i18n/${lang}.mjs`)).default;
  mkdirSync(join(out, lang), { recursive: true });
  for (const [k, max] of Object.entries(LIMITS)) {
    const n = [...t[k]].length;
    if (n > max) { console.error(`${lang}: ${k} — ${n} знаков, можно ${max}`); problems++; }
  }
  writeFileSync(join(out, lang, 'listing.txt'),
    `Язык в консоли: ${t.play}\n\n== Название (${[...t.title].length}/30) ==\n${t.title}\n\n` +
    `== Краткое описание (${[...t.short].length}/80) ==\n${t.short}\n\n== Полное описание (${[...t.full].length}/4000) ==\n${t.full}\n\n== Что нового (до 500) ==\n${t.whatsNew}\n`);

  const shots = join(root, 'android/app/build/screens', SCREENS[lang]);
  page(`${lang}/feature.html`, 1024, 500, `<div class="l"><div class="lock"><img class="mark" src="file://${join(root, 'website/assets/img/mark.svg')}">
<div><div class="name">LeDor</div><div class="slogan">LeDor VaDor · <span dir="rtl" lang="he">לדור ודור</span></div></div></div>
<div class="t">${t.feature}</div></div><img class="p" src="file://${shots}/03_calendar.png">`,
  `body{display:flex;align-items:center;gap:40px;padding:0 0 0 64px}.l{flex:1}.lock{display:flex;align-items:center;gap:18px}
.mark{width:84px;height:84px;border-radius:21px}.name{font-size:58px;font-weight:650;line-height:1}
.slogan{opacity:.75;font-size:19px;margin-top:8px;letter-spacing:1.3px}.t{font-size:40px;font-weight:700;line-height:1.15;margin-top:30px}
.p{width:270px;align-self:flex-start;margin:48px 64px 0 0;border-radius:34px;border:9px solid #0A1433;box-shadow:0 26px 60px rgba(3,14,48,.45)}`,
  lang, t.dir);

  Object.entries(t.shots).forEach(([shot, cap], i) => {
    const src = join(shots, `${shot}.png`);
    if (!existsSync(src)) { console.error(`нет снимка ${src}`); problems++; }
    page(`${lang}/shot-${i + 1}.html`, 1080, 1920, `<div class="c">${cap}</div><img class="p" src="file://${src}">`,
      `body{display:flex;flex-direction:column;align-items:center}
.c{height:330px;box-sizing:border-box;display:flex;align-items:center;justify-content:center;text-align:center;padding:40px 90px 0;
font-size:66px;font-weight:700;line-height:1.12}
.p{width:700px;border-radius:56px;border:14px solid #0A1433;box-shadow:0 40px 90px rgba(3,14,48,.5)}`, lang, t.dir);
  });
}

const chrome = process.env.CHROME || 'chromium';
writeFileSync(join(out, 'render.sh'), `#!/bin/sh\n# Снимает HTML-заготовки в PNG. Браузер: переменная CHROME (по умолчанию chromium).\nset -e\ncd "$(dirname "$0")"\n` +
  jobs.map(j => `"\${CHROME:-${chrome}}" --headless --no-sandbox --hide-scrollbars --allow-file-access-from-files --force-device-scale-factor=1 ` +
    `--window-size=${j.w},${j.h} --screenshot="$PWD/${j.png}" "file://$PWD/${j.file}" 2>/dev/null`).join('\n') + '\n');
console.log(`${jobs.length} картинок, ${LANGS.length} языков${problems ? `, проблем: ${problems}` : ''}`);
if (problems) process.exit(1);
