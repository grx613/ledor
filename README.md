# LeDor · לדור ודור

**LeDor VaDor — из поколения в поколение.** Напоминания о днях рождения, йорцайтах и семейных датах
по еврейскому и григорианскому календарям.

*Jewish birthdays, yahrzeits and family dates by the Hebrew and Gregorian calendars — native Android app,
reminders before sunset, shared family lists, zero battery drain. Free, no ads.*

Сайт и онлайн-конвертер дат: **https://kalendar.bothub.chat** · Автор — Гершон ([канал в Telegram](https://t.me/bothub_founder)).

## Что умеет

- Еврейская и обычная дата для каждого события; отмечать можно по одному календарю или **по обоим**.
- Правила годовщин по галахе: Адар в високосный год, 30 Хешвана/Кислева, 29 февраля (проверено на эталонных векторах Hebcal).
- Два напоминания: накануне вечером — за выбранное время до заката в вашем городе — и в сам день.
- Встроенный список еврейских праздников и постов с напоминанием накануне; время зажигания свечей перед Шабатом и праздниками.
- Первый год после утраты: шлошим, 11 и 12 месяцев, первый йорцайт; дни Изкора в напоминаниях о праздниках.
- Общие списки семьи или общины по ПИН-коду: обычный ПИН — только чтение, мастер-ПИН — редактирование.
- Конвертер дат с подсказкой бар/бат-мицвы и йорцайтов на 10 лет вперёд.
- Языки: English, עברית, Русский, ייִדיש, Français, Español. На иврите и идише даты буквами.
- Без рекламы, аналитики, геолокации и фоновой работы: приложение просыпается только в момент напоминания.

## Устройство

| Папка | Что внутри |
|---|---|
| `android/` | приложение: Kotlin + Jetpack Compose, Room, WorkManager, [KosherJava](https://github.com/KosherJava/zmanim) |
| `supabase/migrations/` | схема БД и RPC-функции (PostgreSQL; работает в Supabase и на своём сервере через PostgREST) |
| `tools/server/` | установка своего бэкенда: PostgreSQL + PostgREST за Caddy, резервные копии, отзывы в Telegram |
| `website/` | статический сайт с онлайн-конвертером (без зависимостей) |
| `spec/golden/` | эталонные тест-векторы еврейского календаря, общие для приложения и сайта |
| `docs/` | ТЗ, архитектура, календарные правила, план и журнал решений |

## Сборка и проверка

```bash
source tools/env.sh                                   # JAVA_HOME, ANDROID_HOME
cp android/local.properties.example android/local.properties   # адрес API и публичный ключ
cd android && ./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
node --test website/tests/*.test.mjs                  # конвертер сайта
```

Без `local.properties` приложение работает полностью, кроме общих списков и отзывов.

## Документация

| Файл | Содержание |
|---|---|
| [`docs/01-product-spec.md`](docs/01-product-spec.md) | техническое задание: экраны и поведение |
| [`docs/02-architecture.md`](docs/02-architecture.md) | архитектура, стек, структура кода |
| [`docs/03-calendar-domain.md`](docs/03-calendar-domain.md) | календарная логика и правила |
| [`docs/04-backend.md`](docs/04-backend.md) | сервер: база данных, API, развёртывание |
| [`docs/05-reminders-battery.md`](docs/05-reminders-battery.md) | напоминания без расхода батареи |
| [`docs/06-design-system.md`](docs/06-design-system.md) | дизайн-система |
| [`docs/07-website.md`](docs/07-website.md) | сайт: структура, языки, SEO, выкладка |
| [`docs/08-store-compliance.md`](docs/08-store-compliance.md) | требования магазинов |
| [`docs/09-ios-notes.md`](docs/09-ios-notes.md) | iOS: план и отличия |
| [`docs/PLAN.md`](docs/PLAN.md) | план по вехам и журнал |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | решения и отклонения |
| [`docs/BACKLOG.md`](docs/BACKLOG.md) | идеи и их статус |

## Лицензия

[GPL-3.0](LICENSE). Код можно изучать, менять и распространять, но производные версии тоже должны оставаться открытыми.
