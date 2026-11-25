# 03. Календарная логика (модуль `:domain`)

Это самая ответственная часть проекта. Ошибка здесь = человек пропустит йорцайт.
Правило работы: **сначала тесты на эталонных векторах, потом код**. Ничего не «вспоминать по памяти» —
всё проверяется файлами `spec/golden/*.json` (получены из независимой реализации Hebcal).

Модуль `:domain` — чистый Kotlin/JVM, без Android-зависимостей. Единственная внешняя библиотека —
`com.kosherjava:zmanim:3.1.0`.

---

## 1. Термины

| Термин | Значение |
|---|---|
| Гражданская (григорианская) дата | `java.time.LocalDate`. Все григорианские даты — по новому стилю (пролептический григорианский календарь). |
| Еврейская дата | год, месяц, день еврейского календаря. Еврейские сутки начинаются с заходом солнца **накануне** гражданской даты. |
| Дневная дата | гражданская дата, на которую приходится **светлая часть** еврейского дня. Вся конвертация «еврейская ↔ григорианская» в проекте оперирует дневной датой. |
| После захода | признак «событие произошло вечером после захода солнца» → еврейская дата берётся от следующих гражданских суток. |
| Отмечать по (observeBy) | по какому календарю событие повторяется каждый год: `HEBREW` или `GREGORIAN`. |

Пример: 23 апреля 1990, до захода → 28 Нисана 5750. 23 апреля 1990, после захода → 29 Нисана 5750.

---

## 2. KosherJava 3.1.0 — шпаргалка по API

> ⚠️ В версии 3.0 (октябрь 2026) библиотека перешла на `java.time` и удалила старое API на
> `java.util.Date/Calendar`. Примеры из интернета и из памяти модели почти наверняка относятся к 2.5.0 и
> **не скомпилируются**. Использовать только сигнатуры ниже (сверены с исходниками 3.1.0).
> Если чего-то не хватает — смотреть исходники в `~/.gradle/caches/.../zmanim-3.1.0-sources.jar`, а не угадывать.

```kotlin
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import com.kosherjava.zmanim.AstronomicalCalendar
import com.kosherjava.zmanim.util.GeoLocation

// Константы месяцев (нумерация от Нисана):
// NISSAN=1 IYAR=2 SIVAN=3 TAMMUZ=4 AV=5 ELUL=6 TISHREI=7 CHESHVAN=8 KISLEV=9 TEVES=10 SHEVAT=11 ADAR=12 ADAR_II=13
// В високосном году 12 = Адар I, 13 = Адар II. В простом году 12 = Адар, месяца 13 нет.

JewishDate(localDate: LocalDate)                 // григорианская -> еврейская (годы до н.э. -> IllegalArgumentException)
JewishDate(year: Int, month: Int, day: Int)      // еврейская; СТРОГАЯ валидация: несуществующая дата -> IllegalArgumentException
jd.jewishYear / jd.jewishMonth / jd.jewishDayOfMonth
jd.localDate: LocalDate                          // дневная гражданская дата
jd.dayOfWeek: Int                                // 1 = воскресенье … 7 = суббота
jd.isJewishLeapYear                              // + static JewishDate.isJewishLeapYear(year)
jd.daysInJewishMonth                             // только у экземпляра (статический метод — private)
jd.isCheshvanLong / jd.isKislevShort             // только у экземпляра
JewishDate.getDaysInJewishYear(year): Int        // static: 353/354/355 (простой), 383/384/385 (високосный)

// Заход солнца:
val geo = GeoLocation("Jerusalem", 31.778, 35.235, ZoneId.of("Asia/Jerusalem"))   // высота = 0
val cal = AstronomicalCalendar(geo).apply { localDate = date }
val sunset: Instant? = cal.seaLevelSunset        // null, если захода нет (полярный день/ночь)
```

Вспомогательные формулы (так же считает сама библиотека):
- длинный Хешван (30 дней) ⇔ `getDaysInJewishYear(y) % 10 == 5`;
- короткий Кислев (29 дней) ⇔ `getDaysInJewishYear(y) % 10 == 3`;
- число дней в месяце: `JewishDate(y, m, 1).daysInJewishMonth`.

`JewishDate` изменяемый (mutable) и не потокобезопасный — наружу из `HebrewCalendarMath` не отдавать,
создавать локально внутри функций.

**Ловушка №1.** Номер месяца 1 = Нисан, но год начинается с Тишрея (7). Внутри года 5787 Тишрей 5787
(сентябрь 2026) идёт **раньше** Нисана 5787 (апрель 2027). Никогда не сравнивать еврейские даты как
`(year, month, day)` по номеру месяца — только через `LocalDate` или через порядок месяцев из §3.

---

## 3. Модель данных

Пакет `org.ledor.domain.calendar`.

```kotlin
/** 14 значений. Строковые имена — контракт: так хранится в БД, на сервере, на iOS и на сайте. */
enum class HebrewMonth { NISAN, IYAR, SIVAN, TAMMUZ, AV, ELUL, TISHREI, CHESHVAN, KISLEV, TEVET, SHEVAT,
                         ADAR,      // Адар простого (невисокосного) года
                         ADAR_I,    // Адар I високосного года
                         ADAR_II }  // Адар II високосного года

data class HebrewDate(val year: Int, val month: HebrewMonth, val day: Int)   // всегда валидная полная дата

enum class CalendarKind { HEBREW, GREGORIAN }
```

Зачем 14 значений, а не номер 1..13: год события может быть неизвестен («день рождения 10 Адара, год не помню»),
а без года номер 12 неоднозначен. Строковый код однозначен всегда и одинаков на всех платформах.

Соответствие KosherJava:

| HebrewMonth | номер KosherJava | допустим в году |
|---|---|---|
| NISAN…SHEVAT | 1…11 | любом |
| ADAR | 12 | только простом |
| ADAR_I | 12 | только високосном |
| ADAR_II | 13 | только високосном |

Обратное преобразование: `12 → if (isLeap(year)) ADAR_I else ADAR`, `13 → ADAR_II`.

Порядок месяцев внутри года (для списков выбора и сетки календаря):
`TISHREI, CHESHVAN, KISLEV, TEVET, SHEVAT, [ADAR | ADAR_I, ADAR_II], NISAN, IYAR, SIVAN, TAMMUZ, AV, ELUL`.

Максимально возможный день месяца, когда год неизвестен:
`CHESHVAN 30, KISLEV 30, ADAR 29, ADAR_I 30, ADAR_II 29, NISAN 30, IYAR 29, SIVAN 30, TAMMUZ 29, AV 30, ELUL 29, TISHREI 30, TEVET 29, SHEVAT 30`.

### `HebrewCalendarMath` — единственное место, где используется KosherJava для дат

```kotlin
object HebrewCalendarMath {
    fun isLeapYear(year: Int): Boolean
    fun daysInYear(year: Int): Int
    fun daysInMonth(year: Int, month: HebrewMonth): Int          // require(month допустим в этом году)
    fun isCheshvanLong(year: Int): Boolean
    fun isKislevShort(year: Int): Boolean
    fun monthsOfYear(year: Int): List<HebrewMonth>               // 12 или 13, в порядке от Тишрея
    fun isValid(year: Int, month: HebrewMonth, day: Int): Boolean
    fun fromGregorian(date: LocalDate): HebrewDate               // по дневной дате
    fun fromGregorian(date: LocalDate, afterSunset: Boolean): HebrewDate   // afterSunset -> date.plusDays(1)
    fun toGregorian(date: HebrewDate): LocalDate                 // дневная дата
    fun hebrewYearOf(date: LocalDate): Int
}
```

Поддерживаемый диапазон (валидируется на входе, вне диапазона — ошибка валидации, а не падение):
григорианские годы **1600…2200**, еврейские годы **5360…5960**.

---

## 4. Модель события

Пакет `org.ledor.domain.event`.

```kotlin
enum class EventType { BIRTHDAY, YAHRZEIT, BAR_MITZVAH, BAT_MITZVAH, WEDDING, OTHER }

data class GregorianParts(val year: Int?, val month: Int, val day: Int)        // month 1..12
data class HebrewParts(val year: Int?, val month: HebrewMonth, val day: Int)

data class EventDate(
    val inputCalendar: CalendarKind,     // в каком календаре пользователь ВВЁЛ дату
    val gregorian: GregorianParts?,
    val afterSunset: Boolean,            // имеет смысл только при inputCalendar = GREGORIAN и известном годе
    val hebrew: HebrewParts?,
)

data class Event(
    val id: String,                      // UUID v4, lowercase, генерируется на клиенте
    val listId: String,
    val title: String,                   // имя человека или название, 1..120 символов после trim
    val type: EventType,
    val customLabel: String?,            // только для OTHER, 1..60
    val recurring: Boolean,              // true = каждый год, false = одноразовое
    val observeBy: CalendarKind,         // по какому календарю повторять (при observeBoth — основной)
    val date: EventDate,
    val note: String?,                   // до 500 символов
    val observeBoth: Boolean = false,    // отмечать и по второму календарю тоже
)
```

### Инварианты (`EventValidator`, проверяются при сохранении и покрыты тестами)

| № | Правило |
|---|---|
| I1 | `inputCalendar = GREGORIAN` ⇒ `gregorian != null`. Если `gregorian.year != null` ⇒ `hebrew` = полная дата `fromGregorian(date, afterSunset)`. Если `gregorian.year == null` ⇒ `hebrew = null`, `afterSunset = false`, `observeBy = GREGORIAN`. |
| I2 | `inputCalendar = HEBREW` ⇒ `hebrew != null`, `afterSunset = false`. Если `hebrew.year != null` ⇒ `gregorian` = полная дата `toGregorian(hebrew)`. Если `hebrew.year == null` ⇒ `gregorian = null`, `observeBy = HEBREW`. |
| I3 | Одноразовое событие (`recurring = false`) требует известный год; `observeBy = inputCalendar`. |
| I4 | Дата существует: с годом — `isValid(...)` / реальная `LocalDate`; без года — день ≤ максимума из таблицы §3 (григорианское 29 февраля без года допустимо). |
| I5 | `customLabel` непуст ⇔ `type = OTHER`. |
| I6 | Годы в поддерживаемом диапазоне (§3). |
| I7 | `observeBoth = true` допустимо только для ежегодного события с известным годом (нужны обе полные даты); иначе ошибка `OBSERVE_NEEDS_YEAR`. Основной календарь при этом — `inputCalendar`. В БД и API пара (`observeBy`, `observeBoth = true`) хранится одним значением `observe_by = 'BOTH'`. |

Вторая (производная) дата хранится рядом с введённой, чтобы все платформы показывали одно и то же и чтобы
списки сортировались без пересчёта. Вычисляет её всегда тот, кто пишет событие (фабрика `EventDate.fromGregorian(...)` /
`EventDate.fromHebrew(...)`); вручную поля не собирать.

### Значения по умолчанию по типу события

| Тип | recurring | observeBy | Правило годовщины |
|---|---|---|---|
| BIRTHDAY | true | HEBREW | BIRTHDAY |
| YAHRZEIT | true | HEBREW | YAHRZEIT |
| BAR_MITZVAH | false | HEBREW | BIRTHDAY |
| BAT_MITZVAH | false | HEBREW | BIRTHDAY |
| WEDDING | true | HEBREW | BIRTHDAY |
| OTHER | true | HEBREW | BIRTHDAY |

`observeBy` принудительно `GREGORIAN`, если введена григорианская дата без года (I1), и принудительно `HEBREW`,
если введена еврейская дата без года (I2).

---

## 5. Правила ежегодной даты по еврейскому календарю

`AnniversaryCalculator.hebrewAnniversary(rule, origin: HebrewParts, targetYear: Int, adarPolicy): HebrewDate`

```kotlin
enum class AnniversaryRule { BIRTHDAY, YAHRZEIT }
enum class YahrzeitAdarPolicy { ADAR_I /* ашкеназский обычай, по умолчанию */, ADAR_II /* сефардский */ }
```

Обозначения: `Y` — целевой год, `leap(Y)` — високосный, `O` — исходная дата (месяц `O.m`, день `O.d`, год `O.y` может быть `null`).

### 5.1. Правило BIRTHDAY (день рождения, свадьба, бар/бат-мицва, «другое»)

Проверять по порядку, первое сработавшее:

| # | Условие | Результат в году Y |
|---|---|---|
| B1 | `O.m ∈ {ADAR, ADAR_II}` | тот же день в **последнем Адаре**: `leap(Y) ? ADAR_II : ADAR` |
| B2 | `O.m = ADAR_I`, `O.d = 30`, год Y простой | **1 NISAN** (в Адаре простого года 29 дней) |
| B3 | `O.m = ADAR_I` | `leap(Y) ? ADAR_I : ADAR`, тот же день |
| B4 | `O.m = CHESHVAN`, `O.d = 30`, Хешван в Y короткий | **1 KISLEV** |
| B5 | `O.m = KISLEV`, `O.d = 30`, Кислев в Y короткий | **1 TEVET** |
| B6 | иначе | тот же месяц и день |

### 5.2. Правило YAHRZEIT (годовщина смерти)

Шаг 1 — выбрать базовую дату (первое сработавшее):

| # | Условие | Базовая дата в году Y |
|---|---|---|
| Y1 | `O.m = CHESHVAN`, `O.d = 30`, `O.y` известен и Хешван в году `O.y + 1` **короткий** | **последний день Хешвана** года Y (29 или 30) → готово, шаг 2 не нужен |
| Y2 | `O.m = KISLEV`, `O.d = 30`, `O.y` известен и Кислев в году `O.y + 1` **короткий** | **последний день Кислева** года Y (29 или 30) → готово |
| Y3 | `O.m = ADAR_II` | тот же день в последнем Адаре: `leap(Y) ? ADAR_II : ADAR` |
| Y4 | `O.m = ADAR_I`, `O.d = 30`, год Y простой | **30 SHEVAT** |
| Y5 | `O.m = ADAR_I` | `leap(Y) ? ADAR_I : ADAR`, тот же день |
| Y6 | `O.m = ADAR` (смерть в Адаре простого года) | год Y простой → `ADAR`; високосный → по `adarPolicy`: `ADAR_I` (умолчание) или `ADAR_II` |
| Y7 | иначе | тот же месяц и день |

Шаг 2 — если базовая дата не существует в году Y:
- 30 CHESHVAN при коротком Хешване → **1 KISLEV**;
- 30 KISLEV при коротком Кислеве → **1 TEVET**.

Пояснение к Y1/Y2 (классическое правило): если на первую годовщину 30-го числа не было, йорцайт навсегда
закрепляется за последним днём месяца; если было — отмечается 30-го, а в короткие годы 1-го числа следующего месяца.
Если год смерти неизвестен (`O.y = null`), Y1/Y2 не применяются (работает ветка Y7 + шаг 2) — см. решение D-12.

В год самой смерти йорцайта нет: правило вызывается только для `Y > O.y`.

### 5.3. Проверочные примеры (из Hebcal, все есть в `spec/golden/anniversaries.json`)

| Правило | Исходная дата | Целевой год | Результат | Дневная дата |
|---|---|---|---|---|
| BIRTHDAY | 30 ADAR_I 5784 | 5785 (простой) | 1 NISAN 5785 | 2025-03-30 |
| BIRTHDAY | 30 ADAR_I 5784 | 5787 (високосный) | 30 ADAR_I 5787 | 2027-03-09 |
| BIRTHDAY | 10 ADAR 5785 | 5787 | 10 ADAR_II 5787 | 2027-03-19 |
| YAHRZEIT | 10 ADAR 5785 | 5787 | 10 ADAR_I 5787 | 2027-02-17 |
| BIRTHDAY | 10 ADAR_II 5784 | 5785 | 10 ADAR 5785 | 2025-03-10 |
| YAHRZEIT | 10 ADAR_II 5784 | 5787 | 10 ADAR_II 5787 | 2027-03-19 |
| YAHRZEIT | 30 ADAR_I 5784 | 5785 | 30 SHEVAT 5785 | 2025-02-28 |
| BIRTHDAY | 30 CHESHVAN 5785 | 5786 (Хешван 29) | 1 KISLEV 5786 | 2025-11-21 |
| YAHRZEIT | 30 CHESHVAN 5785 | 5786 (Хешван 29) | 29 CHESHVAN 5786 | 2025-11-20 |
| YAHRZEIT | 30 CHESHVAN 5785 | 5787 (Хешван 30) | 30 CHESHVAN 5787 | 2026-11-10 |
| YAHRZEIT | 30 CHESHVAN 5779 | 5780 (Хешван 30) | 30 CHESHVAN 5780 | 2019-11-28 |
| YAHRZEIT | 30 CHESHVAN 5779 | 5781 (Хешван 29) | 1 KISLEV 5781 | 2020-11-17 |

Контрольные конвертации: `1948-05-14 = 5 IYAR 5708`, `1990-04-23 = 28 NISAN 5750`, `2000-01-01 = 23 TEVET 5760`,
`2024-02-29 = 20 ADAR_I 5784`, `2026-09-12 = 1 TISHREI 5787`, `2026-11-24 = 14 KISLEV 5787`, `2027-05-05 = 28 NISAN 5787`.

Свойства ближайших лет: 5784 — високосный (383 дня), 5785 — 355, 5786 — 354, 5787 — високосный (385), 5788 — 355,
5789 — 354, 5790 — високосный (383).

---

## 6. Ежегодная дата по григорианскому календарю

`gregorianAnniversary(origin: GregorianParts, targetYear: Int): LocalDate` — тот же месяц и день;
**29 февраля в невисокосный год → 28 февраля** (решение D-11).

---

## 7. Наступления событий (`OccurrenceCalculator`)

```kotlin
data class Occurrence(
    val eventId: String,
    val date: LocalDate,          // дневная гражданская дата наступления
    val hebrewDate: HebrewDate,   // fromGregorian(date)
    val yearsSince: Int?,         // сколько лет исполняется; null, если исходный год неизвестен
    val startsEveBefore: Boolean, // true для наступления по еврейскому календарю: день начинается накануне вечером
) { val calendar: CalendarKind }  // по какому календарю это наступление (HEBREW, если startsEveBefore)

object OccurrenceCalculator {
    fun inHebrewYear(event: Event, year: Int, policy: YahrzeitAdarPolicy): Occurrence?
    fun inGregorianYear(event: Event, year: Int): Occurrence?
    fun next(event: Event, today: LocalDate, policy: YahrzeitAdarPolicy): Occurrence?            // первое с date >= today
    fun between(event: Event, from: LocalDate, toInclusive: LocalDate, policy: YahrzeitAdarPolicy): List<Occurrence>
}
```

Правила:

1. **Одноразовое событие**: одно наступление — `date.gregorian` (полная по I3), `yearsSince = 0`. Если дата в прошлом — `next()` возвращает `null`.
2. **Ежегодное, observeBy = HEBREW**: для целевого еврейского года `Y` дата = `toGregorian(hebrewAnniversary(rule, hebrew, Y, policy))`.
   `yearsSince = Y − hebrew.year` (если год известен).
3. **Ежегодное, observeBy = GREGORIAN**: `gregorianAnniversary(gregorian, gy)`, `yearsSince = gy − gregorian.year`.
4. **С какого года начинаются наступления** (если исходный год известен):
   - BIRTHDAY и YAHRZEIT — с `originYear + 1` (сам день рождения/смерти наступлением не считается);
   - WEDDING, BAR_MITZVAH, BAT_MITZVAH, OTHER — с `originYear` (`yearsSince = 0` — само событие).
   Если год неизвестен — наступления есть в каждом году.
5. `next()` для HEBREW: перебрать `Y ∈ {hebrewYearOf(today), +1, +2}`, взять минимальную дату `≥ today`.
   Для GREGORIAN: `gy ∈ {today.year, today.year + 1}`.
6. `between()`: перебрать все годы, пересекающиеся с диапазоном (еврейские — от `hebrewYearOf(from)` до `hebrewYearOf(to)`), отфильтровать по диапазону, отсортировать.
7. **Ежегодное, `observeBoth = true`** (и обе даты с годом): наступления считаются по обоим календарям и объединяются.
   Если в каком-то году обе даты выпали на один день — остаётся одно наступление (по основному календарю).
   `next()` — ближайшее из двух рядов. В списке и календаре у такого события две строки в году, напоминания — для каждой.

«Через сколько дней» = `ChronoUnit.DAYS.between(today, occurrence.date)`; `today` — гражданская дата устройства.

### Бар-мицва / Бат-мицва (`MitzvahCalculator`)

```kotlin
data class MitzvahHint(val type: EventType /* BAR_MITZVAH | BAT_MITZVAH */, val hebrewDate: HebrewDate, val date: LocalDate)
fun mitzvahHints(birth: HebrewParts /* с годом */, today: LocalDate): List<MitzvahHint>
```

- Бар-мицва = `hebrewAnniversary(BIRTHDAY, birth, birth.year + 13)`; Бат-мицва = `… birth.year + 12`.
- Возвращать только подсказки с `date >= today` (для взрослых подсказок нет). Пол не хранится — показываются обе строки.
- Пример: рождение 2013-03-10 (28 ADAR 5773) → Бар-мицва 28 ADAR 5786 = 2026-03-17.

---

## 8. Заход солнца и города

Пакет `org.ledor.domain.reminder`.

```kotlin
data class City(val id: String, val nameRu: String, val nameEn: String, val nameHe: String,
                val countryCode: String, val lat: Double, val lon: Double, val zoneId: String)

interface SunsetProvider { fun sunset(date: LocalDate): Instant }

class CitySunsetProvider(city: City) : SunsetProvider {          // KosherJava, seaLevelSunset
    fun sunsetOrNull(date: LocalDate): Instant?                  // null — захода в этот день нет
    fun nightfallOrNull(date: LocalDate): Instant?               // выход звёзд: солнце на 8,5° ниже горизонта
}
class DefaultSunsetProvider(zone: ZoneId) : SunsetProvider       // 18:00 в зоне устройства
```

- Нет города → заход = 18:00 в часовом поясе устройства.
- `seaLevelSunset == null` (полярные широты) → 18:00 в часовом поясе города.
- Геолокация устройства не используется нигде и никогда (ни разрешений, ни кода).

Файл городов: `domain/src/main/resources/cities.json` — массив объектов `City` (`id` вида `il-jerusalem`).
Точность координат ±0.05° достаточна (ошибка захода < 30 секунд). Обязательный состав (~150 городов):

- **Израиль:** Иерусалим, Тель-Авив, Хайфа, Беэр-Шева, Ашдод, Ашкелон, Нетания, Ришон-ле-Цион, Петах-Тиква, Бней-Брак, Холон, Бат-Ям, Реховот, Модиин, Бейт-Шемеш, Цфат, Тверия, Эйлат, Ноф-ха-Галиль, Кирьят-Шмона, Хадера, Кфар-Саба, Раанана, Герцлия, Афула, Кармиэль, Нагария, Арад, Димона.
- **Россия:** Москва, Санкт-Петербург, Новосибирск, Екатеринбург, Казань, Нижний Новгород, Самара, Ростов-на-Дону, Краснодар, Сочи, Уфа, Челябинск, Омск, Красноярск, Пермь, Воронеж, Волгоград, Саратов, Иркутск, Хабаровск, Владивосток, Биробиджан, Калининград, Дербент, Махачкала, Нальчик, Пятигорск.
- **Украина:** Киев, Харьков, Одесса, Днепр, Львов, Запорожье, Умань, Житомир, Черновцы, Николаев.
- **Беларусь:** Минск, Гомель, Витебск, Могилёв, Брест, Гродно, Бобруйск.
- **Балтия, Кавказ, Центральная Азия, Молдова:** Рига, Вильнюс, Таллин, Кишинёв, Тбилиси, Баку, Губа, Ереван, Алматы, Астана, Ташкент, Самарканд, Бухара, Бишкек, Душанбе.
- **Европа:** Лондон, Манчестер, Париж, Марсель, Берлин, Франкфурт, Мюнхен, Вена, Цюрих, Амстердам, Антверпен, Брюссель, Рим, Милан, Мадрид, Барселона, Прага, Будапешт, Варшава, Стокгольм, Стамбул, Афины, Лимасол.
- **Америка:** Нью-Йорк, Лейквуд (Нью-Джерси), Лос-Анджелес, Майами, Чикаго, Бостон, Филадельфия, Балтимор, Вашингтон, Хьюстон, Даллас, Атланта, Детройт, Кливленд, Денвер, Сан-Франциско, Сиэтл, Лас-Вегас, Торонто, Монреаль, Ванкувер, Мехико, Буэнос-Айрес, Сан-Паулу.
- **Остальной мир:** Йоханнесбург, Кейптаун, Мельбурн, Сидней, Дубай, Бангкок, Сингапур, Гонконг.

Тесты на файл: все `zoneId` парсятся `ZoneId.of`, широта/долгота в допустимых пределах, `id` уникальны,
названия на трёх языках непустые. Контроль расчёта захода (допуск ±6 минут, местное время):

| Город | 21 июня 2026 | 21 декабря 2026 |
|---|---|---|
| Иерусалим | ≈ 19:47 | ≈ 16:40 |
| Москва | ≈ 21:17 | ≈ 15:58 |
| Нью-Йорк | ≈ 20:30 | ≈ 16:32 |

### Подсказка «до или после захода» (`SunsetHints`)

Помогает правильно выставить `afterSunset`, когда известны местное время и город события.

```kotlin
enum class SunsetVerdict { BEFORE, TWILIGHT, AFTER }
data class SunsetHint(val verdict: SunsetVerdict, val sunset: LocalTime, val nightfall: LocalTime)
object SunsetHints { fun evaluate(date: LocalDate, time: LocalTime, city: City): SunsetHint? }
```

- Момент события — `date` + `time` в часовом поясе города (**исторические** правила пояса: Москва, апрель 1990 → UTC+4).
- До захода → `BEFORE`; от захода до выхода звёзд → `TWILIGHT` (дата под сомнением — интерфейс ничего не выставляет
  и советует спросить раввина); позже → `AFTER`.
- Выход звёзд — 8,5° под горизонтом; если солнце так низко не опускается (белые ночи) — заход + 72 минуты.
- Сначала проверяются сумерки **вчерашнего** вечера: время после полуночи, но до вчерашнего выхода звёзд — тоже `TWILIGHT`.
- Захода в этот день нет (полярный день) → `null`.
- Контроль (тест `sunsetHint`): Иерусалим 21.12.2026 — 15:00 `BEFORE`, 16:50 `TWILIGHT`, 18:00 `AFTER`, 01:30 `BEFORE`,
  выход звёзд ≈ 17:18; Москва 23.04.1990 19:40 — `BEFORE`, заход ≈ 20:50; Санкт-Петербург 21.06.2026 23:00 — `TWILIGHT`
  (72 минуты), 22.06 00:30 — `BEFORE`.

---

## 9. Планировщик напоминаний (`ReminderPlanner`) — чистая функция

Общая для Android и будущего iOS. Системные будильники сюда не входят (см. `05-reminders-battery.md`).

```kotlin
enum class ReminderKind { EVE /* накануне вечером */, DAY /* в день события */ }
enum class EveMode { BEFORE_SUNSET, FIXED_TIME }

data class ReminderSettings(
    val eveEnabled: Boolean = true,
    val eveMode: EveMode = EveMode.BEFORE_SUNSET,
    val eveMinutesBeforeSunset: Int = 180,          // 0..360, шаг 30
    val eveFixedTime: LocalTime = LocalTime.of(15, 0),
    val dayEnabled: Boolean = true,
    val dayTime: LocalTime = LocalTime.of(12, 0),
)

data class PlannedReminder(val eventId: String, val occurrenceDate: LocalDate, val kind: ReminderKind,
                           val triggerAt: Instant, val yearsSince: Int?,
                           val calendar: CalendarKind)   // календарь наступления — от него зависит текст уведомления

object ReminderPlanner {
    /** Все напоминания с triggerAt в полуинтервале (fromExclusive, toInclusive], по возрастанию triggerAt. */
    fun plan(events: List<Event>, settings: ReminderSettings, sunset: SunsetProvider, zone: ZoneId,
             policy: YahrzeitAdarPolicy, fromExclusive: Instant, toInclusive: Instant): List<PlannedReminder>
}
```

Для каждого наступления с дневной датой `D`:

| Напоминание | Момент срабатывания |
|---|---|
| EVE, режим `BEFORE_SUNSET` | `sunset.sunset(D − 1) − eveMinutesBeforeSunset` |
| EVE, режим `FIXED_TIME` | `(D − 1)` в `eveFixedTime` в зоне устройства |
| DAY | `D` в `dayTime` в зоне устройства |

- Расписание одинаково для наступлений по еврейскому и по григорианскому календарю (различается только текст уведомления).
  У события «по обоим календарям» напоминания планируются для каждой из двух дат.
- События из списков с выключенными уведомлениями вызывающая сторона отфильтровывает **до** вызова `plan`.
- Диапазон наступлений для перебора: `[fromDate − 1 день, toDate + 2 дня]`, затем фильтр по `triggerAt`.
- Локальное время, попавшее в «дыру» перехода на летнее время, разрешается стандартно через `ZonedDateTime.of(...)`.

Тесты планировщика — с фиксированными событиями и «заглушкой» `SunsetProvider`: умолчания (15:00 накануне и 12:00 в день
при закате 18:00), оба режима EVE, выключенные напоминания, границы полуинтервала, сортировка, событие 1-го числа
(накануне = предыдущий месяц/год), високосные годы.

---

## 10. Числа буквами (`Gematriya`)

Для иврита и идиша день и год еврейской даты пишутся буквами: `Gematriya.format(28) = "כ״ח"`, `format(5787) = "תשפ״ז"`.
Тысячи года не пишутся; 15 и 16 — `ט״ו`, `ט״ז`; одна буква — с гершем (`א׳`, `ל׳`); последняя буква — в конечной форме
(`תש״ן`). Та же функция есть в JS-порте сайта (`gematriya` в `website/assets/js/hebrew-calendar.js`).

---

## 11. Эталонные векторы `spec/golden/`

Генератор: `tools/golden/generate.mjs` (Node 22, `npm ci && node generate.mjs`). Файлы закоммичены, руками не править.
Формат каждого файла: `{ description, source, columns: [...], rows: [[...], ...] }`.

| Файл | Строк | Что проверять |
|---|---|---|
| `conversions.json` | 1040 | `fromGregorian(g) == (hy, hm, hd)` **и** `toGregorian(hy, hm, hd) == g`; 5-я колонка — номер месяца KosherJava (проверка маппинга) |
| `years.json` | 201 | `isLeapYear`, `daysInYear`, длины Хешвана и Кислева, дата 1 Тишрея |
| `anniversaries.json` | 3724 | `hebrewAnniversary(kind, origin, targetYear, ADAR_I)` — месяц, день и дневная дата |
| `holidays.json` | 704 | `Holidays.inHebrewYear(year, il)` — первый день и число дней каждого праздника, 5785–5800, Израиль и диаспора (§12) |
| `candles.json` | 414 | `CandleLightingCalculator.forDay(eve + 1, city)` — местное время, допуск 2 минуты; 7 городов, 2027 год (§12) |

Два последних файла создаёт `node tools/golden/holidays.mjs` из открытого API Hebcal (нужен интернет); строки — объекты, а не массивы.

Подключение в `:domain`: `sourceSets.test.resources.srcDir("../../spec/golden")`, чтение через `kotlinx-serialization-json`
(`JsonArray`), параметризованный перебор всех строк. Те же файлы использует тест JS-конвертера сайта
(`website/tests/`) и должны использовать тесты iOS.

Чего **нет** в векторах (покрыть обычными модульными тестами): правило YAHRZEIT с политикой `ADAR_II`,
даты без года, григорианские годовщины (29 февраля), `OccurrenceCalculator` (включая «по обоим календарям»),
`MitzvahCalculator`, `ReminderPlanner`, `SunsetHints`, `Gematriya`, `EventValidator` — см. `DomainTest.kt`.

## 12. Праздники и зажигание свечей (`org.ledor.domain.holiday`)

Встроенный список «Еврейские праздники» (`01` §3.13). Имена `Holiday` — контракт (векторы, iOS).
`HolidayOccurrence(holiday, start, days)`: `start` — первый день (гражданская дата), праздник начинается накануне вечером.

| `Holiday` | Дата | Дней | Вид | Свечи |
|---|---|---|---|---|
| `ROSH_HASHANA` | 1 Тишрея | 2 | праздник | да |
| `TZOM_GEDALIAH` | 3 Тишрея; в субботу → воскресенье | 1 | пост | |
| `YOM_KIPPUR` | 10 Тишрея | 1 | праздник | да |
| `SUKKOT` | 15 Тишрея | 7 | праздник | да |
| `SHMINI_ATZERET` (и Симхат Тора) | 22 Тишрея | 1 в Израиле, 2 в диаспоре | праздник | да |
| `CHANUKAH` | 25 Кислева | 8 | праздник | |
| `ASARA_BETEVET` | 10 Тевета | 1 | пост | |
| `TU_BISHVAT` | 15 Швата | 1 | праздник | |
| `TAANIT_ESTHER` | 13 Адара (в високосный год — Адара II); в субботу → четверг | 1 | пост | |
| `PURIM`, `SHUSHAN_PURIM` | 14 и 15 Адара (Адара II) | 1 | праздник | |
| `PESACH` | 15 Нисана | 7 в Израиле, 8 в диаспоре | праздник | да |
| `PESACH_VII` | 21 Нисана | 1 / 2 | праздник | да |
| `YOM_HASHOAH` | 27 Нисана; в пятницу → четверг, в воскресенье → понедельник | 1 | памятный день | |
| `YOM_HAATZMAUT` | 5 Ияра; в пятницу → четверг, в субботу → четверг, в понедельник → вторник | 1 | праздник | |
| `YOM_HAZIKARON` | день перед Йом ха-Ацмаут | 1 | памятный день | |
| `LAG_BAOMER` | 18 Ияра | 1 | праздник | |
| `YOM_YERUSHALAYIM` | 28 Ияра | 1 | праздник | |
| `SHAVUOT` | 6 Сивана | 1 / 2 | праздник | да |
| `TZOM_TAMMUZ` | 17 Таммуза; в субботу → воскресенье | 1 | пост | |
| `TISHA_BAV` | 9 Ава; в субботу → воскресенье | 1 | пост | |
| `TU_BAV` | 15 Ава | 1 | праздник | |

- `Holiday.dawn` — четыре малых поста (`TZOM_GEDALIAH`, `ASARA_BETEVET`, `TAANIT_ESTHER`, `TZOM_TAMMUZ`) начинаются на рассвете
  дня поста и длятся до выхода звёзд; всё остальное, включая Йом Кипур и Девятое Ава, начинается накануне вечером.
- `Holidays.inHebrewYear(year, inIsrael)`, `Holidays.between(from, to, inIsrael)` (праздники, хотя бы один день которых
  попадает в отрезок), `Holidays.shabbats(from, to)`.
- Израиль или диаспора: город из настроек с `countryCode == "IL"`; без города — часовой пояс телефона `Asia/Jerusalem`.
- **Зажигание свечей** — `CandleLightingCalculator.forDay(day, city)`, считается для кануна дня `day` (Шабата или первого
  дня праздника со свечами):
  - канун не суббота — заход солнца (на уровне моря) минус 18 минут; Иерусалим (`il-jerusalem`) — 40, Хайфа (`il-haifa`) — 30;
    время округляется вниз до минуты;
  - канун — суббота — после выхода звёзд (8,5° под горизонтом; если так не темнеет — заход + 72 минуты), округление вверх;
    `afterNightfall = true`, в текстах — «не раньше»;
  - нет захода (полярный день) — `null`, время не показывается.
- Вторые вечера праздников (свечи зажигают от существующего огня после выхода звёзд) приложение не показывает.
- Правила проверены на `holidays.json` и `candles.json` (тест `HolidaysTest`).


## 13. Первый год после смерти и Изкор (B-26)

`MourningCalculator.firstYear(death, policy)` — вехи по возрастанию даты, считаются от еврейской даты смерти:

| `MourningMilestone` | Правило |
|---|---|
| `SHLOSHIM` | 30-й день, считая день смерти первым: гражданская дата смерти + 29 дней |
| `ELEVEN_MONTHS` | то же число через 11 еврейских месяцев подряд (`addMonths`); если такого дня в месяце нет — последний день месяца |
| `TWELVE_MONTHS` | то же через 12 месяцев; в високосный год это на месяц раньше йорцайта. Если дата совпадает с первым йорцайтом — вехи нет |
| `FIRST_YAHRZEIT` | `hebrewAnniversary(YAHRZEIT, death, год + 1, policy)` |

Упрощения (написаны пользователю в карточке): шлошим по закону считается от похорон и может быть сокращён праздником —
приложение этого не учитывает; 11 и 12 месяцев относятся к трауру по родителям; шива не считается.
Эталонных векторов для вех нет — проверяются модульным тестом `mourningFirstYear` на примерах; арифметика месяцев стоит на
проверенных векторами `nextMonth`, `daysInMonth`, `toGregorian`.

**Изкор** — `Holidays.yizkorDay(occurrence)`: Йом Кипур и Шмини Ацерет — первый день; `PESACH_VII` и `SHAVUOT` — последний
день (в Израиле он же первый). Тест `yizkorDays`.
