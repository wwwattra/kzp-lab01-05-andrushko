# Звіт до лабораторної роботи №3

з дисципліни «Кросплатформні засоби програмування», тема «Спадкування, інтерфейси й поліморфізм у Java-проєкті». Варіант 2. Спортивні тренування.
Виконав: студент групи КІ-302 Андрушко А. І. Прийняв: Олексів М. В.

## Тема, номер і варіант

Лабораторна робота № 3 «Спадкування, інтерфейси й поліморфізм у Java-проєкті». Варіант 2 – «Спортивні тренування». ОС – Windows 10 (64-bit), JDK Temurin 21.0.12, Maven 3.9.11 через Wrapper. Версія продукту – 1.2.0.

Посилання на GitHub-репозиторій: https://github.com/[ваш-логін]/kzp-lab-andrushko, гілка lab03, Git-тег v1.2.0.

## Мета роботи

Розширити класову модель: винести спільні поля й інваріанти до абстрактного базового типу Training, створити підтипи IndoorTraining і OutdoorTraining з різною реалізацією поліморфної операції load() (розрахунок навантаження), додати enum TrainingKind, реалізувати узгоджені equals/hashCode та обробляти різнорідні об'єкти через спільний тип без розгалуження за підтипом.

## Стан до і після цієї роботи

*Таблиця 1 – Що додано до моделі ЛР2*

| Ознака | ЛР2 | ЛР3 |
|---|---|---|
| Запис | об'єкт final-класу Training | об'єкт одного з підтипів |
| Спільні поля | у класі-сутності | в абстрактному базовому класі |
| Вид тренування | довільний String | enum TrainingKind (5 значень) |
| Відмінна поведінка | відсутня | перевизначений load() |
| Рівність | посилальна (Object) | узгоджені equals/hashCode |
| Звіт | 4 показники | 4 показники без змін + розділ навантаження |

## Базовий тип

Training – абстрактний клас (обрано саме клас, а не інтерфейс, бо підтипи мають спільний стан і готову реалізацію геттерів, рівності та toString). Поля: date, kind (TrainingKind), minutes, distanceKm, calories – private final. Захищений конструктор перевіряє спільні інваріанти (null, від'ємні, нескінченна дистанція) у статичному методі validateNumbers ще до входу в тіло конструктора. Абстрактний метод double load() – поліморфна операція варіанта «розрахунок навантаження». Фабрика create/fromCsv обирає підтип один раз – на межі створення об'єкта.

*Лістинг 1 – src/main/java/ua/lpnu/kzp/Training.java*

```text
package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/**
 * Спільний абстрактний тип тренування.
 *
 * <p>Базовий клас зберігає спільні поля та інваріанти, а підтипи реалізують
 * поліморфну операцію {@link #load()} — розрахунок навантаження.
 */
public abstract class Training {
    /** Кількість полів у CSV-рядку. */
    public static final int FIELD_COUNT = 5;

    private final LocalDate date;
    private final TrainingKind kind;
    private final int minutes;
    private final double distanceKm;
    private final int calories;

    /**
     * Створює тренування з перевіреним спільним станом.
     *
     * @param date дата тренування
     * @param kind вид тренування
     * @param minutes тривалість, хв, не від'ємна
     * @param distanceKm дистанція, км, не від'ємна й скінченна
     * @param calories витрата калорій, не від'ємна
     */
    protected Training(LocalDate date, TrainingKind kind, int minutes, double distanceKm, int calories) {
        // Перевірка виконується до входу в тіло конструктора, тому частково створений
        // об'єкт не виникає (виправлення SpotBugs CT_CONSTRUCTOR_THROW).
        this(Objects.requireNonNull(date, "Дата не може бути null"),
                Objects.requireNonNull(kind, "Вид не може бути null"),
                validateNumbers(minutes, distanceKm, calories), distanceKm, calories, true);
    }

    /* Присвоює вже перевірений стан під час внутрішнього делегування. */
    private Training(LocalDate date, TrainingKind kind, int minutes, double distanceKm, int calories,
            boolean validated) {
        this.date = date;
        this.kind = kind;
        this.minutes = minutes;
        this.distanceKm = distanceKm;
        this.calories = calories;
    }

    /* Перевіряє числові інваріанти та повертає перевірену тривалість. */
    private static int validateNumbers(int minutes, double distanceKm, int calories) {
        if (minutes < 0 || distanceKm < 0 || calories < 0) {
            throw new IllegalArgumentException("від'ємне числове значення");
        }
        if (!Double.isFinite(distanceKm)) {
            throw new IllegalArgumentException("дистанція має бути скінченним числом");
        }
        return minutes;
    }

    /**
     * Створює тренування потрібного підтипу за підписом виду.
     *
     * <p>Вибір підтипу виконується один раз на межі створення об'єкта; далі код працює
     * лише через спільний тип.
     *
     * @param date дата
     * @param kindLabel вид тренування з CSV
     * @param minutes тривалість, хв
     * @param distanceKm дистанція, км
     * @param calories калорії
     * @return {@link OutdoorTraining} або {@link IndoorTraining}
     */
    public static Training create(LocalDate date, String kindLabel, int minutes,
            double distanceKm, int calories) {
        Objects.requireNonNull(kindLabel, "Вид не може бути null");
        if (kindLabel.isBlank()) {
            throw new IllegalArgumentException("порожня дата або вид тренування");
        }
        TrainingKind kind = TrainingKind.fromLabel(kindLabel);
        return kind.isOutdoor()
                ? new OutdoorTraining(date, kind, minutes, distanceKm, calories)
                : new IndoorTraining(date, kind, minutes, distanceKm, calories);
    }

    /**
     * Створює тренування з CSV-рядка {@code date;kind;minutes;distanceKm;calories}.
     *
     * @param line рядок CSV
     * @return тренування відповідного підтипу
     * @throws IllegalArgumentException якщо структура, типи або значення некоректні
     */
    public static Training fromCsv(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        String[] fields = line.split(";", -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "очікується %d полів, отримано %d".formatted(FIELD_COUNT, fields.length));
        }
        if (fields[0].isBlank() || fields[1].isBlank()) {
            throw new IllegalArgumentException("порожня дата або вид тренування");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(fields[0].trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("дата має формат YYYY-MM-DD", exception);
        }
        try {
            return create(date, fields[1].trim(),
                    Integer.parseInt(fields[2].trim()),
                    Double.parseDouble(fields[3].trim()),
                    Integer.parseInt(fields[4].trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("числове поле має помилковий формат", exception);
        }
    }

    /**
     * Обчислює умовне навантаження тренування в балах.
     *
     * @return навантаження; формула залежить від підтипу
     */
    public abstract double load();

    /** @return дата тренування */
    public final LocalDate getDate() {
        return date;
    }

    /** @return підпис виду (сумісність із ЛР2) */
    public final String getKind() {
        return kind.label();
    }

    /** @return вид тренування як enum */
    public final TrainingKind getTrainingKind() {
        return kind;
    }

    /** @return тривалість у хвилинах */
    public final int getMinutes() {
        return minutes;
    }

    /** @return дистанція в кілометрах */
    public final double getDistanceKm() {
        return distanceKm;
    }

    /** @return витрата калорій */
    public final int getCalories() {
        return calories;
    }

    /** Порівнює тренування за підтипом і всіма полями. */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Training training = (Training) other;
        return minutes == training.minutes
                && Double.compare(distanceKm, training.distanceKm) == 0
                && calories == training.calories
                && date.equals(training.date)
                && kind == training.kind;
    }

    /** Повертає хеш-код, узгоджений із {@link #equals(Object)}. */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), date, kind, minutes, distanceKm, calories);
    }

    /** Повертає текстове подання тренування для звіту. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s: %d хв, %.2f км, %d ккал",
                date, kind.label(), minutes, distanceKm, calories);
    }
}
```

## Підтипи

*Таблиця 2 – Підтипи та формули load()*

| Підтип | Види | Власне правило | load(), бали |
|---|---|---|---|
| OutdoorTraining | біг, велосипед, ходьба | дистанція > 0 | хв + 3·км + ккал/10 |
| IndoorTraining | плавання, тренажери | дистанція може бути 0 | 1.2·хв + ккал/10 |

*Лістинг 2 – src/main/java/ua/lpnu/kzp/OutdoorTraining.java*

```text
package ua.lpnu.kzp;

import java.time.LocalDate;

/** Тренування на відкритому повітрі: біг, велосипед, ходьба. */
public final class OutdoorTraining extends Training {
    /** Бали навантаження за один кілометр дистанції. */
    public static final double POINTS_PER_KM = 3.0;

    /**
     * Створює тренування на відкритому повітрі.
     *
     * @param date дата
     * @param kind вид, для якого {@link TrainingKind#isOutdoor()} істинне
     * @param minutes тривалість, хв
     * @param distanceKm дистанція, км; має бути додатною
     * @param calories калорії
     */
    public OutdoorTraining(LocalDate date, TrainingKind kind, int minutes, double distanceKm, int calories) {
        // Спочатку базовий клас перевіряє спільний стан, потім підтип - власне правило.
        super(date, kind, minutes, distanceKm, calories);
        if (!kind.isOutdoor()) {
            throw new IllegalArgumentException("вид %s не є тренуванням надворі".formatted(kind.label()));
        }
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("дистанція тренування надворі має бути додатною");
        }
    }

    /** Навантаження = хвилини + 3 бали за км + калорії / 10. */
    @Override
    public double load() {
        return getMinutes() + getDistanceKm() * POINTS_PER_KM + getCalories() / 10.0;
    }
}
```

*Лістинг 3 – src/main/java/ua/lpnu/kzp/IndoorTraining.java*

```text
package ua.lpnu.kzp;

import java.time.LocalDate;

/** Тренування в приміщенні: плавання, тренажери. */
public final class IndoorTraining extends Training {
    /** Коефіцієнт інтенсивності для тренувань у приміщенні. */
    public static final double INTENSITY = 1.2;

    /**
     * Створює тренування в приміщенні.
     *
     * @param date дата
     * @param kind вид, для якого {@link TrainingKind#isOutdoor()} хибне
     * @param minutes тривалість, хв
     * @param distanceKm дистанція, км; для тренажерів допускається 0
     * @param calories калорії
     */
    public IndoorTraining(LocalDate date, TrainingKind kind, int minutes, double distanceKm, int calories) {
        super(date, kind, minutes, distanceKm, calories);
        if (kind.isOutdoor()) {
            throw new IllegalArgumentException("вид %s не є тренуванням у приміщенні".formatted(kind.label()));
        }
    }

    /** Навантаження = хвилини · 1.2 + калорії / 10 (дистанція не враховується). */
    @Override
    public double load() {
        return getMinutes() * INTENSITY + getCalories() / 10.0;
    }
}
```

## enum

TrainingKind має константи RUNNING, CYCLING, WALKING, SWIMMING, GYM з полями label (підпис у CSV і звіті) та outdoor (місце проведення). Метод fromLabel перетворює текст із CSV на константу й відхиляє невідомий вид. Значення порівнюються через ==, бо кожна константа існує в одному екземплярі. Звіт групує навантаження в EnumMap, тому порядок видів стабільний (порядок оголошення констант).

*Лістинг 4 – src/main/java/ua/lpnu/kzp/TrainingKind.java*

```text
package ua.lpnu.kzp;

/** Фіксований набір видів тренувань із підписом для звіту та місцем проведення. */
public enum TrainingKind {
    /** Біг на відкритому повітрі. */
    RUNNING("біг", true),
    /** Велосипедна поїздка. */
    CYCLING("велосипед", true),
    /** Піша прогулянка. */
    WALKING("ходьба", true),
    /** Плавання в басейні. */
    SWIMMING("плавання", false),
    /** Заняття на тренажерах у залі. */
    GYM("тренажери", false);

    private final String label;
    private final boolean outdoor;

    /* Конструктор enum використовується JVM для створення фіксованих констант. */
    TrainingKind(String label, boolean outdoor) {
        this.label = label;
        this.outdoor = outdoor;
    }

    /** @return текстове подання виду, яке зберігається у CSV і звіті */
    public String label() {
        return label;
    }

    /** @return {@code true}, якщо тренування проходить на відкритому повітрі */
    public boolean isOutdoor() {
        return outdoor;
    }

    /**
     * Знаходить вид за підписом із CSV.
     *
     * @param label підпис, наприклад {@code біг}
     * @return відповідна константа
     * @throws IllegalArgumentException якщо такого виду немає
     */
    public static TrainingKind fromLabel(String label) {
        for (TrainingKind kind : values()) {
            if (kind.label.equals(label)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("невідомий вид тренування: %s".formatted(label));
    }
}
```

## equals і hashCode

Критерій рівності – той самий підтип (getClass()) і всі п'ять полів (дистанція порівнюється через Double.compare). hashCode будується з тих самих полів і класу через Objects.hash, тому контракт «рівні об'єкти – рівні хеш-коди» виконується. Обидва методи final, щоб підтипи не порушили симетрію. Поля незмінні, тому об'єкт безпечно зберігати в HashSet і використовувати ключем HashMap (перевірено тестами).

## Поліморфізм

TrainingProcessor.totalLoad і formatLoadSection працюють зі списком List<Training> і викликають training.load() без instanceof чи switch: реалізацію обирає JVM за фактичним підтипом. Додавання нового підтипу (наприклад, TeamTraining) не потребує змін у коді звіту. Спадкування доречне, бо кожен підтип «є різновидом» тренування; звіт же лише містить список тренувань, тому для нього обрано композицію, а не extends ArrayList.

*Лістинг 5 – TrainingProcessor.java (поліморфні методи)*

```text

    /**
     * Обчислює сумарне навантаження через поліморфний метод без перевірки підтипу.
     *
     * @param trainings тренування різних підтипів
     * @return сума {@link Training#load()}
     */
    public static double totalLoad(List<Training> trainings) {
        double total = 0.0;
        for (Training training : trainings) {
            // Реалізацію load() обирає JVM за фактичним підтипом об'єкта.
            total += training.load();
        }
        return total;
    }

    /**
     * Формує розділ звіту про навантаження за видами та унікальні записи.
     *
     * @param trainings коректні тренування
     * @return текст розділу
     */
    public static String formatLoadSection(List<Training> trainings) {
        Map<TrainingKind, Double> byKind = new EnumMap<>(TrainingKind.class);
        for (Training training : trainings) {
            byKind.merge(training.getTrainingKind(), training.load(), Double::sum);
        }
        StringBuilder section = new StringBuilder();
        section.append(String.format(Locale.ROOT, "Навантаження (поліморфний розрахунок load), бали%n"));
        for (Map.Entry<TrainingKind, Double> entry : byKind.entrySet()) {
            section.append(String.format(Locale.ROOT, "  %-32s %10.2f%n",
                    entry.getKey().label() + (entry.getKey().isOutdoor() ? " (надворі)" : " (у приміщенні)"),
                    entry.getValue()));
        }
        section.append(String.format(Locale.ROOT, "  %-32s %10.2f%n", "Усього", totalLoad(trainings)));
        section.append(String.format(Locale.ROOT, "Унікальних тренувань (HashSet): %d%n",
                new HashSet<>(trainings).size()));
        return section.toString();
    }
```

## Тестування

Перенесено 22 тести ЛР1–ЛР2 і додано 9 тестів TrainingHierarchyTest. Єдина обґрунтована зміна старих тестів – виклик new Training(...) замінено на Training.create(...), бо базовий клас став абстрактним; перевірки й очікування не змінено. Нові тести: вибір підтипу фабрикою; різні формули load(); власне правило підтипу (ходьба з 0 км); чужий вид для підтипу; відхилення неправильного спільного стану базовим класом; пошук enum і невідомий вид; рівність, однаковий hashCode і один елемент у HashSet; ключ HashMap; поліморфна сума через базовий тип.

*Лістинг 6 – src/test/java/ua/lpnu/kzp/TrainingHierarchyTest.java*

```text
package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrainingHierarchyTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 8);

    /* Перевіряє, що фабрика обирає підтип за видом. */
    @Test
    void factoryChoosesSubtypeByKind() {
        assertInstanceOf(OutdoorTraining.class, Training.create(DATE, "біг", 45, 8.5, 520));
        assertInstanceOf(IndoorTraining.class, Training.create(DATE, "плавання", 40, 1.5, 410));
    }

    /* Перевіряє формули поліморфного методу кожного підтипу. */
    @Test
    void subtypesCalculateLoadDifferently() {
        Training outdoor = Training.create(DATE, "біг", 60, 10.0, 600);
        Training indoor = Training.create(DATE, "тренажери", 60, 0.0, 600);
        assertEquals(60 + 30 + 60, outdoor.load(), 0.0001);
        assertEquals(72 + 60, indoor.load(), 0.0001);
        assertNotEquals(outdoor.load(), indoor.load());
    }

    /* Перевіряє власне правило підтипу: надворі дистанція має бути додатною. */
    @Test
    void outdoorRejectsZeroDistance() {
        assertThrows(IllegalArgumentException.class, () -> Training.create(DATE, "ходьба", 30, 0.0, 100));
    }

    /* Перевіряє, що підтип не приймає вид іншої категорії. */
    @Test
    void subtypeRejectsForeignKind() {
        assertThrows(IllegalArgumentException.class,
                () -> new IndoorTraining(DATE, TrainingKind.RUNNING, 30, 5.0, 300));
        assertThrows(IllegalArgumentException.class,
                () -> new OutdoorTraining(DATE, TrainingKind.GYM, 30, 1.0, 300));
    }

    /* Перевіряє, що базовий тип відхиляє неправильний спільний стан. */
    @Test
    void baseTypeRejectsInvalidCommonState() {
        assertThrows(IllegalArgumentException.class, () -> Training.create(DATE, "плавання", -5, 1.0, 100));
        assertThrows(NullPointerException.class,
                () -> new IndoorTraining(DATE, null, 30, 1.0, 300));
    }

    /* Перевіряє пошук enum за підписом і невідомий вид. */
    @Test
    void enumFindsByLabel() {
        assertEquals(TrainingKind.CYCLING, TrainingKind.fromLabel("велосипед"));
        assertTrue(TrainingKind.CYCLING.isOutdoor());
        assertThrows(IllegalArgumentException.class, () -> TrainingKind.fromLabel("йога"));
    }

    /* Перевіряє, що рівні об'єкти мають однаковий hashCode і не дублюються в HashSet. */
    @Test
    void equalObjectsHaveOneSetEntry() {
        Training first = Training.create(DATE, "біг", 45, 8.5, 520);
        Training second = Training.fromCsv("2026-09-08;біг;45;8.50;520");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        Set<Training> set = new HashSet<>(List.of(first, second));
        assertEquals(1, set.size());
    }

    /* Перевіряє, що логічно рівний об'єкт знаходиться як ключ HashMap. */
    @Test
    void equalObjectWorksAsMapKey() {
        Map<Training, String> notes = new HashMap<>();
        notes.put(Training.create(DATE, "плавання", 40, 1.5, 410), "басейн");
        assertEquals("басейн", notes.get(Training.create(DATE, "плавання", 40, 1.5, 410)));
    }

    /* Перевіряє, що поліморфна сума не залежить від порядку і не перевіряє підтип. */
    @Test
    void polymorphicTotalUsesBaseType() {
        List<Training> trainings = List.of(
                Training.create(DATE, "біг", 60, 10.0, 600),
                Training.create(DATE, "тренажери", 60, 0.0, 600));
        assertEquals(150 + 132, TrainingProcessor.totalLoad(trainings), 0.0001);
    }
}
```

*Лістинг 7 – Результат mvnw.cmd -B clean verify*

```text
[INFO] Building kzp-lab-andrushko 1.2.0
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.084 s -- in ua.lpnu.kzp.TrainingHierarchyTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.096 s -- in ua.lpnu.kzp.TrainingProcessorTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.057 s -- in ua.lpnu.kzp.TrainingTest
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: C:\Users\user\kzp-labs\lab03\target\kzp-lab-andrushko-1.2.0.jar
[INFO] BugInstance size is 0
[INFO] No errors/warnings found
[INFO] BUILD SUCCESS
```

## Інфраструктура

Під час першого verify SpotBugs знайшов дефект CT_CONSTRUCTOR_THROW: конструктор незавершеного (не final) класу Training кидав виняток із тіла, залишаючи частково створений об'єкт (вразливість до Finalizer-атаки). Його оформлено як bug-Issue та виправлено за схемою з методичних вказівок: перевірка виконується у виразах аргументів делегованого виклику this(…) до входу в тіло конструктора. Повторний verify – BUILD SUCCESS, «No errors/warnings found». CI: посилання на запуск GitHub Actions і артефакт – після push.

## GitHub Issues і Pull Request

*Таблиця 3 – План GitHub Issues → зміна → перевірка*

| Issue | Мітка | Зміна | Перевірка |
|---|---|---|---|
| Абстрактний базовий тип Training | feature | спільні поля, load() | baseTypeRejects… |
| Підтипи Indoor/Outdoor | feature | формули load(), власні правила | subtypesCalculate… |
| enum TrainingKind | feature | label, outdoor, fromLabel | enumFindsByLabel |
| equals/hashCode | feature | рівність за класом і полями | HashSet, HashMap |
| [bug] SpotBugs CT_CONSTRUCTOR_THROW | bug | перевірка до тіла конструктора | verify без помилок |
| Документація ЛР3 | docs | README, javadoc, REPORT | рев'ю PR |

## Порівняння поведінки

Перші 12 рядків звіту (показники й повідомлення про помилки) збігаються з ЛР1–ЛР2; після них додано розділ поліморфного навантаження. Перевірка вручну для бігу: (45+25.5+52) + (30+25.5+35) + (45+30+61) = 349.00 бала.

*Лістинг 8 – Вивід версії 1.2.0*

```text
Звіт: спортивні тренування
Показник                             Значення
Коректних записів                           8
Сумарний час, хв                          505
Сумарна дистанція, км                  108.60
Найбільша витрата калорій, ккал           820
Пропущено рядків: 5
Рядок 6: порожній рядок
Рядок 10: від'ємне числове значення
Рядок 11: порожня дата або вид тренування
Рядок 12: числове поле має помилковий формат
Рядок 13: очікується 5 полів, отримано 4
Навантаження (поліморфний розрахунок load), бали
  біг (надворі)                        349.00
  велосипед (надворі)                  589.90
  ходьба (надворі)                     125.40
  плавання (у приміщенні)               89.00
  тренажери (у приміщенні)             117.00
  Усього                              1270.30
Унікальних тренувань (HashSet): 8
Звіт записано у out\report.txt
```

## Академічна доброчесність

Заповніть самостійно: використані інструменти ШІ або твердження, що вони не використовувалися; ролі, запити, прийняті й відхилені рекомендації; власний внесок і підтвердження розуміння всього поданого коду.

## Відповіді на контрольні питання

1. Яку спільну проблему розв'язує базовий тип у предметній області?

Спільні поля й перевірки всіх тренувань описано один раз, а код звіту працює з одним типом Training незалежно від підтипу.

2. Чим відрізняються extends і implements?

extends успадковує клас (стан і реалізацію, лише один батько), implements зобов'язує клас реалізувати контракт інтерфейсу (інтерфейсів може бути кілька).

3. Навіщо базовий клас може бути abstract?

Щоб заборонити створення «тренування взагалі» і змусити кожен підтип реалізувати load().

4. Яку роль виконує виклик super(...) у конструкторі підтипу?

Спочатку ініціалізує й перевіряє спільну частину об'єкта в базовому класі, лише потім підтип перевіряє власне правило.

5. Що перевіряє анотація @Override?

Що метод справді перевизначає метод супертипу; помилка в назві чи параметрах стане помилкою компіляції.

6. Що таке поліморфізм і як він усуває розгалуження за типом?

Змінна базового типу посилається на об'єкт підтипу, а виклик load() виконує реалізацію фактичного об'єкта, тому if (x instanceof …) не потрібен.

7. Коли краще використати інтерфейс, а коли абстрактний клас?

Інтерфейс – для спільної можливості без стану або коли потрібно кілька контрактів; абстрактний клас – коли підтипи мають спільний стан і реалізацію, як у моєму Training.

8. Що означає відношення «є різновидом» і чим воно відрізняється від композиції?

is-a: біг є тренуванням, тому OutdoorTraining extends Training. has-a: звіт містить список тренувань, тож список – поле, а не батьківський клас.

9. Для чого використовують enum із полями та методами?

Для фіксованої множини значень із пов'язаними даними: TrainingKind зберігає підпис і ознаку «надворі», а fromLabel перетворює текст CSV.

10. Чому значення enum можна порівнювати через ==?

Кожна константа створюється JVM один раз, тому однакові значення – той самий об'єкт.

11. Які правила має виконувати пара equals/hashCode?

Рефлексивність, симетричність, транзитивність, узгодженість; якщо a.equals(b), то a.hashCode() == b.hashCode().

12. Чому змінне поле небезпечне для об'єкта, який є ключем HashMap?

Після зміни поля змінюється хеш-код, і об'єкт опиняється не в тому кошику – його вже неможливо знайти.

13. Чому два об'єкти різних підтипів можуть бути нерівними за однакової назви?

Бо критерій рівності включає getClass(): тренування різних категорій мають різну поведінку й не вважаються одним записом.

14. Як поліморфна колекція обробляє об'єкти різних підтипів?

Зберігає їх як List<Training> і викликає спільні методи; диспетчеризація відбувається під час виконання.

15. Які тести доводять, що підтипи мають різну поведінку?

subtypesCalculateLoadDifferently – однакові дані дають 150 і 132 бали.

16. Які тести доводять коректність рівності в HashSet?

equalObjectsHaveOneSetEntry (два рівні об'єкти – один елемент) і equalObjectWorksAsMapKey.

17. Що контролює sealed і навіщо потрібен permits?

sealed обмежує перелік безпосередніх підтипів, permits їх перелічує; компілятор тоді перевіряє вичерпність switch.

18. Чому switch за підтипами не повинен дублювати поліморфні методи?

Логіку довелося б підтримувати у двох місцях, і новий підтип легко забути в одному з них.

19. Які попередні тести та результати потрібно зберегти після перебудови?

Усі 22 тести ЛР1–ЛР2 і перші рядки звіту з чотирма показниками та повідомленнями про помилки.

20. Яке рішення у Вашій ієрархії найкраще демонструє перевагу поліморфізму?

totalLoad: один рядок training.load() у циклі коректно обробляє обидва підтипи й не зміниться, якщо додати третій.

## Висновки

Створено абстрактний тип Training, два підтипи з різними правилами й формулами load(), enum TrainingKind, узгоджені equals/hashCode. Звіт зберіг показники попередніх робіт і отримав поліморфний розділ навантаження. 31 тест проходить, знайдений SpotBugs дефект усунуто. Поліморфна модель стане джерелом для потокових запитів ЛР4.

