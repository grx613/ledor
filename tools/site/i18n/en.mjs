// English (default for the site).
export default {
  lang: 'en', dir: 'ltr', ogLocale: 'en_US', name: 'English', letters: false,

  meta: {
    title: 'LeDor Kalendář — Jewish birthday & yahrzeit calendar for the whole family',
    description: 'Free app for families and communities: Hebrew birthdays, yahrzeits and anniversaries by the Hebrew and Gregorian calendars, a shared list synced by PIN, and reminders the evening before, ahead of sunset.',
    ogTitle: 'LeDor — one list of family dates for the whole family',
    ogDesc: 'Hebrew birthdays, yahrzeits and anniversaries. A shared list by PIN, reminders before sunset. Free, no ads.',
  },
  nav: { share: 'Shared list', features: 'Features', converter: 'Converter', faq: 'FAQ', download: 'Download', lang: 'Language' },

  hero: {
    h1: 'One list of family dates — shared by the whole family',
    lead: 'Birthdays, yahrzeits and anniversaries by the Hebrew and Gregorian calendars. Create a shared list, send your family the PIN — and everyone gets the same dates and reminders on their phone.',
    download: 'Download for Android',
    how: 'How the shared list works',
    note: 'Free · no ads · no sign-up · coming soon to Google Play',
  },

  share: {
    h2: 'A shared list: the same dates for the whole family',
    sub: 'The heart of LeDor is a list kept by the whole family or community. No accounts, emails or passwords.',
    steps: [
      { t: 'Create a list', p: 'For example, “Cohen family”. The app creates two PINs: one for viewing and one for editing.' },
      { t: 'Send the PIN to your family', p: 'One tap in WhatsApp or Telegram. They install LeDor and enter the list name and PIN.' },
      { t: 'The dates appear for everyone', p: 'Add a grandchild’s birthday — grandma and uncle get it too, with reminders by their own settings.' },
    ],
    points: [
      '<b>No sign-up.</b> No email, phone number or password — just the list name and PIN.',
      '<b>Two access levels.</b> The regular PIN lets you view. The Master PIN lets you add and edit.',
      '<b>Works offline.</b> Changes are sent once you are back online.',
      '<b>Several lists at once.</b> Family, community, friends — each with its own color and notifications.',
    ],
    sync: 'Changes arrive when someone opens the app or pulls the list down, and once a day in the background.',
    altLists: 'Lists screen: personal and shared family list',
    altShare: 'Shared list: PINs and Share buttons for Telegram and WhatsApp',
  },

  features: {
    h2: 'Everything you need not to miss an important date',
    items: [
      { icon: 'candle', t: 'A reminder the evening before, ahead of sunset', p: 'The Hebrew day begins at nightfall. LeDor reminds you a chosen time before sunset in your city — and again on the day itself.' },
      { icon: 'star', t: 'Two calendars', p: 'Every date is shown by both the Hebrew and Gregorian calendars. You can even celebrate a birthday by both.' },
      { icon: 'scroll', t: 'Yahrzeit by the rules', p: 'Adar I and Adar II in a leap year, the 30th of Cheshvan and Kislev — all handled. Choose your Adar custom in settings.' },
      { icon: 'bar', t: 'Bar and bat mitzvah', p: 'Enter a date of birth — the app tells you when the bar mitzvah and bat mitzvah fall.' },
      { icon: 'battery', t: 'Easy on the battery', p: 'No background work and no location: the app wakes up only at the moment of a reminder.' },
      { icon: 'heart', t: 'Free and open', p: 'No ads, subscriptions or data collection. The source code is open on GitHub.' },
    ],
  },

  teaser: { h2: 'Find a Hebrew birthday', p: 'Online date converter that accounts for sunset: Hebrew birthday, bar mitzvah and yahrzeit dates for 10 years ahead.', link: 'Open the converter →' },

  shots: {
    h2: 'What the app looks like', prev: 'Back', next: 'Next',
    alt: {
      '03_calendar': 'Calendar by Hebrew months: both dates in every cell',
      '05_events': 'Events from all lists, grouped by Hebrew month',
      '14_list_share': 'Shared family list: PINs and sending to relatives',
      '06_detail': 'Event card: next date and age',
      '08_editor': 'New event: type, date in either calendar, observe by both calendars',
      '07_converter': 'Date converter that accounts for sunset',
    },
  },

  faq: {
    h2: 'Frequently asked questions',
    items: [
      { q: 'How do I share a list with my family?', a: 'Create a shared list under “Lists” and tap Telegram or WhatsApp — your family gets a message with the list name, PIN and a link to the app.' },
      { q: 'Do I need to sign up? Do you collect my data?', a: 'There is no sign-up. The app has no ads, analytics or trackers. The personal list stays on your phone; a shared list is stored on our server for its members only.' },
      { q: 'What is the difference between the PIN and the Master PIN?', a: 'The regular PIN lets people view dates and get reminders. The Master PIN lets them add, change and delete events. Share it only with people you trust.' },
      { q: 'Can I celebrate a birthday by both calendars?', a: 'Yes. Choose “By both calendars” — there will be two dates a year, and LeDor reminds you of each. The year of birth is needed to convert the date.' },
      { q: 'Does it account for sunset?', a: 'Yes. When entering a date you can mark “after sunset” — the Hebrew date is then the next one. The evening reminder comes a chosen time before sunset in your city.' },
      { q: 'How is a yahrzeit calculated in a leap year?', a: 'For someone who passed away in Adar of a regular year, Ashkenazi custom observes it in Adar I and Sephardi custom in Adar II; you choose the custom in settings. The rules for the 30th of Cheshvan and Kislev are applied too.' },
      { q: 'Does the app work offline?', a: 'Yes. The internet is needed only for shared lists; changes are sent once you are back online.' },
      { q: 'How much battery does the app use?', a: 'Practically none. There are no background services, no location and no permanent connection. The phone wakes LeDor for a split second only at the moment of a reminder — the evening before and on the day — and, if you use a shared list, once a day to sync, when the system finds it convenient.' },
      { q: 'Which languages does LeDor support?', a: 'English, עברית, Русский, ייִדיש, Français, Español. In Hebrew and Yiddish the interface is right-to-left and Hebrew dates are written in letters.' },
      { q: 'Can I move my events to a shared list?', a: 'Yes. When creating a list, check “Move events from the personal list”, or open the personal list and choose “Move events to a cloud list”.' },
      { q: 'Is there a dark theme?', a: 'Yes: light, dark or system — under “Settings → Appearance”.' },
      { q: 'Is there an iPhone version?', a: 'Coming soon. LeDor is currently available for Android.' },
      { q: 'How much does the app cost?', a: 'It is free, with no ads or paid content. The project is non-commercial and open source.' },
    ],
  },

  author: {
    h2: 'About the author', name: 'Gershon',
    p: 'Author of LeDor. Found a mistake in a date or have an idea? Write right from the app: “More → Send feedback”.',
    channel: 'Author’s Telegram channel', torahAlt: 'Gershon with a Torah scroll',
  },
  eco: {
    h2: 'Other BotHub projects', sub: 'LeDor is part of a small family of projects.',
    bothub: 'ChatGPT, Claude, Gemini, Grok and other AI models in one place.',
    fire: 'Top market news: stocks, rates, oil, crypto — short stories from dozens of outlets.',
  },
  footer: { privacy: 'Privacy policy', delete: 'Delete data', converter: 'Date converter', source: 'Source code', copy: 'non-commercial project · no ads or trackers' },

  conv: {
    title: 'Hebrew Date Converter — Hebrew birthday, bar mitzvah, yahrzeit | LeDor',
    description: 'Free Hebrew date converter: convert a date of birth to the Hebrew calendar accounting for sunset and back, find bar mitzvah, bat mitzvah and yahrzeit dates for 10 years ahead.',
    h1: 'Hebrew date converter',
    lead: 'Accounts for sunset, leap years and anniversary rules. The result comes with bar mitzvah, bat mitzvah and yahrzeit dates.',
    g2h: 'Gregorian → Hebrew', h2g: 'Hebrew → Gregorian', day: 'Day', month: 'Month', year: 'Year',
    before: 'Before sunset', after: 'After sunset', dirLabel: 'Conversion direction', timeLabel: 'Time of day',
    noscript: 'Please enable JavaScript to use the converter.', app: 'Download the app',
    prose: `<h2>How to convert a date of birth to the Hebrew calendar</h2>
<p>The Hebrew day begins not at midnight but at sunset. So for someone born in the evening, the Hebrew date is already the next one. If the time of birth is unknown, choose “before sunset” — that is the usual practice.</p>
<h3>Why a Hebrew birthday “moves”</h3>
<p>The Hebrew year is lunisolar: months follow the moon, and every few years a thirteenth month, Adar II, is added. That is why the same Hebrew date falls on a different Gregorian date every year.</p>
<h2>How to calculate a yahrzeit</h2>
<p>A yahrzeit is the anniversary of a loved one’s passing by the Hebrew calendar. The memorial candle is lit the evening before. If someone passed away in Adar of a regular year, Ashkenazi custom observes the yahrzeit in Adar I in a leap year. For the 30th of Cheshvan and Kislev the first-anniversary rule applies. The converter handles all of this.</p>
<h2>How to calculate a bar mitzvah and bat mitzvah date</h2>
<p>A bar mitzvah is at 13 and a bat mitzvah at 12, on the Hebrew birthday. For those born in Adar of a leap year, the date follows the last Adar.</p>`,
    faq: [
      { q: 'How do I convert a date of birth to the Hebrew calendar?', a: 'Enter the date and mark whether it was before or after sunset: a birth after sunset belongs to the next Hebrew date.' },
      { q: 'When is the bar mitzvah and bat mitzvah?', a: 'Bar mitzvah at 13, bat mitzvah at 12 by the Hebrew calendar, on the Hebrew birthday.' },
      { q: 'When is the yahrzeit candle lit?', a: 'The evening before: the Hebrew date begins at sunset.' },
    ],
  },

  js: {
    hebLabel: 'Hebrew date', gregLabel: 'Gregorian date',
    evening: ' evening — the Hebrew day has already begun',
    eveNote: 'The Hebrew day begins the evening before, at sunset on {date}.',
    errGYear: 'Enter a year from 1600 to 2200.', errNoDate: 'This date does not exist.',
    errHYear: 'Enter a Hebrew year from 5360 to 5960.', errMonthDays: '{month} {year} has {n} days.',
    nextBday: 'Next Hebrew birthday: <b>{heb}</b> — {greg}', turns: { one: ', turns {n}', other: ', turns {n}' },
    bar: 'Bar mitzvah (13)', bat: 'Bat mitzvah (12)',
    ifBirth: 'If this is a date of birth', ifDeath: 'If this is a date of death (yahrzeit)',
    nextYz: 'Next yahrzeit: <b>{heb}</b> — {greg}. The candle is lit the evening before.', yzNextYear: 'The yahrzeit starts from the year after the date.',
    ctaDays: { one: '{n} day until the next Hebrew birthday.', other: '{n} days until the next Hebrew birthday.' },
    ctaApp: 'The LeDor app reminds you the evening before and on the day.',
    monthsNom: { NISAN: 'Nisan', IYAR: 'Iyar', SIVAN: 'Sivan', TAMMUZ: 'Tammuz', AV: 'Av', ELUL: 'Elul', TISHREI: 'Tishrei', CHESHVAN: 'Cheshvan', KISLEV: 'Kislev', TEVET: 'Tevet', SHEVAT: 'Shevat', ADAR: 'Adar', ADAR_I: 'Adar I', ADAR_II: 'Adar II' },
    monthsGen: { NISAN: 'Nisan', IYAR: 'Iyar', SIVAN: 'Sivan', TAMMUZ: 'Tammuz', AV: 'Av', ELUL: 'Elul', TISHREI: 'Tishrei', CHESHVAN: 'Cheshvan', KISLEV: 'Kislev', TEVET: 'Tevet', SHEVAT: 'Shevat', ADAR: 'Adar', ADAR_I: 'Adar I', ADAR_II: 'Adar II' },
  },

  privacy: {
    title: 'Privacy Policy — LeDor',
    description: 'What data the LeDor app processes, where it is stored, for how long and how to delete it.',
    h1: 'Privacy Policy',
    updated: 'Version of 24 November 2026',
    body: `<p>This policy explains what data the LeDor mobile app and the kalendar.bothub.chat website process, why, for how long it is kept and how you can control it.</p>

<h2>1. Who is responsible</h2>
<p>LeDor is a non-commercial open-source project. The data controller is the app’s author, a private individual (Gershon). Contact: the “More → Send feedback” form in the app or the <a href="https://t.me/bothub_founder">author’s Telegram channel</a>.</p>

<h2>2. In short</h2>
<ul>
<li>No sign-up, ads, analytics or trackers.</li>
<li>We do not sell data or share it with third parties for advertising or marketing.</li>
<li>The app does not request location, contacts, photos or other permissions it does not need.</li>
</ul>

<h2>3. What is stored only on your phone</h2>
<ul>
<li>your personal list of events (names, dates, notes);</li>
<li>settings, the city chosen for sunset times, and the language;</li>
<li>PINs of connected shared lists.</li>
</ul>
<p>This data never leaves your phone unless you move events to a shared list.</p>

<h2>4. What is stored on the server</h2>
<p><b>Shared lists</b> — only if you create or connect a shared list: the list name, events (name or title, type, dates, note), technical timestamps and versions. The regular PIN is stored so editors can share the list; the Master PIN only as an irreversible hash. Purpose: syncing the list between members.</p>
<p><b>Feedback</b> — if you send feedback: its text and, only if you provided or allowed them, a contact for a reply and technical details (app version, phone model, Android version, language). Events and PINs are never included. Purpose: replying to you and improving the app. Feedback is forwarded to the author via Telegram.</p>
<p><b>Technical data</b> — every request to the server involves your IP address. To protect against PIN guessing, only its irreversible hash is stored.</p>
<p>Data is stored on the developer’s server; connections are encrypted (HTTPS).</p>

<h2>5. How long data is kept</h2>
<ul>
<li>a shared list and its events — until the list is deleted;</li>
<li>feedback — no longer than 2 years;</li>
<li>IP address hash — no longer than one day;</li>
<li>database backups — 14 days.</li>
</ul>

<h2>6. Legal bases</h2>
<p>Shared lists and syncing — providing what you ask for by creating or connecting a list. Feedback — your consent, given by sending it. Protection against PIN guessing — our legitimate interest in keeping the service secure.</p>

<h2>7. Who can see a shared list</h2>
<p>Anyone who knows its name and PIN. You decide whom to give the PIN to and are responsible for the consent of the people whose dates you add.</p>

<h2>8. Who receives data</h2>
<p>No one, except where required by law. The website uses the Cloudflare network, which technically processes visitors’ IP addresses. Feedback is delivered to the author via Telegram.</p>

<h2 id="delete">9. Your rights and deleting data</h2>
<p>You can ask what data about you is stored, correct it or delete it.</p>
<ul>
<li><b>An entire shared list:</b> “Lists” → the list → “Delete the list from the cloud” → Master PIN. The list is permanently deleted for all members.</li>
<li><b>A single event:</b> open the event → “Delete”. It disappears for everyone after syncing.</li>
<li><b>Disconnect a list from your phone:</b> “Lists” → the list → “Disconnect”.</li>
<li><b>Everything on your phone:</b> uninstall the app or clear its data in Android settings.</li>
<li><b>Feedback, a lost Master PIN, other requests:</b> write via “Send feedback” in the app with the list name and a contact for a reply. We reply within 30 days.</li>
</ul>
<p>If you are in the EU, you also have the right to lodge a complaint with a data protection authority.</p>

<h2>10. Android backups</h2>
<p>App data may be included in your phone’s backup in your Google account — this is controlled by your phone settings.</p>

<h2>11. Children</h2>
<p>The app is not intended for children under 13 and does not knowingly collect their data.</p>

<h2>12. Changes to this policy</h2>
<p>If the policy changes, the new version will appear on this page with a new date. We will announce significant changes in the app.</p>`,
  },

  root: { choose: 'Choose a language' },
};
