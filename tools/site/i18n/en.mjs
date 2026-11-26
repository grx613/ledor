// English (default for the site).
export default {
  lang: 'en', dir: 'ltr', ogLocale: 'en_US', name: 'English', letters: false,

  meta: {
    title: 'LeDor Kalendář — Jewish birthday & yahrzeit calendar for the whole family',
    description: 'Free app for families and communities: Hebrew birthdays, yahrzeits and anniversaries by the Hebrew and Gregorian calendars, a shared list synced by PIN, and reminders the evening before, ahead of sunset.',
    ogTitle: 'LeDor — reminds the whole family of birthdays and yahrzeits',
    ogDesc: 'Hebrew birthdays, yahrzeits and anniversaries. A shared list by PIN, reminders before sunset. Free, no ads.',
  },
  nav: { download: 'Download', lang: 'Language' },
  stores: { soon: 'Coming soon to', apk: 'Download for Android', apkNote: 'APK file · Android 8.0 or later' },
  lists: ['Cohen family', 'Chabad of New York', 'Beth Shalom community'],

  hero: {
    h1: 'Reminds the whole family of birthdays and yahrzeits',
    lead: 'LeDor keeps track of family dates by the Hebrew and Gregorian calendars and reminds you ahead of time. And the list is easy to share with your family or community — everyone gets the same dates on their phone.',
    note: 'Free · no ads · no sign-up',
    alt: 'LeDor calendar: Hebrew and Gregorian dates, upcoming events from family and community lists',
    listsCap: 'Your lists', now: 'now',
  },

  remind: {
    kicker: 'Reminders',
    h2: 'A reminder the evening before — and on the day',
    sub: 'A Hebrew date falls on a different day every year, and the day begins at nightfall. LeDor does the math and reminds you while there is still time.',
    points: [
      { icon: 'candle', t: 'The evening before, ahead of sunset', p: 'By default three hours before sunset in your city: time to call, set the table or light a memorial candle.' },
      { icon: 'bell', t: 'And again on the day', p: 'At noon, or at the time you choose.' },
      { icon: 'star', t: 'By the Hebrew calendar, the Gregorian, or both', p: 'For each event you decide which date to observe.' },
    ],
    cap: { eve: 'The evening before · 3 hours before sunset', day: 'On the day · 12:00', yz: 'The evening before a yahrzeit' },
    notifs: {
      eve: { title: 'This evening: Birthday — Sarah Cohen', text: '19 Kislev 5787 begins at sunset. Turns 37' },
      day: { title: 'Today: Birthday — Sarah Cohen', text: '19 Kislev 5787 · 29 November 2026. Turns 37' },
      yz: { title: 'Yahrzeit this evening: Grandpa Aaron', text: '20 Kislev 5787 begins at sunset. Time to light the memorial candle.' },
    },
  },

  share: {
    kicker: 'Shared lists',
    h2: 'Share the list with your family or community',
    sub: 'One person adds the dates — everyone sees them and gets reminders. No sign-up, email or passwords.',
    steps: [
      { t: 'Create a list', p: '“Cohen family”, “Our community”, “Friends” — you can have several lists.' },
      { t: 'Tap “Share”', p: 'LeDor sends an invitation via WhatsApp or Telegram with the list name and access code.' },
      { t: 'Your family connects', p: 'They install LeDor, enter the name and code — and see the same dates, with reminders on their own phone.' },
    ],
    points: [
      '<b>You decide who can edit.</b> One code lets people view, another lets them add and correct dates.',
      '<b>Several lists at once.</b> Family, community, friends — each list has its own color and notifications.',
      '<b>Works offline.</b> Changes are sent once you are back online.',
      '<b>No accounts.</b> No email, phone number or password.',
    ],
    sync: 'Changes arrive when someone opens the app or pulls the list down, and once a day in the background.',
    msgCap: 'Invitation via WhatsApp or Telegram',
    msg: ['The list “Cohen family” in the LeDor app: birthdays, yahrzeits and family events by the Hebrew and Gregorian calendars.', '1. Install the app: kalendar.bothub.chat', '2. Open “Lists” → “Connect a list”', '3. Name: Cohen family', '4. PIN: 4821'],
    altLists: 'Several lists in LeDor: personal, family and two communities',
    altEvents: 'Events from all lists, each labeled with its list',
  },

  features: {
    kicker: 'And more',
    h2: 'Built for the Hebrew calendar',
    items: [
      { icon: 'star', t: 'Both dates side by side', p: 'Every event is shown by both the Hebrew and the Gregorian calendar — in the calendar and in the list.' },
      { icon: 'scroll', t: 'Yahrzeit by the rules', p: 'Adar I and Adar II in a leap year, the 30th of Cheshvan and Kislev — all handled. Choose your Adar custom in settings.' },
      { icon: 'bar', t: 'Bar and bat mitzvah', p: 'Enter a date of birth — LeDor tells you the bar mitzvah and bat mitzvah dates.' },
      { icon: 'battery', t: 'Easy on the battery', p: 'No background work and no location: it wakes up only at the moment of a reminder.' },
      { icon: 'globe', t: 'Six languages', p: 'English, עברית, Русский, ייִדיש, Français, Español. In Hebrew and Yiddish, dates are written in letters.' },
      { icon: 'heart', t: 'Free and open', p: 'No ads, subscriptions or data collection. The source code is open.' },
    ],
  },

  cta: { h2: 'Install LeDor — and important dates stop slipping by', p: 'Free, no ads, no sign-up.' },

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
      { q: 'Does it account for sunset?', a: 'Yes. When entering a date you can mark “after sunset” — the Hebrew date is then the next one. The evening reminder comes a chosen time before sunset in your city. Not sure whether the sun had set? Enter the time and city — the app works it out.' },
      { q: 'How is a yahrzeit calculated in a leap year?', a: 'For someone who passed away in Adar of a regular year, Ashkenazi custom observes it in Adar I and Sephardi custom in Adar II; you choose the custom in settings. The rules for the 30th of Cheshvan and Kislev are applied too.' },
      { q: 'Does the app work offline?', a: 'Yes. The internet is needed only for shared lists, for sending feedback and for checking for updates when you tap the button; changes are sent once you are back online.' },
      { q: 'How much battery does the app use?', a: 'Practically none. There are no background services, no location and no permanent connection. The phone wakes LeDor for a split second only at the moment of a reminder — the evening before and on the day — and, if you use a shared list, once a day to sync, when the system finds it convenient.' },
      { q: 'Which languages does LeDor support?', a: 'English, עברית, Русский, ייִדיש, Français, Español. In Hebrew and Yiddish the interface is right-to-left and Hebrew dates are written in letters.' },
      { q: 'Can I move my events to a shared list?', a: 'Yes. When creating a list, check “Move events from the personal list”, or open the personal list and choose “Move events to a cloud list”.' },
      { q: 'Is there a dark theme?', a: 'Yes: light, dark or system — under “Settings → Appearance”.' },
      { q: 'Does LeDor show Jewish holidays and candle-lighting times?', a: 'Yes. Under Lists there is a built-in “Jewish holidays” list. It is off by default — turn it on with its switch. Holidays and fasts then appear in the calendar, with a reminder the evening before. For Shabbat and holidays it shows the candle-lighting time in your city; the Shabbat reminder is switched on separately. You can turn reminders off and simply look up the dates.' },
      { q: 'Is there an iPhone version?', a: 'Coming soon. LeDor is currently available for Android.' },
      { q: 'How much does the app cost?', a: 'It is free, with no ads or paid content. The project is non-commercial and open source.' },
    ],
  },

  author: {
    h2: 'About the author', name: 'Gershon',
    p: 'Author of LeDor. Found a mistake in a date or have an idea? Write right from the app: “More → Send feedback”.',
    channel: 'Author’s Telegram channel', photoAlt: 'Gershon with a lulav in the sukkah',
  },
  eco: {
    h2: 'Other BotHub projects',
    bothub: 'ChatGPT, Claude, Gemini, Grok and other AI models in one place — no subscription.',
    fire: 'Top market news: stocks, rates, oil, crypto — short stories from dozens of outlets.',
  },
  footer: { privacy: 'Privacy policy', delete: 'Delete data', converter: 'Date converter', faq: 'FAQ', copy: 'non-commercial project · no ads or trackers' },

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
    updated: 'Version of 26 November 2026',
    body: `<p>This policy explains what data the LeDor mobile app and the kalendar.bothub.chat website process, why, for how long it is kept and how you can control it.</p>

<h2>1. Who is responsible</h2>
<p>LeDor is a non-commercial open-source project. The data controller is the app’s author, a private individual (Gershon). Contact: email <a href="mailto:grx613@gmail.com">grx613@gmail.com</a>, the “More → Send feedback” form in the app or the <a href="https://t.me/bothub_founder">author’s Telegram channel</a>.</p>

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
<p><b>Technical data</b> — every request to the server involves your IP address. To protect against PIN guessing, only its irreversible hash is stored. When you tap “Check for updates”, the app requests the latest version number from this website; nothing about you or your events is sent.</p>
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
<p>No one, except where required by law. Requests to the website and server pass through a hosting provider, which technically processes visitors’ IP addresses. Feedback is delivered to the author via Telegram.</p>

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

  // Страница «Один календарь на всю семью» (B-38): /<язык>/family/
  family: {
    "title": "One calendar for the whole family — shared lists in LeDor",
    "description": "How you and your family can see the same birthdays and yahrzeits and get reminders: a LeDor shared list in five steps.",
    "h1": "One calendar for the whole family",
    "intro": "You and your spouse (or the whole family) want to see the same dates: whoever adds a birthday, it shows up for both of you, and the reminder comes to both phones. That is what a LeDor <b>shared list</b> is for. Only people you give its name and code can see it.",
    "link": "Step-by-step guide with pictures →",
    "footer": "Shared lists: how it works",
    "steps": [
      {
        "t": "Open “Lists”",
        "p": "On the calendar or in “Events”, tap “Lists” at the top. You will see your “Personal” list (it lives only on this phone) and the “Create” and “Connect” buttons.",
        "shot": "10_lists"
      },
      {
        "t": "Create a shared list",
        "p": "Tap “Create”. Choose a name — say, “The Cohen Family” — and two codes: a <b>read PIN</b> (4–8 digits, for viewing only) and a <b>master PIN</b> (6–12 digits, for adding and editing). If your “Personal” list already has dates, tick “Move events from the personal list”.",
        "shot": "11_list_create"
      },
      {
        "t": "Share it with your family",
        "p": "Open the new list. So that your spouse can add dates too, turn on “Share editing rights” and tap WhatsApp or Telegram. For relatives who only need to see the dates, send the invitation without that switch.",
        "shot": "14_list_share"
      },
      {
        "t": "They connect the list",
        "p": "They install LeDor, open “Lists” → “Connect” and enter the list name and code from the message. Nothing else: no sign-up, no email.",
        "shot": "11b_list_join"
      },
      {
        "t": "Done — one calendar for everyone",
        "p": "Now whatever any of you adds, everyone sees, and reminders come to every phone. New events go to the shared list by default. A colored tag shows which list an event belongs to.",
        "shot": "05_events"
      }
    ],
    "faq": [
      {
        "q": "Who else can see our list?",
        "a": "No one. A list opens only with its name <b>and</b> code. The server does not allow guessing: after a few wrong tries, attempts are limited."
      },
      {
        "q": "What is the difference between the two codes?",
        "a": "The read PIN lets you see the dates and get reminders. The master PIN also lets you add, edit and delete. Give the master PIN only to people you trust."
      },
      {
        "q": "On first launch the app asked about “importing a list”. What file do I need?",
        "a": "None. It is the same as “Connect”: if a relative has already created a shared list, enter its name and code. If not, tap “No” and create your own as described above."
      },
      {
        "q": "What if I do not want to share anything?",
        "a": "Use the “Personal” list: it is stored only on your phone and is never sent anywhere."
      },
      {
        "q": "Can I have several lists?",
        "a": "Yes: “Family”, “Community”, “Friends” — each with its own color and codes. Notifications can be turned on or off for each list."
      }
    ]
  },

  root: { choose: 'Choose a language' },
};
