# Звіт до лабораторної роботи №2

з дисципліни «Кросплатформні засоби програмування», тема «Класи, record і інкапсуляція в Java-проєкті». Варіант 2. Спортивні тренування.
Виконав: студент групи КІ-302 Андрушко А. І. Прийняв: Олексів М. В.

## Тема, номер і варіант

Лабораторна робота № 2 «Класи, record і інкапсуляція в Java-проєкті». Варіант 2 – «Спортивні тренування» (незмінний з ЛР1). ОС – Windows 10 (64-bit), JDK Temurin 21.0.12, Maven 3.9.11 через Wrapper. Версія продукту – 1.1.0.

Посилання на GitHub-репозиторій: https://github.com/wwwattra/kzp-lab01-05-andrushko. Гілка lab02, Pull Request #16 (https://github.com/wwwattra/kzp-lab01-05-andrushko/pull/16), Git-тег v1.1.0 (https://github.com/wwwattra/kzp-lab01-05-andrushko/tree/v1.1.0).

## Мета роботи

Перебудувати предметну частину продукту ЛР1: замість рядків, масивів і паралельних змінних використати клас-сутність Training з інкапсульованим незмінним станом, перевірку інваріантів у конструкторі, статичний фабричний метод fromCsv, record DurationDistance для підсумку та тести, які підтверджують збереження поведінки. Інфраструктура ЛР1 (Wrapper, JUnit, SpotBugs, Shade, CI) успадкована без змін.

## Стан до і після цієї роботи

*Таблиця 1 – Що замінено*

| Елемент | ЛР1 | ЛР2 |
|---|---|---|
| Один запис | рядок і fields[2], fields[3]… | об'єкт Training |
| Перевірка | метод validate з умовами над масивом | конструктор + Training.fromCsv |
| Набір даних | List<String> коректних рядків | List<Training> |
| Підсумок | масив double[4] з індексами | record DurationDistance |
| Дата | текст без розбору | LocalDate (YYYY-MM-DD) |
| Звіт | formatReport(double[], …) | formatReport(DurationDistance, …) – той самий текст |

Методи ЛР1 (validate, splitValid, computeStats, formatReport(double[])) збережено як тонкий фасад над новими класами, тому 10 тестів ЛР1 проходять без жодної зміни.

## Клас-сутність

Training – final-клас із полями private final: LocalDate date, String kind, int minutes, double distanceKm, int calories. Сеттерів немає: тренування після запису в журнал не змінюється, тому незмінний об'єкт виключає появу неперевірених значень. Інваріанти (з таблиці варіантів): дата й вид не порожні (Objects.requireNonNull для null, isBlank для порожнього тексту); хвилини, дистанція й калорії >= 0; дистанція скінченна (Double.isFinite відкидає NaN та нескінченність). Методи читання – getDate, getKind, getMinutes, getDistanceKm, getCalories. toString форматує запис через Locale.ROOT: 2026-09-01 біг: 45 хв, 8.50 км, 520 ккал.

*Лістинг 1 – src/main/java/ua/lpnu/kzp/Training.java*

```text
package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/** Описує незмінний запис про одне спортивне тренування. */
public final class Training {
    /** Кількість полів у CSV-рядку. */
    public static final int FIELD_COUNT = 5;

    private final LocalDate date;
    private final String kind;
    private final int minutes;
    private final double distanceKm;
    private final int calories;

    /**
     * Створює коректне тренування після перевірки інваріантів.
     *
     * @param date дата тренування
     * @param kind вид тренування, не порожній
     * @param minutes тривалість у хвилинах, не від'ємна
     * @param distanceKm дистанція в кілометрах, не від'ємна й скінченна
     * @param calories витрата калорій, не від'ємна
     * @throws IllegalArgumentException якщо порушено інваріант
     */
    public Training(LocalDate date, String kind, int minutes, double distanceKm, int calories) {
        this.date = Objects.requireNonNull(date, "Дата не може бути null");
        this.kind = Objects.requireNonNull(kind, "Вид не може бути null");
        if (kind.isBlank()) {
            throw new IllegalArgumentException("порожня дата або вид тренування");
        }
        if (minutes < 0 || distanceKm < 0 || calories < 0) {
            throw new IllegalArgumentException("від'ємне числове значення");
        }
        if (!Double.isFinite(distanceKm)) {
            throw new IllegalArgumentException("дистанція має бути скінченним числом");
        }
        this.minutes = minutes;
        this.distanceKm = distanceKm;
        this.calories = calories;
    }

    /**
     * Створює тренування з CSV-рядка {@code date;kind;minutes;distanceKm;calories}.
     *
     * @param line рядок CSV
     * @return створене тренування
     * @throws IllegalArgumentException якщо структура, типи або значення некоректні
     */
    public static Training fromCsv(String line) {
        // Фабрика відповідає за структуру й типи, конструктор - за інваріанти.
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
            return new Training(date, fields[1].trim(),
                    Integer.parseInt(fields[2].trim()),
                    Double.parseDouble(fields[3].trim()),
                    Integer.parseInt(fields[4].trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("числове поле має помилковий формат", exception);
        }
    }

    /** @return дата тренування */
    public LocalDate getDate() {
        return date;
    }

    /** @return вид тренування */
    public String getKind() {
        return kind;
    }

    /** @return тривалість у хвилинах */
    public int getMinutes() {
        return minutes;
    }

    /** @return дистанція в кілометрах */
    public double getDistanceKm() {
        return distanceKm;
    }

    /** @return витрата калорій */
    public int getCalories() {
        return calories;
    }

    /** Повертає текстове подання тренування для звіту. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s: %d хв, %.2f км, %d ккал",
                date, kind, minutes, distanceKm, calories);
    }
}
```

## Фабричний метод і record

Training.fromCsv(String) відповідає за структуру й типи: порожній рядок, кількість полів (split(";", -1)), порожні дата/вид, формат дати (LocalDate.parse), числа (NumberFormatException перетворюється на IllegalArgumentException із причиною). Інваріанти перевіряє конструктор, тому обійти їх іншим способом створення неможливо. Номер рядка додається на рівні циклу читання TrainingProcessor.parseAll.

Record DurationDistance(count, totalMinutes, totalDistance, maxCalories) – незмінне значення підсумку. Компактний конструктор відхиляє від'ємні та нескінченні показники; компілятор автоматично створює конструктор, методи доступу, equals, hashCode, toString. Статичний метод of(List<Training>) обчислює показники.

*Лістинг 2 – src/main/java/ua/lpnu/kzp/DurationDistance.java*

```text
package ua.lpnu.kzp;

import java.util.List;

/**
 * Незмінний підсумок показників варіанта 2.
 *
 * @param count кількість коректних записів
 * @param totalMinutes сумарний час, хв
 * @param totalDistance сумарна дистанція, км
 * @param maxCalories найбільша витрата калорій
 */
public record DurationDistance(int count, int totalMinutes, double totalDistance, int maxCalories) {
    /** Перевіряє допустимість підсумкових показників. */
    public DurationDistance {
        if (count < 0 || totalMinutes < 0 || maxCalories < 0
                || totalDistance < 0 || !Double.isFinite(totalDistance)) {
            throw new IllegalArgumentException("Показники не можуть бути від'ємними");
        }
    }

    /**
     * Обчислює підсумок для списку тренувань.
     *
     * @param trainings коректні тренування
     * @return підсумкові показники
     */
    public static DurationDistance of(List<Training> trainings) {
        int minutes = 0;
        double distance = 0.0;
        int maxCalories = 0;
        for (Training training : trainings) {
            minutes += training.getMinutes();
            distance += training.getDistanceKm();
            maxCalories = Math.max(maxCalories, training.getCalories());
        }
        return new DurationDistance(trainings.size(), minutes, distance, maxCalories);
    }
}
```

*Лістинг 3 – TrainingProcessor.java (фрагмент parseAll)*

```text
    /**
     * Перетворює рядки на тренування; хибний рядок не потрапляє до списку.
     *
     * @param lines усі рядки файла
     * @param trainings список, до якого додаються коректні тренування
     * @return повідомлення про пропущені рядки з номером і причиною
     */
    public static List<String> parseAll(List<String> lines, List<Training> trainings) {
        List<String> errors = new ArrayList<>();
        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            try {
                trainings.add(Training.fromCsv(lines.get(lineNumber - 1)));
            } catch (IllegalArgumentException exception) {
                errors.add("Рядок %d: %s".formatted(lineNumber, exception.getMessage()));
            }
        }
        return errors;
    }
```

## Тестування

До 10 перенесених тестів ЛР1 додано 12 тестів TrainingTest: коректний конструктор зберігає всі поля; граничний випадок 0 хв / 0 км / 0 ккал; порожній і null-вид, null-дата; від'ємні значення; NaN і нескінченність; fromCsv для коректного рядка; неправильна кількість полів, число й дата; порушення інваріанта в CSV; toString із крапкою; значеннєва рівність і компоненти record; record відхиляє від'ємні значення; хибний рядок не потрапляє до списку й не змінює статистику.

*Лістинг 4 – src/test/java/ua/lpnu/kzp/TrainingTest.java*

```text
package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrainingTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 1);

    /* Перевіряє, що коректний конструктор зберігає всі поля. */
    @Test
    void constructorStoresAllFields() {
        Training training = new Training(DATE, "біг", 45, 8.5, 520);
        assertEquals(DATE, training.getDate());
        assertEquals("біг", training.getKind());
        assertEquals(45, training.getMinutes());
        assertEquals(8.5, training.getDistanceKm(), 0.0001);
        assertEquals(520, training.getCalories());
    }

    /* Перевіряє граничний коректний випадок: нульові хвилини, дистанція й калорії. */
    @Test
    void constructorAcceptsZeroBoundary() {
        Training training = new Training(DATE, "тренажери", 0, 0.0, 0);
        assertEquals(0, training.getMinutes());
    }

    /* Перевіряє відхилення порожнього та null-виду. */
    @Test
    void constructorRejectsBlankOrNullKind() {
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, " ", 45, 8.5, 520));
        assertThrows(NullPointerException.class, () -> new Training(DATE, null, 45, 8.5, 520));
        assertThrows(NullPointerException.class, () -> new Training(null, "біг", 45, 8.5, 520));
    }

    /* Перевіряє відхилення від'ємних значень. */
    @Test
    void constructorRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, "біг", -1, 8.5, 520));
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, "біг", 45, -0.1, 520));
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, "біг", 45, 8.5, -5));
    }

    /* Перевіряє відхилення NaN і нескінченної дистанції. */
    @Test
    void constructorRejectsNonFiniteDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Training(DATE, "біг", 45, Double.NaN, 520));
        assertThrows(IllegalArgumentException.class,
                () -> new Training(DATE, "біг", 45, Double.POSITIVE_INFINITY, 520));
    }

    /* Перевіряє створення сутності з коректного CSV-рядка. */
    @Test
    void fromCsvCreatesValidEntity() {
        Training training = Training.fromCsv("2026-09-02;велосипед;90;32.00;780");
        assertEquals(LocalDate.of(2026, 9, 2), training.getDate());
        assertEquals("велосипед", training.getKind());
        assertEquals(32.0, training.getDistanceKm(), 0.0001);
    }

    /* Перевіряє відхилення неправильної кількості полів, числа та дати. */
    @Test
    void fromCsvRejectsInvalidStructure() {
        assertThrows(IllegalArgumentException.class, () -> Training.fromCsv("2026-09-14;біг;40;7.00"));
        assertThrows(IllegalArgumentException.class,
                () -> Training.fromCsv("2026-09-13;плавання;сорок;1.20;380"));
        assertThrows(IllegalArgumentException.class, () -> Training.fromCsv("13.09.2026;біг;40;7.00;300"));
    }

    /* Перевіряє, що порушення інваріанта у CSV також відхиляється. */
    @Test
    void fromCsvRejectsInvariantViolation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Training.fromCsv("2026-09-11;біг;-20;4.00;300"));
        assertEquals("від'ємне числове значення", exception.getMessage());
    }

    /* Перевіряє toString із крапкою як десятковим роздільником. */
    @Test
    void toStringUsesRootLocale() {
        assertEquals("2026-09-01 біг: 45 хв, 8.50 км, 520 ккал",
                new Training(DATE, "біг", 45, 8.5, 520).toString());
    }

    /* Перевіряє значеннєву рівність record. */
    @Test
    void recordUsesValueEquality() {
        DurationDistance first = new DurationDistance(2, 135, 40.5, 780);
        assertEquals(first, new DurationDistance(2, 135, 40.5, 780));
        assertNotEquals(first, new DurationDistance(2, 135, 40.5, 781));
        assertEquals(135, first.totalMinutes());
    }

    /* Перевіряє, що record відхиляє від'ємні показники. */
    @Test
    void recordRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new DurationDistance(-1, 0, 0, 0));
    }

    /* Перевіряє, що хибний рядок не потрапляє до списку й не змінює статистику. */
    @Test
    void invalidLineDoesNotChangeStatistics() {
        List<Training> trainings = new ArrayList<>();
        List<String> errors = TrainingProcessor.parseAll(List.of(
                "2026-09-01;біг;45;8.50;520", "2026-09-11;біг;-20;4.00;900"), trainings);
        assertEquals(1, trainings.size());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("Рядок 2"));
        assertEquals(520, DurationDistance.of(trainings).maxCalories());
    }
}
```

## Інфраструктура

*Лістинг 5 – Результат mvnw.cmd -B clean verify*

```text
[INFO] Building kzp-lab-andrushko 1.1.0
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.155 s -- in ua.lpnu.kzp.TrainingProcessorTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.060 s -- in ua.lpnu.kzp.TrainingTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: C:\Users\user\kzp-labs\lab02\target\kzp-lab-andrushko-1.1.0.jar
[INFO] BugInstance size is 0
[INFO] No errors/warnings found
[INFO] BUILD SUCCESS
```

*Лістинг 6 – java -jar target/kzp-lab-andrushko-1.1.0.jar --version*

```text
kzp-lab-andrushko 1.1.0
```

GitHub Actions (успадкований ci.yml) запускає старі й нові тести на трьох ОС і публікує JAR:

GitHub Actions, запуск для PR #16: https://github.com/wwwattra/kzp-lab01-05-andrushko/actions/runs/39139699577

*Таблиця 2 – Результати CI на трьох ОС*

| Job | Результат |
|---|---|
| verify (ubuntu-latest) | success ✔ |
| verify (macos-latest) | success ✔ |
| verify (windows-latest) | success ✔ |

*Таблиця 3 – Артефакти workflow (JAR)*

| Артефакт | Розмір, КБ |
|---|---|
| jar-macos-latest-6 | 19.9 |
| jar-ubuntu-latest-6 | 19.9 |
| jar-windows-latest-6 | 19.9 |

Артефакти доступні для завантаження на сторінці запуску https://github.com/wwwattra/kzp-lab01-05-andrushko/actions/runs/39139699577 (розділ Artifacts).

## GitHub Issues і Pull Request

*Таблиця 4 – GitHub Issues → зміна → перевірка*

| Issue | Мітка | Зміна | Перевірка |
|---|---|---|---|
| #10 Клас-сутність Training | feature | private final поля, геттери, toString | TrainingTest |
| #11 Валідація в конструкторі | feature | інваріанти, requireNonNull, isFinite | негативні тести |
| #12 Фабрика fromCsv | feature | розбір структури, LocalDate | тести fromCsv |
| #13 Record DurationDistance | feature | підсумок замість double[] | тест рівності |
| #14 Збереження поведінки ЛР1 | infra | фасад TrainingProcessor | 10 тестів ЛР1 без змін, порівняння звітів |
| #15 Документація ЛР2 | docs | README, javadoc, REPORT | рев'ю PR |

Pull Request #16 «ЛР2: клас Training, fromCsv і record DurationDistance» (https://github.com/wwwattra/kzp-lab01-05-andrushko/pull/16) містить опис змін, результати перевірок і рядки Closes для Issues #10, #11, #12, #13, #14, #15. Після зеленого CI PR злито в main (merge commit), Issues закрилися автоматично, на коміт злиття поставлено тег v1.1.0.

## Порівняння поведінки

Контрольний вхідний файл ЛР1 оброблено версіями 1.0.0 і 1.1.0. Команда Compare-Object (PowerShell) для out/report.txt обох версій не знайшла жодної відмінності: кількість записів, показники, порядок рядків і повідомлення про помилки однакові.

*Лістинг 7 – Вивід версії 1.1.0 (ідентичний ЛР1)*

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
Звіт записано у out\report.txt
```

## Академічна доброчесність

Заповніть самостійно: використані інструменти ШІ або твердження, що вони не використовувалися; ролі, запити, прийняті й відхилені рекомендації; власний внесок і підтвердження розуміння всього поданого коду.

## Відповіді на контрольні питання

1. Яку проблему розв'язує клас-сутність порівняно з рядком і паралельними змінними?

Поля отримують імена й типи (getMinutes() замість fields[2]), правила перевірки зосереджено в одному місці, а некоректний об'єкт неможливо створити.

2. Що означають private і final у полях класу?

private закриває прямий доступ ззовні класу, final забороняє повторне присвоєння після конструктора. Разом вони роблять стан Training незмінним.

3. Що таке інкапсуляція і яку проблему вона запобігає?

Приховування внутрішнього представлення з наданням лише дозволених операцій. Вона не дає зовнішньому коду записати в об'єкт від'ємні хвилини чи порожній вид.

4. Чому перевірку інваріантів розміщують у конструкторі?

Конструктор – єдиний шлях створення об'єкта, тому хибне значення відхиляється до того, як об'єкт потрапить у список чи статистику.

5. Чим Objects.requireNonNull відрізняється від isBlank?

requireNonNull перевіряє відсутність значення (null) і кидає NullPointerException; isBlank перевіряє наявний рядок на порожність або пробіли.

6. Чому відсутність сеттерів може допомогти зберегти коректний стан?

Немає способу змінити поле після перевірки в конструкторі, тож інваріант виконується протягом усього життя об'єкта.

7. Які методи й властивості автоматично отримує record?

Канонічний конструктор, private final поля, методи доступу з іменами компонентів (count()), equals, hashCode і toString.

8. Чим record відрізняється від звичайного класу-сутності?

Record – компактний носій даних із фіксованим набором компонентів і значеннєвою рівністю; він не може мати інших полів екземпляра і не успадковується. Клас-сутність може мати довільну поведінку, приховані поля й власні правила.

9. Чому record не гарантує глибоку незмінність змінного списку?

Record зберігає посилання на список, а не копію; вміст списку можна змінити через інше посилання. Потрібна явна List.copyOf.

10. Для чого потрібен статичний фабричний метод fromCsv?

Назва пояснює джерело даних, метод приховує розбір полів і перетворення типів, а конструктор залишається відповідальним лише за інваріанти.

11. Де потрібно перехоплювати помилку розбору одного рядка?

У циклі читання (parseAll), де відомий номер рядка: виняток перетворюється на повідомлення «Рядок N: причина», і обробка продовжується.

12. Чому неправильний запис не можна додавати до списку до завершення перевірки?

Інакше він вплинув би на суми й максимум. У моєму коді add виконується лише з результатом успішного fromCsv; це перевіряє тест invalidLineDoesNotChangeStatistics.

13. Яке призначення toString і чому він не замінює формат CSV?

toString дає зрозуміле подання для звіту й налагодження; його формат («45 хв, 8.50 км») не призначений для зворотного розбору, тому CSV формується окремо.

14. Навіщо використовувати Locale.ROOT у toString і звіті?

Щоб десятковий роздільник завжди був крапкою, незалежно від регіональних налаштувань Windows (у українській локалі – кома).

15. Що означає схема тесту AAA?

Arrange – підготувати дані, Act – виконати дію, Assert – перевірити результат.

16. Які негативні випадки потрібно перевірити для конструктора?

null і порожній вид, null-дата, від'ємні хвилини/дистанція/калорії, NaN і нескінченна дистанція.

17. Як довести, що рефакторинг не змінив зовнішню поведінку попередньої роботи?

Обробити той самий контрольний файл обома версіями й порівняти звіти, а також запустити старі тести без змін. Обидві перевірки виконано.

18. Чому тести попередньої роботи мають проходити після цієї роботи?

Вони фіксують контракт продукту; падіння старого тесту означало б зміну поведінки, а не лише внутрішньої моделі.

19. Які файли інфраструктури переносяться з попередньої роботи без повторного створення?

pom.xml (змінено лише версію), mvnw, mvnw.cmd, .mvn/wrapper, ci.yml, форми Issues, шаблон PR, .gitignore, .gitattributes, .editorconfig.

20. Який рядок Вашої реалізації найкраще демонструє інкапсуляцію і чому?

private final int minutes; разом із перевіркою if (minutes < 0 …) throw у конструкторі: поле неможливо ні прочитати напряму, ні змінити, ні отримати від'ємним.

## Висновки

Рядкові записи ЛР1 замінено незмінним класом Training з перевіркою інваріантів, фабрикою fromCsv і record DurationDistance. Зовнішній звіт збігся з попередньою версією побайтово, 22 тести (10 старих і 12 нових) проходять, SpotBugs не має зауважень. Клас-сутність стане основою ЛР3, де його буде розділено на підтипи з поліморфною поведінкою.

