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
const ASSET_V = '9';
const CHANNEL = 'https://t.me/bothub_founder';
const SOURCE = 'https://github.com/grx613/ledor';
const LIST_COLORS = ['#D4A72C', '#1FA187', '#6A58B5'];   // как цвета списков в приложении на снимках

const T = {};
for (const l of LANGS) T[l] = (await import(`./i18n/${l}.mjs`)).default;

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const strip = (s) => String(s).replace(/<[^>]+>/g, '');
const write = (rel, html) => { const f = join(OUT, rel); mkdirSync(dirname(f), { recursive: true }); writeFileSync(f, html); };
const url = (l, page) => `${SITE}/${l}/${page ? page + '/' : ''}`;

const ICONS = {
  candle: '<rect x="9" y="10" width="6" height="11" rx="1.5"/><path d="M12 2.5c2 2.2 2 4.5 0 5.5c-2-1-2-3.3 0-5.5z"/>',
  bell: '<path d="M6 16.5V11a6 6 0 1 1 12 0v5.5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>',
  star: '<path d="M12 2l8.66 15H3.34z M12 22L3.34 7h17.32z"/>',
  scroll: '<path d="M5 3v18M19 3v18"/><rect x="7" y="5" width="10" height="14"/><path d="M9.5 9h5M9.5 12h5M9.5 15h3"/>',
  bar: '<circle cx="12" cy="8" r="4"/><path d="M5 21c0-3.9 3.1-7 7-7s7 3.1 7 7"/>',
  battery: '<rect x="3" y="7" width="16" height="10" rx="2"/><path d="M21 10.5v3M6 10v4M9 10v4"/>',
  heart: '<path d="M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.3 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z"/>',
  globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.7 2.5 15.3 0 18M12 3c-2.5 2.7-2.5 15.3 0 18"/>',
};
const icon = (k) => `<div class="icon" aria-hidden="true"><svg viewBox="0 0 24 24">${ICONS[k]}</svg></div>`;
const DOWNLOAD_ICON = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4v11M7 11l5 5 5-5M5 20h14"/></svg>';

// Магазины: пока приложения там нет — нейтральные неактивные плашки (не официальные значки магазинов).
const stores = (t) => `<span class="store" aria-disabled="true"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M6 3.5v17l14-8.5z"/></svg><span><small>${t.stores.soon}</small><b>Google Play</b></span></span>
      <span class="store" aria-disabled="true"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" d="M12 4.5L5.5 19.5M12 4.5l6.5 15M8 14.5h8"/></svg><span><small>${t.stores.soon}</small><b>App Store</b></span></span>`;
const ctaRow = (t, id = '') => `<div class="cta-row"${id ? ` id="${id}"` : ''}>
      <a class="btn btn-light" href="/${APK}" download>${DOWNLOAD_ICON}${t.stores.apk}</a>
      ${stores(t)}
    </div>`;
const phone = (l, name, alt, eager = false) => `<div class="phone"><img src="/assets/img/screens/${l}/${name}.png?v=${ASSET_V}" alt="${esc(alt)}" width="1200" height="2580"${eager ? ' fetchpriority="high"' : ' loading="lazy"'}></div>`;
const notif = (t, n) => `<div class="notif"><img src="/assets/img/mark.svg" alt="" width="38" height="38"><div><div class="notif-app">LeDor · ${t.hero.now}</div><b>${n.title}</b><p>${n.text}</p></div></div>`;

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
<meta property="og:image" content="${SITE}/assets/img/og/${l}.png?v=${ASSET_V}">
<meta property="og:image:width" content="1200"><meta property="og:image:height" content="630">
<meta property="og:locale" content="${t.ogLocale}">
${LANGS.filter((x) => x !== l).map((x) => `<meta property="og:locale:alternate" content="${T[x].ogLocale}">`).join('\n')}
<meta name="twitter:card" content="summary_large_image">
<link rel="stylesheet" href="/assets/css/styles.css?v=${ASSET_V}">
<script>try{localStorage.setItem('ledor-lang','${l}')}catch(e){}</script>
${extra}
</head>`;
}

// Шапка: только логотип, язык и «Скачать» — разделы страницы идут по порядку, ссылки на них — в подвале.
function header(l, page) {
  const t = T[l];
  const langs = LANGS.map((x) => `<a href="/${x}/${page ? page + '/' : ''}" lang="${x}" hreflang="${x}"${x === l ? ' aria-current="true"' : ''}>${T[x].name}</a>`).join('');
  return `<header class="top">
  <div class="wrap">
    <a class="brand" href="/${l}/"><img src="/assets/img/mark.svg" alt="" width="34" height="34"><span>LeDor</span></a>
    <div class="top-actions">
      <a class="top-link" href="/${l}/family/"${page === 'family' ? ' aria-current="page"' : ''}>${t.family.nav}</a>
      <details class="langs"><summary aria-label="${esc(t.nav.lang)}"><svg viewBox="0 0 24 24" aria-hidden="true">${ICONS.globe}</svg>${l.toUpperCase()}</summary><div class="langs-menu">${langs}</div></details>
      <a class="btn btn-primary" href="/${APK}" download>${t.nav.download}</a>
    </div>
  </div>
</header>`;
}

function footer(l) {
  const t = T[l];
  return `<footer>
  <div class="wrap">
    <span class="wordmark">Kalendář</span>
    <nav><a href="/${l}/converter/">${t.footer.converter}</a><a href="/${l}/family/">${t.family.footer}</a><a href="/${l}/#faq">${t.footer.faq}</a><a href="/${l}/privacy/">${t.footer.privacy}</a><a href="/${l}/privacy/#delete">${t.footer.delete}</a><a href="${SOURCE}" rel="noopener">GitHub</a><a href="${CHANNEL}" rel="noopener">Telegram</a></nav>
    <div>© 2026 LeDor · ${t.footer.copy}</div>
  </div>
</footer>`;
}

const faqLd = (items) => ({ '@type': 'FAQPage', mainEntity: items.map((i) => ({ '@type': 'Question', name: strip(i.q), acceptedAnswer: { '@type': 'Answer', text: strip(i.a) } })) });
const ld = (obj) => `<script type="application/ld+json">${JSON.stringify({ '@context': 'https://schema.org', '@graph': obj })}</script>`;
const faqHtml = (items) => items.map((i) => `<details><summary>${i.q}</summary><p>${i.a}</p></details>`).join('\n    ');

const SHOTS = ['03_calendar', '05_events', '10_lists', '14_list_share', '06_detail', '08_editor', '07_converter'];

function indexPage(l) {
  const t = T[l];
  const app = { '@type': 'MobileApplication', name: 'LeDor', alternateName: ['LeDor VaDor', 'LeDor Kalendář'], applicationCategory: 'LifestyleApplication', operatingSystem: 'Android', inLanguage: LANGS, offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' }, description: t.meta.description, downloadUrl: `${SITE}/${APK}`, author: { '@type': 'Person', name: t.author.name, url: CHANNEL }, license: `${SOURCE}/blob/main/LICENSE` };
  const site = { '@type': 'WebSite', name: 'LeDor Kalendář', url: url(l), inLanguage: l };
  const shotAlt = { ...t.shots.alt, '10_lists': t.share.altLists };
  return `${head(l, '', t.meta.title, t.meta.description, ld([app, site, faqLd(t.faq.items)]))}
<body>
${header(l, '')}
<main>
<div class="hero">
  <div class="wrap hero-grid">
    <div class="hero-copy">
      <div class="lockup">
        <img src="/assets/img/mark.svg" alt="" width="68" height="68">
        <div><div class="lockup-name">LeDor</div><div class="lockup-slogan">LeDor VaDor · <span lang="he" dir="rtl">לדור ודור</span></div></div>
      </div>
      <div class="hero-wordmark">Kalendář</div>
      <h1>${t.hero.h1}</h1>
      <p class="lead">${t.hero.lead}</p>
      <p class="hero-family"><a href="/${l}/family/">${t.family.heroLink}</a></p>
    ${ctaRow(t, 'download')}
      <p class="hero-note">${t.hero.note}<br>${t.stores.apkNote}</p>
    </div>
    <div class="hero-visual">
      <div class="float float-notif">${notif(t, t.remind.notifs.eve)}</div>
      ${phone(l, '03_calendar', t.hero.alt, true)}
      <a class="float float-lists chips-card" href="/${l}/family/"><small>${t.hero.listsCap}</small>${t.lists.map((n, i) => `<span class="chip"><i style="--c:${LIST_COLORS[i]}"></i>${n}</span>`).join('')}</a>
    </div>
  </div>
</div>

<section id="reminders" aria-labelledby="remind-title">
  <div class="wrap split">
    <div>
      <span class="kicker">${t.remind.kicker}</span>
      <h2 id="remind-title">${t.remind.h2}</h2>
      <p class="sub">${t.remind.sub}</p>
      <ul class="icon-list">
        ${t.remind.points.map((p) => `<li>${icon(p.icon)}<div><h3>${p.t}</h3><p>${p.p}</p></div></li>`).join('\n        ')}
      </ul>
    </div>
    <div class="lock">
      <div class="lock-cap">${t.remind.cap.eve}</div>
      ${notif(t, t.remind.notifs.eve)}
      <div class="lock-cap">${t.remind.cap.day}</div>
      ${notif(t, t.remind.notifs.day)}
      <div class="lock-cap">${t.remind.cap.yz}</div>
      ${notif(t, t.remind.notifs.yz)}
    </div>
  </div>
</section>

<section id="share" class="alt" aria-labelledby="share-title">
  <div class="wrap">
    <div class="center">
      <span class="kicker">${t.share.kicker}</span>
      <h2 id="share-title">${t.share.h2}</h2>
      <p class="sub">${t.share.sub}</p>
    </div>
    <div class="flow">
      ${phone(l, '10_lists', t.share.altLists)}
      <div class="bubble-wrap">
        <div class="bubble-cap">${t.share.msgCap}</div>
        <div class="bubble"><p>${t.share.msg[0]}</p><ul>${t.share.msg.slice(1).map((m) => `<li>${m}</li>`).join('')}</ul></div>
      </div>
      ${phone(l, '05_events', t.share.altEvents)}
    </div>
    <ol class="steps">
      ${t.share.steps.map((s, i) => `<li><div class="num">${i + 1}</div><h3>${s.t}</h3><p>${s.p}</p></li>`).join('\n      ')}
    </ol>
    <ul class="points">
      ${t.share.points.map((p) => `<li>${p}</li>`).join('\n      ')}
    </ul>
    <p class="fineprint">${t.share.sync}</p>
    <p class="center guide-link"><a href="/${l}/family/">${t.family.link}</a></p>
  </div>
</section>

<section id="features" aria-labelledby="features-title">
  <div class="wrap">
    <span class="kicker">${t.features.kicker}</span>
    <h2 id="features-title">${t.features.h2}</h2>
    <div class="features">
      ${t.features.items.map((f) => `<div class="card">${icon(f.icon)}<h3>${f.t}</h3><p>${f.p}</p></div>`).join('\n      ')}
    </div>
  </div>
</section>

<section id="screens" class="alt" aria-labelledby="shots-title">
  <div class="wrap">
    <div class="shots-head">
      <h2 id="shots-title">${t.shots.h2}</h2>
      <div class="shots-nav"><button type="button" data-scroll="-1" aria-label="${esc(t.shots.prev)}">‹</button><button type="button" data-scroll="1" aria-label="${esc(t.shots.next)}">›</button></div>
    </div>
    <div class="shots" id="shots" tabindex="0">
      ${SHOTS.map((s) => phone(l, s, shotAlt[s])).join('\n      ')}
    </div>
  </div>
</section>

<section id="faq">
  <div class="wrap faq">
    <h2>${t.faq.h2}</h2>
    ${faqHtml(t.faq.items)}
  </div>
</section>

<section aria-labelledby="cta-title" style="padding-top:0">
  <div class="wrap">
    <div class="cta-band">
      <span class="wordmark">Kalendář</span>
      <h2 id="cta-title">${t.cta.h2}</h2>
      <p>${t.cta.p}</p>
    ${ctaRow(t)}
    </div>
  </div>
</section>

<section class="alt" aria-labelledby="author-title">
  <div class="wrap about">
    <div class="card author">
      <img class="author-photo" src="/assets/img/author-lulav.jpg?v=${ASSET_V}" alt="${esc(t.author.photoAlt)}" width="720" height="720" loading="lazy">
      <div>
        <div class="author-head"><div><p class="kicker">${t.author.h2}</p><h2 id="author-title">${t.author.name}</h2></div></div>
        <p>${t.author.p}</p>
        <a href="${CHANNEL}" rel="noopener">${t.author.channel}</a>
      </div>
    </div>
    <div class="card projects">
      <h2>${t.eco.h2}</h2>
      <a href="https://bothub.chat" rel="noopener"><img src="/assets/img/eco/bothub.png" alt="" width="48" height="48" loading="lazy"><div><b>BotHub</b><span>${t.eco.bothub}</span></div></a>
      <a href="https://fire.bothub.chat" rel="noopener"><img src="/assets/img/eco/fire.svg" alt="" width="48" height="48" loading="lazy"><div><b>BotHub FiRe</b><span>${t.eco.fire}</span></div></a>
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
<section style="padding-top:0">
  <div class="wrap prose">
    ${c.prose}
  </div>
</section>
<section style="padding-top:0">
  <div class="wrap faq">
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

// «Один календарь на всю семью» (B-38): пошаговая инструкция про общие списки со снимками экранов.
function familyPage(l) {
  const t = T[l]; const f = t.family;
  const steps = f.steps.map((st, i) => `<li class="guide-step">
      <div class="guide-text"><div class="num">${i + 1}</div><h2>${st.t}</h2><p>${st.p}</p></div>
      ${phone(l, st.shot, strip(st.t), i === 0)}
    </li>`).join('\n    ');
  return `${head(l, 'family', f.title, f.description, ld([faqLd(f.faq)]))}
<body>
${header(l, 'family')}
<main>
<section class="guide-head">
  <div class="wrap">
    <h1>${f.h1}</h1>
    <p class="sub">${f.intro}</p>
  </div>
</section>
<section style="padding-top:0">
  <div class="wrap">
    <ol class="guide">
    ${steps}
    </ol>
    ${ctaRow(t)}
  </div>
</section>
<section class="alt">
  <div class="wrap faq">
    <h2>${t.faq.h2}</h2>
    ${faqHtml(f.faq)}
  </div>
</section>
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
<meta property="og:image" content="${SITE}/assets/img/og/en.png?v=${ASSET_V}">
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
  const pages = ['', 'converter', 'privacy', 'family'];
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
  write(`${l}/family/index.html`, familyPage(l));
}
write('index.html', redirectPage(''));
write('converter/index.html', redirectPage('converter'));
write('privacy/index.html', redirectPage('privacy'));
write('family/index.html', redirectPage('family'));
write('sitemap.xml', sitemap());
// Номер последней версии Android — его читает кнопка «Проверить обновление» в приложении (docs/01, ABT-5).
const APK_VERSION = /^ledor-(\d+(?:\.\d+){1,3})\.apk$/.exec(APK)?.[1];
if (!APK_VERSION) throw new Error(`tools/site/apk-name.txt: ожидается ledor-X.Y.Z.apk, а там «${APK}»`);
write('version.json', JSON.stringify({ android: { version: APK_VERSION, apk: APK } }) + '\n');
console.log(`Сайт собран: ${LANGS.length} языков × 4 страницы, APK ${APK}`);
