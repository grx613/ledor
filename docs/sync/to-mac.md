# Записки Mac от сервера

Пишет только сессия на сервере, читает сессия на Mac. Правила и формат — `README.md` в этой папке. Новые записи — сверху, номера `S-1`, `S-2`…

## S-6 · 2026-11-26 · Страница в Google Play: строка о запросе «не экономить батарею»

Статус: открыто

Заказчик спросил, как удержать разрешение `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` при проверке рабочей версии, и 2026-11-26 согласился добавить в описание строку о нём — чтобы проверяющий видел связь с главной функцией. Тексты в `tools/store/i18n/*.mjs` обновлены (`node tools/store/build.mjs <каталог>` → `<язык>/listing.txt`).

**Что сделать в консоли:** «Страница приложения» → полное описание → раздел «Напоминания вовремя» (REMINDERS ON TIME) → добавить последним, четвёртым пунктом, на каждом из пяти языков:

```
<en-US>
• So that reminders are not lost, LeDor asks once to be excluded from battery saving: phones put rarely opened apps to sleep, and their reminders do not arrive.
</en-US>
<ru-RU>
• Чтобы напоминания не терялись, LeDor один раз просит не ограничивать его экономией батареи: телефоны усыпляют редко открываемые приложения, и их напоминания не приходят.
</ru-RU>
<iw-IL>
• כדי שתזכורות לא יאבדו, LeDor מבקש פעם אחת לא להגביל אותו בחיסכון בסוללה: טלפונים מרדימים אפליקציות שנפתחות לעיתים רחוקות, והתזכורות שלהן לא מגיעות.
</iw-IL>
<fr-FR>
• Pour que les rappels ne se perdent pas, LeDor demande une seule fois à ne pas être limité par l'économie de batterie : les téléphones mettent en veille les applications rarement ouvertes, et leurs rappels n'arrivent pas.
</fr-FR>
<es-ES>
• Para que los recordatorios no se pierdan, LeDor pide una sola vez que el ahorro de batería no lo limite: los teléfonos ponen en reposo las apps que se abren poco, y sus recordatorios no llegan.
</es-ES>
```

Остальной текст описания не менять. Правка страницы уходит на проверку Google — перед отправкой на проверку подтверди у заказчика, как обычно (правило 9). Сейчас на проверке Alpha 0.6.5: если консоль объединит правку страницы с ним в одну отправку — так и скажи заказчику.

Для App Store строка не нужна: там такого разрешения нет.

Ответь записью `M-…`, когда страница обновлена.

## S-5 · 2026-11-26 · Android 0.6.5: главная дата — та, что отмечается (iOS и Google Play)

Статус: закрыто (M-4). Полный текст — в истории Git, коммит `5c5463c`.

## S-4 · 2026-11-26 · Android 0.6.4: выпуск в Google Play и три изменения для iOS

Статус: закрыто (M-3). Правило дат из п. 3.1 уточнено в S-5. Полный текст — в истории Git, коммит `5e30862`.

## S-3 · 2026-11-26 · Ответ на M-1

Статус: закрыто (M-2).

## S-2 · 2026-11-26 · Автоматическая проверка почты

Статус: закрыто (M-2: наблюдатель на Mac запущен).

## S-1 · 2026-11-26 · Выпуск 0.6.3 в Google Play и те же исправления в iOS

Статус: закрыто (M-1). Полный текст — в истории Git, коммит `8045d4e`.
