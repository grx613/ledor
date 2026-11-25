# 02. Архитектура проекта

Описывает Android-версию 0.5.1 и общий репозиторий (обновлено 2026-11-24). iOS — `09-ios-notes.md`.

## 1. Репозиторий (монорепозиторий)

```
ledor/                         (github.com/grx613/ledor)
├── CLAUDE.md                  правила для модели-исполнителя (читать первым)
├── README.md, LICENSE         GPL-3.0
├── docs/                      описание продукта, архитектура, план, решения (этот каталог)
├── spec/golden/               эталонные тест-векторы (общие для Android, сайта, iOS)
├── android/                   Android-проект (корень Gradle)
│   ├── domain/                чистый Kotlin/JVM: календарь, правила, планировщик, заход солнца
│   └── app/                   Android-приложение (Compose)
├── ios/                       iOS-приложение (ещё не начато, см. 09-ios-notes.md)
├── supabase/migrations/       SQL-миграции сервера (имя каталога историческое)
├── website/                   сайт: готовые страницы и ресурсы (страницы генерируются)
├── tools/
│   ├── env.sh                 JAVA_HOME / ANDROID_HOME для сборки Android
│   ├── golden/                генератор эталонных векторов (Node)
│   ├── rpc-smoke/             интеграционный тест сервера (Node, 14 сценариев)
│   ├── server/                установка сервера: PostgreSQL + PostgREST + Caddy, бэкапы, отзывы в Telegram
│   ├── site/                  генератор сайта: build.mjs, og.mjs, i18n/<lang>.mjs
│   └── deploy/                блок Caddy и выкладка сайта (publish.sh)
└── assets/brand/              исходники логотипов BotHub для карточек на сайте, лицензия шрифта
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

Идентификаторы: `applicationId` и `namespace` — **`org.ledor`**, пакеты кода — `org.ledor…`.
Версия — `versionCode` / `versionName` в `android/app/build.gradle.kts` (сейчас 8 / 0.5.1).
Подпись release: ключ загрузки из `android/keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`;
файл и `android/keystore/*.jks` не коммитятся). Без этого файла release подписывается отладочным ключом — только для
локальной проверки. `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` для Google Play.
Адрес сервера и публичный ключ берутся из `android/local.properties` (`supabase.url`, `supabase.key`; файл не коммитится,
образец — `local.properties.example`) и попадают в `BuildConfig`.

## 4. Пакеты

### `:domain` — `org.ledor.domain`

```
calendar/HebrewCalendar.kt   HebrewMonth, HebrewDate, CalendarKind, HebrewCalendarMath (единственное место с JewishDate)
calendar/Gematriya.kt        числа еврейскими буквами (для иврита и идиша)
event/Event.kt               Event (в т. ч. observeBoth), EventType, EventDate, GregorianParts, HebrewParts,
                             EventDefaults, EventValidator, AnniversaryRule, YahrzeitAdarPolicy
event/Occurrences.kt         AnniversaryCalculator, Occurrence, OccurrenceCalculator, MitzvahCalculator
reminder/Reminders.kt        City, CityCatalog, SunsetProvider, CitySunsetProvider (единственное место с AstronomicalCalendar),
                             DefaultSunsetProvider, SunsetHints (подсказка «до или после захода»),
                             ReminderSettings, PlannedReminder, ReminderPlanner
lists/ListRules.kt           ListNameRules, PinRules, PinGenerator
resources/cities.json        143 города: названия ru/en/he, координаты, часовой пояс
```

### `:app` — `org.ledor`

```
AppContainer.kt        KalendarApp (Application, Configuration.Provider для WorkManager) и AppContainer —
                       ручное внедрение зависимостей (lazy-поля), без фреймворков
MainActivity.kt        единственная Activity: enableEdgeToEdge(), setContent { KalendarRoot() }

data/holiday/          HolidayEvents.kt — события встроенного списка праздников (вычисляются, в базе их нет); домен — `org.ledor.domain.holiday`, экран — `ui/holidays/`
data/local/            Database.kt (AppDatabase, ListEntity, EventEntity, ListDao, EventDao), Mappers.kt
data/prefs/            SettingsStore.kt (DataStore Preferences), AppSettings
data/remote/           SupabaseRpc.kt — клиент сервера на OkHttp (имя историческое: сервер свой), RpcResult, DTO
data/repo/             Repositories.kt — EventRepository, ListRepository, SyncEngine
reminders/             Reminders.kt — ReminderScheduler, AlarmGateway, ReminderReceiver, SystemEventsReceiver,
                       NotificationFactory, ReminderHealthChecker
sync/                  SyncWorker.kt — SyncWorker, SyncScheduler (раз в сутки)
ui/AppViewModel.kt     общее состояние: события, списки, настройки, «здоровье» напоминаний
ui/theme/              Theme.kt — цвета, типографика, формы, KalendarTheme
ui/components/         Components, EventRow, Graphics (LedorMark, Маген Давид), Logo (LogoBlock, Wordmark),
                       SunsetHint, AppIcons
ui/format/             DateFormatters (еврейские/григорианские даты, «через N дней», гематрия для he/yi)
ui/locale/             AppLocale — язык приложения
ui/navigation/         Navigation.kt — маршруты, KalendarRoot, нижняя панель
ui/splash/ calendar/ events/ editor/ detail/ converter/ lists/ settings/ feedback/ about/ more/ — экраны
```

Отдельная ViewModel есть у редактора (`EventEditorViewModel`); остальные экраны работают через `AppViewModel` и
состояние в самих экранах.

## 5. Слои и поток данных

```
 Compose-экран ──события UI──▶ ViewModel ──▶ Repository ──▶ Room (единственный источник истины для UI)
       ▲                           │              │
       └── StateFlow<UiState> ─────┘              ├──▶ SupabaseRpc → сервер (синхронизация — только через SyncEngine)
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

Поля события 1:1 с серверной таблицей (`title`, `type`, `custom_label`, `recurring`, `observe_by`, `input_calendar`,
`g_year`, `g_month`, `g_day`, `after_sunset`, `h_year`, `h_month`, `h_day`, `note`). `observe_by` — `HEBREW`,
`GREGORIAN` или `BOTH`; при `BOTH` в домене `observeBoth = true`, а основной календарь равен `input_calendar`
(отдельного столбца нет, миграция Room не понадобилась). Плюс:

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
| `calendarGridMode` | GREGORIAN/HEBREW | HEBREW |
| `yahrzeitAdarPolicy` | ADAR_I/ADAR_II | ADAR_I |
| `lastSyncAttemptAt` | Long | 0 |
| `pendingNotices` | String (JSON-массив) | `[]` — очередь внутренних сообщений («список удалён владельцем») |
| `lastListId` | String | последний список, выбранный в редакторе |

Язык приложения хранится не здесь: на Android 13+ — в системе (`LocaleManager`), на 8–12 — в `SharedPreferences`
`app_locale` (нужен синхронно в `attachBaseContext`).

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
                     ├─▶ Feedback(prefill?)      также из «О программе» и «Пожаловаться на список»
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

| Уровень | Инструмент | Что покрывает сейчас |
|---|---|---|
| `:domain` | JUnit 4 + эталонные векторы | `GoldenVectorsTest` — все строки `spec/golden`; `DomainTest` — наступления, «по обоим календарям», бар/бат-мицва, проверка ввода, города, заход солнца, подсказка про сумерки, планировщик, гематрия |
| `:app` | Robolectric + Roborazzi | `ScreensSmokeTest` — сквозной проход по всем экранам с тестовыми данными (три общих списка), по классу на язык: ru, ru-тёмная, en, he, yi, fr, es; снимки — `app/build/screens/<язык>/` |
| Сервер | Node (`tools/rpc-smoke`) | 14 сценариев на живом сервере |
| Сайт | Node (`website/tests`) | JS-порт календаря на тех же эталонных векторах |

Каждый класс снимков идёт в отдельном JVM (`unitTests.all { forkEvery = 1 }`): DataStore и WorkManager — синглтоны процесса.
Исполнитель **обязан открыть PNG и посмотреть глазами**. Отдельных тестов синхронизации, DAO и планировщика
будильников на уровне `:app` нет — это известный пробел.

Команда полной проверки (должна проходить после каждого изменения):

```bash
cd android && ./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:recordRoborazziDebug        # записать снимки экранов
```

## 11. Локализация

- Шесть языков: `values/` — английский (язык по умолчанию), `values-ru/`, `values-iw/` (иврит), `values-ji/` (идиш),
  `values-fr/`, `values-es/`. Иврит и идиш — старыми кодами `iw`/`ji`: так их сопоставляет Android.
  he, yi, fr, es переведены машинно и ждут вычитки носителем.
- Вёрстка RTL-совместимая (`start/end`, `android:supportsRtl="true"`); на иврите и идише всё зеркалится.
- Язык выбирается в «Настройках» (`AppLocale`): Android 13+ — системный язык приложения (`LocaleManager`,
  `android:localeConfig`), Android 8–12 — `attachBaseContext` в `Application`, `MainActivity` и при показе уведомлений.
- В каждом языке есть строка `locale_tag` (en, ru, he, yi, fr, es): по ней выбираются форматтеры дат, название города
  (ru / he / en) и адрес сайта (`https://kalendar.bothub.chat/<locale_tag>/`).
- Названия еврейских месяцев — массивы ресурсов; для русского **две формы**: именительный («Нисан 5787» — заголовки)
  и родительный («28 Нисана» — даты); на иврите «родительная» форма — с предлогом («בניסן»).
  Григорианские даты — `DateTimeFormatter` с локалью из `locale_tag` (`d MMMM yyyy`, `LLLL yyyy` для заголовков).
- На иврите и идише (`bools.xml`: `hebrew_letters = true`) числа еврейских дат пишутся буквами: `כ״ח בניסן תש״ן`.
- Множественное число («через 3 дня», «исполняется 37 лет») — только через `plurals`.

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
