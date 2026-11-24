# 02. Архитектура проекта

## 1. Репозиторий (монорепозиторий)

```
kalendar/
├── CLAUDE.md                  правила для модели-исполнителя (читать первым)
├── README.md
├── docs/                      ТЗ, архитектура, план (этот каталог)
├── spec/golden/               эталонные тест-векторы (общие для Android, сайта, iOS)
├── tools/
│   ├── golden/                генератор векторов (Node)
│   └── rpc-smoke/             интеграционный тест бэкенда (Node)           ← создаётся в вехе M4
├── assets/brand/              логотип BotHub FiRe (оригиналы с сайта)
├── android/                   Android-проект (корень Gradle)               ← M0
│   ├── domain/                чистый Kotlin/JVM: календарь, правила, планировщик
│   └── app/                   Android-приложение (Compose)
├── supabase/migrations/       SQL-миграции                                 ← M4
├── website/                   статический сайт разработчика                ← M14
└── ios/                       позже (см. 09-ios-notes.md)
```

## 2. Стек и версии (проверены 24.11.2026 по Maven Central / Google Maven)

| Компонент | Версия | Примечание |
|---|---|---|
| JDK | Temurin 21 (LTS) | AGP 9.5 требует ≥ 17 |
| Gradle | 9.9.2 | AGP 9.5 требует ≥ 9.6.0 |
| Android Gradle Plugin | 9.5.2 | максимум API 37; **встроенная поддержка Kotlin** |
| Kotlin | 2.4.21 | + плагины `kotlin.plugin.compose`, `kotlin.plugin.serialization` той же версии |
| KSP | 2.3.14 | для Room |
| Compose BOM | 2026.11.01 | Material 3 (1.5.x) |
| activity-compose | 1.13.0 | |
| navigation-compose | 2.10.2 | типобезопасные маршруты (`@Serializable`) |
| lifecycle (runtime/viewmodel-compose) | 2.11.1 | |
| Room | 2.8.5 | KSP, `exportSchema = true` |
| DataStore Preferences | 1.3.0 | |
| WorkManager | 2.12.0 | |
| core-ktx / core-splashscreen | 1.19.1 / 1.2.0 | |
| kotlinx-coroutines | 1.11.0 | |
| kotlinx-serialization-json | 1.13.0 | |
| OkHttp (+ mockwebserver3 в тестах) | 5.5.1 | |
| **KosherJava zmanim** | **3.1.0** | только в `:domain`; API на `java.time` — см. `03-calendar-domain.md` §2 |
| JUnit 4 / Robolectric / Roborazzi | 4.13.2 / 4.18.1 / 1.79.0 | тесты на JVM, без эмулятора |

SDK: `minSdk = 26`, `targetSdk = 36`, **`compileSdk` = 37.1**. Проверено по метаданным библиотек: Compose UI 1.13.0
(из BOM 2026.11.01) требует `minCompileSdk = 37`, `minCompileMinorSdk = 1`, а `core-ktx 1.19.1` — `minCompileSdk = 37`;
с `compileSdk 36` сборка не пройдёт. Пакеты SDK: `platforms;android-37.1`, `build-tools;36.0.0` (умолчание AGP 9.5).
Синтаксис минорной версии (появился в AGP 8.13):

```kotlin
android {
    compileSdk { version = release(37) { minorApiLevel = 1 } }
    defaultConfig { minSdk = 26; targetSdk = 36 }
}
```

`targetSdk` остаётся 36: поведение Android 16 хорошо известно, а требование Google Play на конец 2026 года — не ниже 36.

Правила работы с версиями:
- Все версии — в `android/gradle/libs.versions.toml`. Версии из таблицы — отправная точка; обновлять только
  при реальной несовместимости, с записью в `DECISIONS.md`.
- **AGP 9 отличается от того, что модель помнит по AGP 8**: плагин `org.jetbrains.kotlin.android` применять
  не нужно (Kotlin встроен), вместо `kotlin-kapt` — KSP. Если сборка ведёт себя не так, как описано здесь, —
  верить официальной документации (`developer.android.com/build/releases/gradle-plugin`,
  страница «Migrate to built-in Kotlin»), а не памяти, и зафиксировать отклонение в `DECISIONS.md`.
- Новые зависимости не добавлять без записи в `DECISIONS.md` с обоснованием. Запрещены: Flutter/React Native/KMP-UI,
  Firebase и Google Play Services, любые SDK аналитики и рекламы, `supabase-kt`, Hilt/Dagger/Koin.

## 3. Модули Gradle

| Модуль | Тип | Зависит от | Содержимое |
|---|---|---|---|
| `:domain` | `org.jetbrains.kotlin.jvm` | KosherJava, kotlinx-serialization (только тесты и `cities.json`) | календарь, события, правила годовщин, планировщик напоминаний, валидация |
| `:app` | Android application | `:domain`, AndroidX, OkHttp | данные, синхронизация, будильники, UI |

Почему `:domain` отдельно: вся сложная логика тестируется за секунды обычным `./gradlew :domain:test`
без Android и эмулятора; её границы совпадают с тем, что потом переносится на Swift.

Идентификаторы: `applicationId = chat.bothub.fire.kalendar` (рабочее, см. вопрос Q-01), пакет кода тот же.

## 4. Пакеты

### `:domain` — `chat.bothub.fire.kalendar.domain`

```
calendar/    HebrewMonth, HebrewDate, CalendarKind, HebrewCalendarMath
event/       Event, EventType, EventDate, GregorianParts, HebrewParts, EventValidator, EventDefaults,
             AnniversaryRule, YahrzeitAdarPolicy, AnniversaryCalculator,
             Occurrence, OccurrenceCalculator, MitzvahHint, MitzvahCalculator
reminder/    ReminderSettings, ReminderKind, EveMode, PlannedReminder, ReminderPlanner,
             SunsetProvider, CitySunsetProvider, DefaultSunsetProvider, City, CityCatalog
lists/       ListNameRules, PinRules, PinGenerator
```

### `:app` — `chat.bothub.fire.kalendar`

```
KalendarApp            Application: AppContainer, каналы уведомлений, первый rearm()
AppContainer           ручное внедрение зависимостей (lazy-поля), без фреймворков
MainActivity           единственная Activity: enableEdgeToEdge(), setContent { KalendarRoot() }

data/local/            AppDatabase, ListEntity, EventEntity, ListDao, EventDao, Mappers
data/prefs/            SettingsStore (DataStore Preferences), AppSettings
data/remote/           SupabaseRpc, RpcResult, Dto (kotlinx.serialization)
data/repo/             ListRepository, EventRepository, SyncEngine, NoticeQueue
reminders/             ReminderScheduler, AlarmGateway (+ AndroidAlarmGateway), ReminderReceiver,
                       SystemEventsReceiver, NotificationFactory, ReminderHealthChecker
sync/                  SyncWorker, SyncScheduler
share/                 ShareMessageBuilder, ShareLauncher
ui/theme/              Color, Type, Shape, KalendarTheme
ui/components/         общие компоненты (см. 06-design-system.md §5)
ui/format/             DateFormatters (еврейские/григорианские даты, «через N дней», склонения)
ui/navigation/         Routes, KalendarNavHost, нижняя панель
ui/splash/ ui/onboarding/ ui/calendar/ ui/events/ ui/editor/ ui/detail/ ui/converter/
ui/lists/ ui/settings/ ui/about/        — по экрану: <Имя>Screen.kt + <Имя>ViewModel.kt + <Имя>UiState.kt
```

## 5. Слои и поток данных

```
 Compose-экран ──события UI──▶ ViewModel ──▶ Repository ──▶ Room (единственный источник истины для UI)
       ▲                           │              │
       └── StateFlow<UiState> ─────┘              ├──▶ SupabaseRpc (только через SyncEngine)
                                                  └──▶ изменение в Room ──▶ ReminderScheduler.rearm()
```

- **UI читает только Room.** Сеть никогда не блокирует экран: правка сохраняется локально, затем отправляется.
- ViewModel: один `StateFlow<UiState>` (неизменяемый data class), публичные функции-обработчики; `collectAsStateWithLifecycle()`.
  Вычисления с календарём — на `Dispatchers.Default`.
- Вся работа с датами в ViewModel — через `:domain`; в Compose-коде вычислений нет, только отображение.
- Время: `java.time.Clock` и «сегодня» передаются параметрами (в `AppContainer` — системные). В тестах — фиксированные.
- «Сегодня» как поток: `todayFlow` — выдаёт дату при подписке, в локальную полночь и при возврате приложения на экран.
- Строки интерфейса — только в ресурсах. `:domain` строк не содержит (возвращает типы и коды).

## 6. Локальная база (Room, версия 1)

### Таблица `lists`

| Колонка | Тип | Описание |
|---|---|---|
| `id` | TEXT PK | `"local"` для личного списка или UUID облачного |
| `kind` | TEXT | `LOCAL` / `CLOUD` |
| `name` | TEXT | |
| `role` | TEXT | `OWNER` (личный), `READER`, `EDITOR` |
| `state` | TEXT | `ACTIVE` / `ACCESS_LOST` |
| `read_pin` | TEXT? | сохранённый обычный ПИН |
| `master_pin` | TEXT? | сохранённый Мастер-ПИН (только если пользователь его вводил/создавал) |
| `access_token` | TEXT? | токен устройства |
| `last_synced_rev` | INTEGER | по умолчанию 0 |
| `last_sync_at` | INTEGER? | epoch millis |
| `notify_enabled` | INTEGER | 1/0 — уведомления по этому списку |
| `color_index` | INTEGER | цвет метки списка (0…7) |
| `sort_order` | INTEGER | порядок в интерфейсе |
| `created_at` | INTEGER | |

Личный список создаётся при первом запуске (`RoomDatabase.Callback.onCreate`): `id = "local"`, `kind = LOCAL`,
`role = OWNER`, название берётся из ресурсов при отображении (в базе — пустая строка).

### Таблица `events`

Поля события 1:1 с `Event` из `:domain` и с серверной таблицей (`title`, `type`, `custom_label`, `recurring`,
`observe_by`, `input_calendar`, `g_year`, `g_month`, `g_day`, `after_sunset`, `h_year`, `h_month`, `h_day`, `note`) плюс:

| Колонка | Тип | Описание |
|---|---|---|
| `id` | TEXT PK | UUID |
| `list_id` | TEXT, FK → `lists.id` ON DELETE CASCADE, индекс | |
| `rev` | INTEGER | серверная ревизия, 0 для несинхронизированных |
| `sync_state` | TEXT | `SYNCED` / `PENDING_UPSERT` / `PENDING_DELETE` |
| `updated_at` | INTEGER | epoch millis |

- В личном списке события всегда `SYNCED`, удаление — физическое.
- В облачном списке: правка → `PENDING_UPSERT`; удаление → `PENDING_DELETE` (строка скрыта из интерфейса и
  физически удаляется после подтверждения сервером).
- Все запросы для UI исключают `PENDING_DELETE`.
- Перемещение события между списками = удаление в старом + создание с **новым** `id` в новом (одна транзакция).

### Правила редактирования

Событие можно менять, если `list.role ∈ {OWNER, EDITOR}` и `list.state == ACTIVE`. Иначе — только просмотр.

### `SettingsStore` (DataStore)

| Ключ | Тип | Умолчание |
|---|---|---|
| `onboardingImportShown` | Boolean | false |
| `eveEnabled`, `eveMode`, `eveMinutesBeforeSunset`, `eveFixedTime` | см. `ReminderSettings` | true, BEFORE_SUNSET, 180, 15:00 |
| `dayEnabled`, `dayTime` | | true, 12:00 |
| `cityId` | String? | null (закат = 18:00) |
| `healthWarningsEnabled` | Boolean | true |
| `reminderWatermark` | Long (epoch millis) | момент первого запуска |
| `weekStartsOnSunday` | Boolean | true |
| `themeMode` | SYSTEM/LIGHT/DARK | SYSTEM |
| `calendarGridMode` | GREGORIAN/HEBREW | GREGORIAN |
| `yahrzeitAdarPolicy` | ADAR_I/ADAR_II | ADAR_I |
| `lastSyncAttemptAt` | Long | 0 |
| `pendingNotices` | String (JSON-массив) | `[]` — очередь внутренних сообщений («список удалён владельцем») |

## 7. Ключевые интерфейсы приложения

```kotlin
class EventRepository {
    fun observeAll(): Flow<List<Event>>                  // без PENDING_DELETE
    fun observeByList(listId: String): Flow<List<Event>>
    suspend fun get(id: String): Event?
    suspend fun save(event: Event)                       // вставка/обновление; для облачных -> PENDING_UPSERT + разовая синхронизация
    suspend fun delete(id: String)
    suspend fun move(id: String, targetListId: String)
}

class ListRepository {
    fun observeLists(): Flow<List<EventList>>            // с числом событий
    suspend fun checkExists(name: String): RpcResult<Boolean>
    suspend fun create(name: String, readPin: String, masterPin: String, moveLocalEvents: Boolean): RpcResult<EventList>
    suspend fun join(name: String, pin: String): RpcResult<JoinOutcome>      // + первая синхронизация; сколько событий импортировано
    suspend fun disconnect(listId: String)               // leave_list (ошибки игнорировать) + локальное удаление
    suspend fun deleteFromCloud(listId: String, masterPin: String): RpcResult<Unit>
    suspend fun setNotifyEnabled(listId: String, enabled: Boolean)
}

class SyncEngine { suspend fun syncAll(): SyncReport; suspend fun syncList(listId: String): SyncReport }
class ReminderScheduler { suspend fun rearm() }
```

`AppContainer` создаёт всё лениво: `database`, `settingsStore`, `rpc`, репозитории, `syncEngine`,
`reminderScheduler`, `healthChecker`, `cityCatalog`, `clock`, `appScope` (`SupervisorJob + Dispatchers.Default`).
ViewModel'и получают зависимости через `viewModelFactory { initializer { … } }`.

## 8. Навигация

Одна Activity, Navigation Compose, типобезопасные маршруты.

```
Splash ──▶ Main (нижняя панель)
             ├─ Calendar            стартовая вкладка
             ├─ Events
             ├─ Converter
             └─ More ──▶ Lists ──▶ ListDetail(listId) / ListCreate / ListJoin(prefillName?)
                     ├─▶ Settings ──▶ CityPicker
                     └─▶ About
EventEditor(eventId?, presetCalendar?, presetYear?, presetMonth?, presetDay?, presetAfterSunset?)
EventDetail(eventId)       — открывается и по нажатию на уведомление
```

Диалог импорта при первом запуске и баннер «здоровья» — не маршруты, а состояние экрана `Main`.

## 9. Производительность и размер

| Цель | Как |
|---|---|
| Первый кадр < 500 мс на среднем телефоне | никакой работы на главном потоке в `Application.onCreate`: всё ленивое; база и DataStore открываются на фоновых диспетчерах |
| Заставка не «тормозит» запуск | брендовая заставка 1500 мс идёт параллельно с загрузкой данных, а не после неё |
| Плавный календарь | наступления считаются один раз на окно «−1…+13 месяцев» и кэшируются во ViewModel; ячейки — стабильные data-классы |
| APK < 7 МБ | R8 (`isMinifyEnabled`, `isShrinkResources`) в release; иконки — векторные XML, без `material-icons-extended` |
| 0 % батареи в фоне | см. `05-reminders-battery.md` |

## 10. Тестирование (всё на JVM, эмулятор не нужен)

| Уровень | Инструмент | Что покрывает |
|---|---|---|
| `:domain` | JUnit 4 + эталонные векторы | конвертация, правила годовщин, наступления, бар/бат-мицва, планировщик, валидация, города |
| `:app` данные | Robolectric + Room in-memory + MockWebServer | DAO, мапперы, `SupabaseRpc`, `SyncEngine` (все ветки ошибок) |
| `:app` напоминания | Robolectric + поддельные шлюзы | `ReminderScheduler`, `ReminderHealthChecker`, ресиверы, `SyncWorker` |
| `:app` ViewModel | kotlinx-coroutines-test | состояния редактора, конвертера, списков |
| UI | Roborazzi (снимки экранов под Robolectric) | каждый экран: светлая/тёмная тема, ru/en. Исполнитель **обязан открыть PNG и посмотреть глазами** |

Команда полной проверки (должна проходить в конце каждой вехи):

```bash
cd android && ./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Roborazzi — вспомогательный инструмент самопроверки вёрстки. Если его не удаётся завести с текущими версиями
AGP/Robolectric за ~40 минут — отложить, записать в `DECISIONS.md` и продолжать (сборка и остальные тесты важнее).

## 11. Локализация

- `values/strings.xml` — английский (обязательный запасной язык), `values-ru/` — русский (первичные тексты из ТЗ).
  Иврит (`values-iw/`) — вторая очередь; вёрстка сразу RTL-совместимая (`start/end`, `android:supportsRtl="true"`).
- Названия еврейских месяцев — массивы ресурсов; для русского **две формы**: именительный («Нисан 5787» — заголовки)
  и родительный («28 Нисана» — даты). Григорианские даты — `DateTimeFormatter` с текущей локалью
  (`d MMMM yyyy` для дат, `LLLL yyyy` для заголовков — иначе в русском будет «ноября 2026» вместо «Ноябрь 2026»).
- Множественное число («через 3 дня», «исполняется 37 лет») — только через `plurals`.
- `android:localeConfig` со списком языков → язык приложения выбирается в системных настройках (Android 13+).

| Месяц | ru (им.) | ru (род.) | en | he |
|---|---|---|---|---|
| NISAN | Нисан | Нисана | Nisan | ניסן |
| IYAR | Ияр | Ияра | Iyar | אייר |
| SIVAN | Сиван | Сивана | Sivan | סיוון |
| TAMMUZ | Таммуз | Таммуза | Tammuz | תמוז |
| AV | Ав | Ава | Av | אב |
| ELUL | Элул | Элула | Elul | אלול |
| TISHREI | Тишрей | Тишрея | Tishrei | תשרי |
| CHESHVAN | Хешван | Хешвана | Cheshvan | חשוון |
| KISLEV | Кислев | Кислева | Kislev | כסלו |
| TEVET | Тевет | Тевета | Tevet | טבת |
| SHEVAT | Шват | Швата | Shevat | שבט |
| ADAR | Адар | Адара | Adar | אדר |
| ADAR_I | Адар I | Адара I | Adar I | אדר א׳ |
| ADAR_II | Адар II | Адара II | Adar II | אדר ב׳ |
