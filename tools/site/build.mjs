// Генератор сайта LeDor: один шаблон + словари tools/site/i18n/*.mjs → website/<lang>/…
// Запуск: node tools/site/build.mjs   (без зависимостей; результат коммитится и выкладывается tools/deploy/publish.sh)
import { writeFileSync, mkdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = join(ROOT, 'website');
const SITE = 'https://kalendar.bothub.chat';
const LANGS = ['en', 'he', 'ru', 'yi', 'fr', 'es'];     // порядок — как в приложении
const DEFAULT = 'en';
const APK = readFileSync(join(ROOT, 'tools', 'site', 'apk-name.txt'), 'utf8').trim();   // ledor-X.Y.Z.apk
const ASSET_V = '5';
const CHANNEL = 'https://t.me/bothub_founder';
const SOURCE = 'https://github.com/grx613/ledor';

const T = {};
for (const l of LANGS) T[l] = (await import(`./i18n/${l}.mjs`)).default;

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const strip = (s) => String(s).replace(/<[^>]+>/g, '');
const write = (rel, html) => { const f = join(OUT, rel); mkdirSync(dirname(f), { recursive: true }); writeFileSync(f, html); };
const url = (l, page) => `${SITE}/${l}/${page ? page + '/' : ''}`;

const ICONS = {
  candle: '<rect x="9" y="10" width="6" height="11" rx="1.5"/><path d="M12 2.5c2 2.2 2 4.5 0 5.5c-2-1-2-3.3 0-5.5z"/>',
  star: '<path d="M12 2l8.66 15H3.34z M12 22L3.34 7h17.32z"/>',
  scroll: '<path d="M5 3v18M19 3v18"/><rect x="7" y="5" width="10" height="14"/><path d="M9.5 9h5M9.5 12h5M9.5 15h3"/>',
  bar: '<circle cx="12" cy="8" r="4"/><path d="M5 21c0-3.9 3.1-7 7-7s7 3.1 7 7"/>',
  battery: '<rect x="3" y="7" width="16" height="10" rx="2"/><path d="M21 10.5v3M6 10v4M9 10v4"/>',
  heart: '<path d="M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.3 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z"/>',
  list: '<path d="M8 6h12M8 12h12M8 18h12"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/>',
  send: '<path d="M21 3L3 10.5l7 2.5 2.5 7z M10 13l11-10"/>',
  people: '<circle cx="8" cy="8" r="3"/><circle cx="16.5" cy="9" r="2.5"/><path d="M2.5 20c0-3.3 2.5-6 5.5-6s5.5 2.7 5.5 6M13.5 20c0-2.8 1.3-5 3-5s4 2.2 4 5"/>',
};
const icon = (k) => `<div class="icon" aria-hidden="true"><svg viewBox="0 0 24 24">${ICONS[k]}</svg></div>`;

function head(l, page, title, description, extra = '') {
  const t = T[l];
  const alts = LANGS.map((x) => `<link rel="alternate" hreflang="${x}" href="${url(x, page)}">`).join('\n');
  return `<!doctype html>
<html lang="${l}" dir="${t.dir}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}</title>
<meta name="description" content="${esc(description)}">
<link rel="canonical" href="${url(l, page)}">
${alts}
<link rel="alternate" hreflang="x-default" href="${SITE}/${page ? page + '/' : ''}">
<meta name="theme-color" content="#0B3FAE">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<meta property="og:type" content="website">
<meta property="og:site_name" content="LeDor Kalendář">
<meta property="og:title" content="${esc(page ? title : t.meta.ogTitle)}">
<meta property="og:description" content="${esc(page ? description : t.meta.ogDesc)}">
<meta property="og:url" content="${url(l, page)}">
<meta property="og:image" content="${SITE}/assets/img/og/${l}.png">
<meta property="og:image:width" content="1200"><meta property="og:image:height" content="630">
<meta property="og:locale" content="${t.ogLocale}">
${LANGS.filter((x) => x !== l).map((x) => `<meta property="og:locale:alternate" content="${T[x].ogLocale}">`).join('\n')}
<meta name="twitter:card" content="summary_large_image">
<link rel="stylesheet" href="/assets/css/styles.css?v=${ASSET_V}">
<script>try{localStorage.setItem('ledor-lang','${l}')}catch(e){}</script>
${extra}
</head>`;
}

function header(l, page) {
  const t = T[l]; const home = `/${l}/`;
  const langs = LANGS.map((x) => `<a href="/${x}/${page ? page + '/' : ''}" lang="${x}" hreflang="${x}"${x === l ? ' aria-current="true"' : ''}>${T[x].name}</a>`).join('');
  return `<header class="top">
  <div class="wrap">
    <a class="brand" href="${home}"><img src="/assets/img/mark.svg" alt="" width="34" height="34"><span>LeDor</span><span class="brand-sub">Kalendář</span></a>
    <nav>
      <a href="${home}#share">${t.nav.share}</a>
      <a href="${home}#features">${t.nav.features}</a>
      <a href="/${l}/converter/">${t.nav.converter}</a>
      <a href="${home}#faq">${t.nav.faq}</a>
      <details class="langs"><summary aria-label="${esc(t.nav.lang)}"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.7 2.5 15.3 0 18M12 3c-2.5 2.7-2.5 15.3 0 18"/></svg>${l.toUpperCase()}</summary><div class="langs-menu">${langs}</div></details>
      <a class="btn btn-primary" href="/${APK}" download>${t.nav.download}</a>
    </nav>
  </div>
</header>`;
}

function footer(l) {
  const t = T[l];
  return `<footer>
  <div class="wrap">
    <span class="wordmark">Kalendář</span>
    <nav><a href="/${l}/converter/">${t.footer.converter}</a><a href="/${l}/privacy/">${t.footer.privacy}</a><a href="/${l}/privacy/#delete">${t.footer.delete}</a><a href="${SOURCE}" rel="noopener">${t.footer.source}</a><a href="${CHANNEL}" rel="noopener">Telegram</a></nav>
    <div>© 2026 LeDor · ${t.footer.copy}</div>
  </div>
</footer>`;
}

const faqLd = (items) => ({ '@type': 'FAQPage', mainEntity: items.map((i) => ({ '@type': 'Question', name: strip(i.q), acceptedAnswer: { '@type': 'Answer', text: strip(i.a) } })) });
const ld = (obj) => `<script type="application/ld+json">${JSON.stringify({ '@context': 'https://schema.org', '@graph': obj })}</script>`;
const faqHtml = (items) => items.map((i) => `<details><summary>${i.q}</summary><p>${i.a}</p></details>`).join('\n    ');

const SHOTS = ['03_calendar', '05_events', '14_list_share', '06_detail', '08_editor', '07_converter'];

function indexPage(l) {
  const t = T[l];
  const app = { '@type': 'MobileApplication', name: 'LeDor', alternateName: ['LeDor VaDor', 'LeDor Kalendář'], applicationCategory: 'LifestyleApplication', operatingSystem: 'Android', inLanguage: LANGS, offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' }, description: t.meta.description, downloadUrl: `${SITE}/${APK}`, author: { '@type': 'Person', name: t.author.name, url: CHANNEL }, license: `${SOURCE}/blob/main/LICENSE` };
  const site = { '@type': 'WebSite', name: 'LeDor Kalendář', url: url(l), inLanguage: l };
  return `${head(l, '', t.meta.title, t.meta.description, ld([app, site, faqLd(t.faq.items)]))}
<body>
${header(l, '')}
<main>
<div class="hero">
  <div class="wrap">
    <div class="logo-block">
      <img src="/assets/img/mark.svg" alt="" width="96" height="96">
      <div class="logo-name">LeDor</div>
      <div class="slogan"><span class="lat">LeDor VaDor</span> · <span class="he" lang="he" dir="rtl">לדור ודור</span></div>
      <div class="hero-wordmark">Kalendář</div>
    </div>
    <div>
      <h1>${t.hero.h1}</h1>
      <p class="lead">${t.hero.lead}</p>
    </div>
    <div class="cta" id="download">
      <a class="btn btn-primary" href="/${APK}" download>${t.hero.download}</a>
      <a class="btn btn-ghost" href="#share">${t.hero.how}</a>
    </div>
    <small>${t.hero.note}</small>
  </div>
</div>

<section id="share" aria-labelledby="share-title">
  <div class="wrap">
    <h2 id="share-title">${t.share.h2}</h2>
    <p class="sub">${t.share.sub}</p>
    <div class="share-grid">
      <div>
        <ol class="steps">
          ${t.share.steps.map((s, i) => `<li><span class="num">${i + 1}</span><div><h3>${s.t}</h3><p>${s.p}</p></div></li>`).join('\n          ')}
        </ol>
        <ul class="points">
          ${t.share.points.map((p) => `<li>${p}</li>`).join('\n          ')}
        </ul>
        <p class="note">${t.share.sync}</p>
      </div>
      <div class="share-shots">
        <img src="/assets/img/screens/${l}/10_lists.png" alt="${esc(t.share.altLists)}" width="1200" height="2580" loading="lazy">
        <img src="/assets/img/screens/${l}/14_list_share.png" alt="${esc(t.share.altShare)}" width="1200" height="2580" loading="lazy">
      </div>
    </div>
  </div>
</section>

<section id="features" class="alt">
  <div class="wrap">
    <h2>${t.features.h2}</h2>
    <div class="features">
      ${t.features.items.map((f) => `<div class="card">${icon(f.icon)}<h3>${f.t}</h3><p>${f.p}</p></div>`).join('\n      ')}
    </div>
  </div>
</section>

<section aria-labelledby="teaser-title">
  <div class="wrap">
    <a class="card teaser" href="/${l}/converter/">
      ${icon('star')}
      <div><h2 id="teaser-title">${t.teaser.h2}</h2><p>${t.teaser.p}</p><span class="more">${t.teaser.link}</span></div>
    </a>
  </div>
</section>

<section id="screens" aria-labelledby="shots-title" class="alt">
  <div class="wrap">
    <h2 id="shots-title">${t.shots.h2}</h2>
    <div class="shots-nav"><button type="button" data-scroll="-1" aria-label="${esc(t.shots.prev)}">‹</button><button type="button" data-scroll="1" aria-label="${esc(t.shots.next)}">›</button></div>
    <div class="shots" id="shots" tabindex="0">
      ${SHOTS.map((s) => `<img src="/assets/img/screens/${l}/${s}.png" alt="${esc(t.shots.alt[s])}" width="1200" height="2580" loading="lazy">`).join('\n      ')}
    </div>
  </div>
</section>

<section id="faq">
  <div class="wrap">
    <h2>${t.faq.h2}</h2>
    ${faqHtml(t.faq.items)}
  </div>
</section>

<section aria-labelledby="author-title" class="alt">
  <div class="wrap">
    <div class="card author">
      <img class="author-torah" src="/assets/img/author-torah.jpg" alt="${esc(t.author.torahAlt)}" width="720" height="720" loading="lazy">
      <div>
        <div class="author-head"><img src="/assets/img/author.jpg" alt="" width="72" height="72" loading="lazy"><div><p class="kicker">${t.author.h2}</p><h2 id="author-title">${t.author.name}</h2></div></div>
        <p>${t.author.p}</p>
        <div class="links"><a href="${CHANNEL}" rel="noopener">${t.author.channel}</a><a href="${SOURCE}" rel="noopener">GitHub</a></div>
      </div>
    </div>
  </div>
</section>

<section aria-labelledby="eco-title">
  <div class="wrap">
    <h2 id="eco-title">${t.eco.h2}</h2>
    <p class="sub">${t.eco.sub}</p>
    <div class="eco">
      <div class="card"><a href="https://bothub.chat" rel="noopener"><img src="/assets/img/eco/bothub.png" alt="" width="52" height="52" loading="lazy"><div><b>BotHub</b><span>${t.eco.bothub}</span></div></a></div>
      <div class="card"><a href="https://fire.bothub.chat" rel="noopener"><img src="/assets/img/eco/fire.svg" alt="" width="52" height="52" loading="lazy"><div><b>BotHub FiRe</b><span>${t.eco.fire}</span></div></a></div>
    </div>
  </div>
</section>
</main>
${footer(l)}
<script src="/assets/js/site.js?v=${ASSET_V}" defer></script>
</body>
</html>
`;
}

function converterPage(l) {
  const t = T[l]; const c = t.conv;
  const tool = { '@type': 'WebApplication', name: c.h1, url: url(l, 'converter'), applicationCategory: 'UtilitiesApplication', operatingSystem: 'Any', offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' }, inLanguage: l };
  const i18n = `<script type="application/json" id="i18n">${JSON.stringify({ lang: l, letters: t.letters, ...t.js }).replace(/</g, '\\u003c')}</script>`;
  return `${head(l, 'converter', c.title, c.description, ld([tool, faqLd(c.faq)]))}
<body>
${header(l, 'converter')}
<main>
<div class="hero hero-small">
  <div class="wrap">
    <h1>${c.h1}</h1>
    <p class="lead">${c.lead}</p>
  </div>
</div>
<section id="converter" class="converter" aria-labelledby="conv-title">
  <div class="wrap">
    <div class="card">
      <h2 id="conv-title" class="visually-hidden">${c.h1}</h2>
      <div class="seg" role="group" aria-label="${esc(c.dirLabel)}">
        <button type="button" data-mode="g2h" aria-pressed="true">${c.g2h}</button>
        <button type="button" data-mode="h2g" aria-pressed="false">${c.h2g}</button>
      </div>
      <div class="fields" id="g-fields">
        <label>${c.day}<select id="g-day"></select></label>
        <label>${c.month}<select id="g-month"></select></label>
        <label>${c.year}<input id="g-year" type="number" inputmode="numeric" min="1600" max="2200"></label>
      </div>
      <div class="fields" id="h-fields" hidden>
        <label>${c.day}<select id="h-day"></select></label>
        <label>${c.month}<select id="h-month"></select></label>
        <label>${c.year}<input id="h-year" type="number" inputmode="numeric" min="5360" max="5960"></label>
      </div>
      <div class="seg" id="sunset" role="group" aria-label="${esc(c.timeLabel)}">
        <button type="button" data-after="0" aria-pressed="true">${c.before}</button>
        <button type="button" data-after="1" aria-pressed="false">${c.after}</button>
      </div>
      <div id="result" class="result" aria-live="polite"></div>
      <div class="extras" id="extras" hidden>
        <div class="card" id="birth"></div>
        <div class="card" id="yahrzeit"></div>
      </div>
      <p class="cta-inline"><span id="cta"></span> <a href="/${APK}" download>${c.app}</a></p>
      <noscript><p>${c.noscript}</p></noscript>
    </div>
  </div>
</section>
<section>
  <div class="wrap prose">
    ${c.prose}
  </div>
</section>
<section>
  <div class="wrap">
    <h2>${t.faq.h2}</h2>
    ${faqHtml(c.faq)}
  </div>
</section>
</main>
${footer(l)}
${i18n}
<script type="module" src="/assets/js/converter.js?v=${ASSET_V}"></script>
</body>
</html>
`;
}

function privacyPage(l) {
  const t = T[l]; const p = t.privacy;
  return `${head(l, 'privacy', p.title, p.description)}
<body>
${header(l, 'privacy')}
<main class="doc">
<h1>${p.h1}</h1>
<p><em>${p.updated}</em></p>
${p.body}
</main>
${footer(l)}
</body>
</html>
`;
}

// Корень и старые адреса (/privacy/, /converter/): выбор языка по браузеру, иначе английский.
function redirectPage(page) {
  const links = LANGS.map((x) => `<li><a href="/${x}/${page ? page + '/' : ''}" hreflang="${x}" lang="${x}">${T[x].name}</a></li>`).join('');
  const alts = LANGS.map((x) => `<link rel="alternate" hreflang="${x}" href="${url(x, page)}">`).join('\n');
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(T.en.meta.title)}</title>
<meta name="description" content="${esc(T.en.meta.description)}">
${alts}
<link rel="alternate" hreflang="x-default" href="${SITE}/${page ? page + '/' : ''}">
<link rel="canonical" href="${url(DEFAULT, page)}">
<meta property="og:title" content="${esc(T.en.meta.ogTitle)}">
<meta property="og:description" content="${esc(T.en.meta.ogDesc)}">
<meta property="og:image" content="${SITE}/assets/img/og/en.png">
<meta name="twitter:card" content="summary_large_image">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="stylesheet" href="/assets/css/styles.css?v=${ASSET_V}">
<script>
(function () {
  var langs = ${JSON.stringify(LANGS)}, alias = { iw: 'he', ji: 'yi', uk: 'ru', be: 'ru', kk: 'ru' }, pick = null;
  try { pick = localStorage.getItem('ledor-lang'); } catch (e) {}
  if (langs.indexOf(pick) < 0) {
    pick = null;
    var list = navigator.languages || [navigator.language || ''];
    for (var i = 0; i < list.length && !pick; i++) {
      var c = String(list[i]).toLowerCase().split('-')[0]; c = alias[c] || c;
      if (langs.indexOf(c) >= 0) pick = c;
    }
  }
  location.replace('/' + (pick || '${DEFAULT}') + '/${page ? page + '/' : ''}' + location.hash);
})();
</script>
</head>
<body>
<main class="doc choose"><img src="/assets/img/mark.svg" alt="" width="72" height="72"><h1>LeDor Kalendář</h1><ul>${links}</ul></main>
</body>
</html>
`;
}

function sitemap() {
  const pages = ['', 'converter', 'privacy'];
  const rows = pages.flatMap((p) => LANGS.map((l) => `  <url><loc>${url(l, p)}</loc>
${LANGS.map((x) => `    <xhtml:link rel="alternate" hreflang="${x}" href="${url(x, p)}"/>`).join('\n')}
    <xhtml:link rel="alternate" hreflang="x-default" href="${SITE}/${p ? p + '/' : ''}"/>
  </url>`));
  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${rows.join('\n')}
</urlset>
`;
}

for (const l of LANGS) {
  write(`${l}/index.html`, indexPage(l));
  write(`${l}/converter/index.html`, converterPage(l));
  write(`${l}/privacy/index.html`, privacyPage(l));
}
write('index.html', redirectPage(''));
write('converter/index.html', redirectPage('converter'));
write('privacy/index.html', redirectPage('privacy'));
write('sitemap.xml', sitemap());
console.log(`Сайт собран: ${LANGS.length} языков × 3 страницы, APK ${APK}`);
