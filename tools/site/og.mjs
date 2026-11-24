// Картинки-превью для соцсетей (1200×630) на каждом языке: пишет HTML, снимок делает headless Chromium.
//   node tools/site/og.mjs <каталог для html>
//   for l in en he ru yi fr es; do chromium --headless --hide-scrollbars --allow-file-access-from-files \
//     --window-size=1200,630 --screenshot=website/assets/img/og/$l.png file://<каталог>/og_$l.html; done
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const w = join(root, 'website');
const out = process.argv[2];
if (!out) { console.error('Укажите каталог для HTML.'); process.exit(2); }

for (const lang of ['en', 'he', 'ru', 'yi', 'fr', 'es']) {
  const t = (await import(`./i18n/${lang}.mjs`)).default;
  writeFileSync(join(out, `og_${lang}.html`), `<!doctype html><html lang="${lang}" dir="${t.dir}"><head><meta charset="utf-8"><style>
@font-face{font-family:Rubik;src:url("file://${w}/assets/fonts/rubik.ttf");font-weight:300 900}
body{margin:0;width:1200px;height:630px;overflow:hidden;font-family:Rubik,sans-serif;color:#fff;display:flex;align-items:center;gap:56px;padding:0 72px;box-sizing:border-box;
background:radial-gradient(700px 420px at 85% 0%,rgba(74,128,255,.55),transparent 68%),linear-gradient(175deg,#0B3FAE,#061F5C)}
.l{flex:1}.lock{display:flex;align-items:center;gap:20px}.mark{width:92px;height:92px;border-radius:23px}
.name{font-size:64px;font-weight:650;line-height:1}.slogan{opacity:.75;font-size:21px;margin-top:10px;letter-spacing:1.5px}
.wm{color:#F2C75C;font-size:27px;letter-spacing:9px;margin-top:22px;font-weight:500}
.t{font-size:44px;font-weight:700;line-height:1.14;margin-top:22px;max-width:660px}
.p{width:300px;align-self:flex-start;margin-top:56px;border-radius:40px;border:10px solid #0A1433;box-shadow:0 30px 70px rgba(3,14,48,.45)}
</style></head><body><div class="l"><div class="lock"><img class="mark" src="file://${w}/assets/img/mark.svg"><div><div class="name">LeDor</div>
<div class="slogan">LeDor VaDor · <span dir="rtl" lang="he">לדור ודור</span></div></div></div><div class="wm">Kalendář</div><div class="t">${t.hero.h1}</div></div>
<img class="p" src="file://${w}/assets/img/screens/${lang}/03_calendar.png"></body></html>`);
}
