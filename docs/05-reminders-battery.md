# 05. Напоминания, фоновая работа и батарея (Android)

Цель: уведомления приходят вовремя, а приложение в фоне **не работает вообще** — оно просыпается
только в момент напоминания и один раз в сутки для синхронизации.

## 1. Жёсткие правила (нарушать нельзя)

| Запрещено | Почему |
|---|---|
| Foreground-сервисы, собственные wake lock'и | постоянный расход батареи |
| Любые разрешения на геолокацию | закат считается по выбранному городу |
| Firebase, FCM, Google Play Services, аналитика, crash-репортеры | фоновая сеть и трекинг |
| Realtime, WebSocket, длинные соединения | держат радио активным |
| `setRepeating` / `setInexactRepeating`, периодические задачи чаще раза в сутки | лишние пробуждения |
| Больше одного ожидающего будильника напоминаний | не нужно, см. §2 |

## 2. Схема «цепочка из одного будильника»

В системе всегда зарегистрирован **ровно один** будильник — на ближайшее напоминание.
Когда он срабатывает, приложение показывает уведомления и ставит следующий.

```
            ┌────────────── любое изменение: событие, список, настройки, синхронизация ─────────────┐
            ▼                                                                                        │
   ReminderScheduler.rearm()                                                                         │
     1. now, watermark ← SettingsStore.reminderWatermark   (при первом запуске = now)                │
     2. due ← ReminderPlanner.plan(fromExclusive = watermark, toInclusive = now)                     │
        показать уведомления для due (кроме устаревших, см. §2.2)                                    │
     3. watermark ← now                                                                              │
     4. next ← первое напоминание из plan(fromExclusive = now, toInclusive = now + 400 дней)         │
     5. next есть → AlarmManager: один точный будильник на next.triggerAt; нет → отменить будильник  │
            │                                                                                        │
            ▼ срабатывание                                                                           │
   ReminderReceiver.onReceive → goAsync() → rearm() ─────────────────────────────────────────────────┘
```

`ReminderPlanner` — чистая функция из `:domain` (`03-calendar-domain.md` §9). Планировщик не хранит список
запланированного: всё каждый раз вычисляется заново из базы, поэтому рассинхронизация невозможна.

### 2.1. Когда вызывается `rearm()`

| Триггер | Где |
|---|---|
| Сработал будильник | `ReminderReceiver` |
| Перезагрузка телефона (`BOOT_COMPLETED`) | `SystemEventsReceiver` |
| Обновление приложения (`MY_PACKAGE_REPLACED`) | `SystemEventsReceiver` |
| Смена времени или часового пояса (`TIME_SET`, `TIMEZONE_CHANGED`) | `SystemEventsReceiver` |
| Изменился доступ к точным будильникам (`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`) | `SystemEventsReceiver` |
| Запуск процесса приложения | `KalendarApp.onCreate` (асинхронно, не блокируя старт) |
| Изменились события/списки в Room | подписка в `AppContainer`, debounce 500 мс |
| Изменились настройки напоминаний или город | там же |
| Завершилась синхронизация | `SyncEngine` |

Вызовы сериализуются `Mutex`; работа идёт на `Dispatchers.Default`. В ресиверах — `goAsync()` и завершение
`PendingResult` в `finally`, общий лимит 9 секунд (`withTimeout`).

### 2.2. Догоняющие и устаревшие напоминания

Если телефон был выключен в момент срабатывания, напоминание попадёт в `due` при следующем `rearm()`:
- EVE (накануне) показывается, только если событие ещё не наступило: `today < occurrenceDate`;
- DAY (в день события) показывается, только если `today == occurrenceDate`;
- иначе молча пропускается.

`watermark` гарантирует, что одно напоминание не покажется дважды.

### 2.3. Постановка будильника

```kotlin
interface AlarmGateway {                       // абстракция для тестов
    fun canScheduleExact(): Boolean
    fun set(triggerAtMillis: Long, exact: Boolean)
    fun cancel()
}
```

- Точный: `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, …)`.
  Если `canScheduleExactAlarms() == false` → `setAndAllowWhileIdle(...)` (напоминание придёт, но может опоздать) —
  приложение не падает и не молчит.
- `PendingIntent.getBroadcast(context, 1001, Intent(context, ReminderReceiver::class.java), FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)`.
  Один и тот же `requestCode` → новый будильник заменяет старый.
- `setAlarmClock()` не использовать (рисует значок будильника в статус-баре).

## 3. Манифест

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- Android 13+: выдаётся автоматически; разрешено политикой Google Play для приложений-календарей -->
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<!-- Android 12–12L -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32" />
<!-- WorkManager приносит службу переднего плана; LeDor её не использует — удалена из манифеста -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" tools:node="remove" />
<service android:name="androidx.work.impl.foreground.SystemForegroundService" tools:node="remove" />

<receiver android:name=".reminders.ReminderReceiver" android:exported="false" />
<receiver android:name=".reminders.SystemEventsReceiver" android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        <action android:name="android.intent.action.TIME_SET" />
        <action android:name="android.intent.action.TIMEZONE_CHANGED" />
        <action android:name="android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" />
    </intent-filter>
</receiver>
```

`minSdk = 26` — каналы уведомлений и `java.time` доступны без условий и десугаринга.
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` убрано в 0.4.0: прямой запрос «не экономить батарею» нарушает правила Google Play.
Риск по `USE_EXACT_ALARM` и запасной вариант — в `08-store-compliance.md` §3.

## 4. Уведомления

Каналы (создаются в `KalendarApp.onCreate`, важность `IMPORTANCE_DEFAULT`):

| id | Название | Назначение |
|---|---|---|
| `reminders_eve` | Накануне вечером | первое напоминание |
| `reminders_day` | В день события | второе напоминание |

- `notificationId = (eventId + occurrenceDate + kind).hashCode()`; группа `reminders` + сводное уведомление, когда их ≥ 2.
- Нажатие → `MainActivity` с extra `eventId` → открывается карточка события.
- Маленькая иконка — монохромный `ic_stat_magen_david`.
- Нет разрешения на уведомления → просто не показывать (без исключений).

Тексты (строковые ресурсы; `{type}` — название типа, `{n}` — `yearsSince`, склонения через `plurals`):

| Случай | Заголовок | Текст |
|---|---|---|
| EVE, по еврейскому календарю | Сегодня вечером: {type} — {title} | С заходом солнца начинается {еврейская дата}. {возраст} |
| EVE, йорцайт | Сегодня вечером йорцайт: {title} | С заходом солнца начинается {еврейская дата}. Время зажечь поминальную свечу. {годовщина} |
| EVE, по григорианскому календарю | Завтра: {type} — {title} | {григорианская дата}. {возраст} |
| DAY | Сегодня: {type} — {title} | {еврейская дата} · {григорианская дата}. {возраст} |

Какой из двух текстов «накануне» показать, определяет календарь **наступления** (`PlannedReminder.calendar`), а не событие:
у события «по обоим календарям» еврейская дата получает «Сегодня вечером…», григорианская — «Завтра…».
Тексты — на языке приложения (на Android 8–12 контекст оборачивается `AppLocale.wrap`); на иврите и идише даты буквами.

`{возраст}`: день рождения — «Исполняется {n}»; свадьба — «{n} лет со дня свадьбы»; йорцайт — «{n}-я годовщина»;
при `n = null` или `n = 0` фраза опускается. Для `WEDDING` с `n > 0` тип называется «Годовщина свадьбы».

## 5. Проверка системных разрешений («здоровье напоминаний»)

```kotlin
enum class HealthIssue { NOTIFICATIONS_OFF, EXACT_ALARMS_DENIED, BACKGROUND_RESTRICTED, BATTERY_OPTIMIZED }

class ReminderHealthChecker(context: Context) {
    fun check(): List<HealthIssue>              // синхронно, дёшево; вызывать на каждом ON_RESUME
    fun fixIntent(issue: HealthIssue): Intent   // куда вести пользователя
}
```

| Проблема | Как определяется | Кнопка ведёт в |
|---|---|---|
| `NOTIFICATIONS_OFF` | API 33+: нет `POST_NOTIFICATIONS`; любой API: `!NotificationManagerCompat.areNotificationsEnabled()` или важность канала `IMPORTANCE_NONE` | сначала системный запрос разрешения (API 33+, если ещё можно спрашивать); иначе `Settings.ACTION_APP_NOTIFICATION_SETTINGS` |
| `EXACT_ALARMS_DENIED` | API 31+: `!alarmManager.canScheduleExactAlarms()` | `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` + `package:` URI |
| `BACKGROUND_RESTRICTED` | API 28+: `activityManager.isBackgroundRestricted` | `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` |
| `BATTERY_OPTIMIZED` | `!powerManager.isIgnoringBatteryOptimizations(packageName)` | `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (общий список) → карточка приложения; прямой запрос `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` не используется (правила Google Play) |

Запуск любого системного экрана — в `try/catch (ActivityNotFoundException)` с откатом на
`ACTION_APPLICATION_DETAILS_SETTINGS`.

### 5.1. Что видит пользователь

Баннер на главном экране (над календарём) показывается, когда: есть хотя бы одна проблема **и** включено хотя бы
одно напоминание **и** `healthWarningsEnabled = true`.

> **Напоминания могут не прийти**
> Телефон ограничивает работу приложения в фоне. Если не включить эту настройку, система заблокирует фоновую
> работу: вы просто не получите оповещения, и в напоминаниях не будет смысла.
> [Исправить] [Мне не нужны напоминания]

- **Исправить** → нижний лист со списком проблем; у каждой — пояснение и кнопка «Открыть настройку». После
  возврата в приложение список перепроверяется; исправленное исчезает; когда проблем нет — «Всё настроено ✓».
- **Мне не нужны напоминания** → подтверждение: «Приложение будет работать как записная книжка, без уведомлений.
  Включить проверку снова можно в Настройках → Надёжность напоминаний.» → `healthWarningsEnabled = false`, баннер больше не появляется.

Тексты проблем:

| Проблема | Заголовок | Пояснение |
|---|---|---|
| `NOTIFICATIONS_OFF` | Уведомления выключены | Разрешите приложению показывать уведомления — иначе напоминания не появятся. |
| `EXACT_ALARMS_DENIED` | Точные будильники запрещены | Без этого разрешения система может задержать напоминание на несколько часов. |
| `BACKGROUND_RESTRICTED` | Работа в фоне ограничена | Телефон полностью блокирует приложение в фоне. Снимите ограничение батареи. |
| `BATTERY_OPTIMIZED` | Включена экономия батареи для приложения | Выберите «Не ограничивать» (на Samsung — добавьте приложение в «Никогда не спящие»). Иначе телефон может усыпить приложение, и напоминания не придут. Приложение не работает в фоне и не расходует заряд — оно просыпается только в момент напоминания. |

Дополнительная подсказка по производителю (`Build.MANUFACTURER`: samsung, xiaomi/redmi/poco, huawei/honor, oppo/realme/oneplus, vivo) —
одна строка с путём в настройках этого производителя. Только текст, без попыток открыть фирменные экраны (их адреса
нестабильны). Для Samsung: «Настройки → Обслуживание устройства → Батарея → Ограничения в фоновом режиме → Никогда не спящие приложения».

В Настройках — раздел «Надёжность напоминаний»: те же четыре пункта со статусом ✓/⚠, переключатель
«Показывать предупреждения» и кнопка «Отправить тестовое уведомление» (сразу показывает пример уведомления).

## 6. Суточная синхронизация

```kotlin
PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED, requiresBatteryNotLow = true))
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
// enqueueUniquePeriodicWork("daily-sync", ExistingPeriodicWorkPolicy.KEEP, …)
```

- `SyncWorker` — `CoroutineWorker`: `SyncEngine.syncAll()` → `ReminderScheduler.rearm()`.
  Сетевая ошибка → `Result.retry()`; остальное → `Result.success()`.
- Ставится в очередь, только когда есть хотя бы один облачный список; при отключении последнего — отменяется.
- Разовая синхронизация после локальной правки: `OneTimeWorkRequest` с тем же условием сети,
  `enqueueUniqueWork("push-sync", ExistingWorkPolicy.REPLACE, …)`.
- Заодно это страховка для цепочки будильников: раз в сутки `rearm()` вызывается в любом случае.

## 7. Что нельзя починить из приложения (честно написать в FAQ на сайте)

- Принудительная остановка приложения («Остановить» в настройках) отменяет будильники до следующего запуска.
- Некоторые оболочки (MIUI, EMUI, ColorOS) убивают приложения агрессивнее стандартного Android — поэтому и
  существует баннер с инструкцией.

## 8. Тесты (JVM, Robolectric)

| Что | Как |
|---|---|
| `ReminderScheduler` | поддельные `AlarmGateway`, `Clock`, репозитории: ставится ровно один будильник на ближайшее напоминание; после срабатывания — следующий; догоняющие/устаревшие (§2.2); нет событий → `cancel()`; `exact = false`, когда точные запрещены |
| `ReminderHealthChecker` | тени Robolectric для `AlarmManager`, `PowerManager`, `NotificationManager`: каждая проблема по отдельности и все вместе |
| `SyncWorker` | `work-testing` (`TestListenableWorkerBuilder`) |
| Ресиверы | `SystemEventsReceiver` на каждое действие вызывает `rearm()` |

Ручная проверка на реальном телефоне — чек-лист в `PLAN.md` (раздел «Ручная приёмка»).

## 9. Праздники и Шабат (встроенный список, B-25)

- События списка «Еврейские праздники» в базе не хранятся: `HolidayEvents.build(...)` вычисляет их на 400 дней вперёд при
  каждом `rearm()` и отдаёт планировщику вместе с событиями пользователя. Отдельных будильников нет — та же цепочка из одного.
- Для них планируется **только напоминание накануне** (`EVE`), по времени из настроек напоминания накануне, даже если для
  событий оно выключено. В сам праздник и в Шабат телефон не беспокоит.
- Управление: «Напоминать накануне вечером» в списке праздников (и переключатель на карточке в «Списках»); Шабат — отдельный
  переключатель, работает только при выбранном городе.
- Изменение этих настроек и города запускает `rearm()` (подписка в `AppContainer`).
- Нажатие на уведомление открывает экран списка праздников.

- Вехи первого года (шлошим, 11 и 12 месяцев, B-26) вычисляются из йорцайтов тех списков, где уведомления включены, и
  планируются как обычные события: накануне вечером и в сам день.
- Если есть хотя бы один йорцайт, в напоминание накануне четырёх праздников добавляется день Изкора.
