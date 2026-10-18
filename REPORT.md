# Звіт до лабораторної роботи №4

з дисципліни «Кросплатформні засоби програмування», тема «Потокова обробка даних у Java-проєкті». Варіант 2. Спортивні тренування.
Виконав: студент групи КІ-302 Андрушко А. І. Прийняв: Олексів М. В.

## Тема, номер і варіант

Лабораторна робота № 4 «Потокова обробка даних у Java-проєкті». Варіант 2 – «Спортивні тренування». ОС – Windows 10 (64-bit), JDK Temurin 21.0.12, Maven 3.9.11 через Wrapper. Версія продукту – 1.3.0.

Посилання на GitHub-репозиторій: https://github.com/[ваш-логін]/kzp-lab-andrushko, гілка lab04, Git-тег v1.3.0.

## Мета роботи

Замінити основні циклові обчислення поліморфної моделі на декларативні запити Stream API без зміни зовнішньої поведінки: додати відбір, перетворення, групування, зведену статистику, складене впорядкування top-N і безпечний пошук через Optional.

## Стан до і після цієї роботи

*Таблиця 1 – Цикл → Stream API → результат*

| Обчислення | До (цикл) | Після (потік) | Результат |
|---|---|---|---|
| Сумарний час | for + minutes += | mapToInt(getMinutes).sum() | 505 – збігся |
| Сумарна дистанція | for + distance += | mapToDouble(getDistanceKm).sum() | 108.60 – збігся |
| Максимум калорій | Math.max у циклі | mapToInt(getCalories).max().orElse(0) | 820 – збігся |
| Сумарне навантаження | for + total += load() | mapToDouble(Training::load).sum() | 1270.30 – збігся |
| Навантаження за видом | EnumMap.merge у циклі | groupingBy(..., EnumMap, summingDouble) | 5 груп – збіглися |

Еквівалентність додатково доведено тестом streamSummaryMatchesLegacyLoop, який обчислює ті самі значення старим циклом і порівнює з потоковою реалізацією.

## Контракт даних

Джерело – List<Training> (поліморфні об'єкти OutdoorTraining/IndoorTraining). Поля: LocalDate date, TrainingKind kind, int minutes, double distanceKm, int calories; правила валідності – з ЛР2–ЛР3. Дати подаються через LocalDate, кількості – int, дистанція – double з форматуванням Locale.ROOT.

Уточнення варіанта: таблиця ЛР4 називає поля «тривалість, дистанція, пульс», але формат CSV закріплено в ЛР1 (date;kind;minutes;distanceKm;calories) і змінювати його заборонено. Тому запит статистики будується за полем calories (витрата калорій). Поріг фільтра – константа MIN_MINUTES = 45; у даних є значення до межі (30, 40), на межі (45) і після (60–120).

## П'ять запитів

*Таблиця 2 – Запити класу TrainingQueries*

| № | Метод | Проміжні операції | Кінцева | Тип результату |
|---|---|---|---|---|
| 1 | longTrainings | filter(minutes >= 45) | toList() | List<Training> |
| 2 | distances | map(getDistanceKm) | toList() | List<Double> |
| 3 | distanceByKind | – | collect(groupingBy) | Map<TrainingKind, Double> |
| 4 | calorieStatistics | – | collect(summarizingInt) | IntSummaryStatistics |
| 5 | topByDistance | sorted(comparator), limit(N) | toList() | List<Training> |
| + | findByDate | filter(date) | findFirst() | Optional<Training> |

*Лістинг 1 – src/main/java/ua/lpnu/kzp/TrainingQueries.java*

```text
package ua.lpnu.kzp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Набір чистих потокових запитів до колекції тренувань (варіант 2). */
public final class TrainingQueries {
    /** Поріг фільтра: тривалість не менша 45 хв. */
    public static final int MIN_MINUTES = 45;
    /** Кількість записів у запиті top-N. */
    public static final int TOP_N = 5;

    /** Дистанція за спаданням, за рівності - дата за зростанням. */
    public static final Comparator<Training> BY_DISTANCE_DESC_THEN_DATE =
            Comparator.comparingDouble(Training::getDistanceKm)
                    .reversed()
                    .thenComparing(Training::getDate);

    /* Забороняє створення екземплярів класу статичних запитів. */
    private TrainingQueries() {
    }

    /**
     * Запит 1 (filter): тренування тривалістю не менше {@link #MIN_MINUTES} хв.
     *
     * @param trainings джерело
     * @return новий незмінний список
     */
    public static List<Training> longTrainings(List<Training> trainings) {
        return streamOf(trainings)
                .filter(training -> training.getMinutes() >= MIN_MINUTES)
                .toList();
    }

    /**
     * Запит 2 (map): дистанції у початковому порядку.
     *
     * @param trainings джерело
     * @return список дистанцій, км
     */
    public static List<Double> distances(List<Training> trainings) {
        return streamOf(trainings)
                .map(Training::getDistanceKm)
                .toList();
    }

    /**
     * Запит 3 (groupingBy): сумарна дистанція за видом у стабільному порядку enum.
     *
     * @param trainings джерело
     * @return карта вид → сума дистанцій
     */
    public static Map<TrainingKind, Double> distanceByKind(List<Training> trainings) {
        return streamOf(trainings)
                .collect(Collectors.groupingBy(
                        Training::getTrainingKind,
                        () -> new EnumMap<>(TrainingKind.class),
                        Collectors.summingDouble(Training::getDistanceKm)));
    }

    /**
     * Запит 4 (summarizingInt): статистика витрати калорій.
     *
     * <p>Поле «пульс» з таблиці ЛР4 відсутнє у форматі CSV, закріпленому в ЛР1, тому
     * статистика будується за калоріями.
     *
     * @param trainings джерело
     * @return кількість, сума, мінімум, максимум і середнє
     */
    public static IntSummaryStatistics calorieStatistics(List<Training> trainings) {
        return streamOf(trainings)
                .collect(Collectors.summarizingInt(Training::getCalories));
    }

    /**
     * Запит 5 (sorted + limit): перші N тренувань за дистанцією, потім за датою.
     *
     * @param trainings джерело
     * @param limit кількість результатів, не від'ємна
     * @return впорядкований префікс
     * @throws IllegalArgumentException якщо {@code limit < 0}
     */
    public static List<Training> topByDistance(List<Training> trainings, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Ліміт не може бути від'ємним");
        }
        // Спочатку повний порядок, потім усікання: sorted(...).limit(N).
        return streamOf(trainings)
                .sorted(BY_DISTANCE_DESC_THEN_DATE)
                .limit(limit)
                .toList();
    }

    /**
     * Пошук першого тренування за датою.
     *
     * @param trainings джерело
     * @param date дата пошуку
     * @return перше тренування в початковому порядку або {@link Optional#empty()}
     */
    public static Optional<Training> findByDate(List<Training> trainings, LocalDate date) {
        Objects.requireNonNull(date, "Дата не може бути null");
        return streamOf(trainings)
                .filter(training -> training.getDate().equals(date))
                .findFirst();
    }

    /* Перевіряє список і повертає новий потік для кожного незалежного запиту. */
    private static Stream<Training> streamOf(List<Training> trainings) {
        return Objects.requireNonNull(trainings, "Список не може бути null").stream();
    }
}
```

## Групування та статистика

Ключ групування – TrainingKind; карта створюється як EnumMap, тому порядок ключів стабільний і не залежить від HashMap. Агрегат – summingDouble(getDistanceKm). Статистика калорій повертає count, sum, min, max і average. Для порожнього потоку count = 0, а min/max мають службові значення Integer.MAX_VALUE/MIN_VALUE, тому звіт обробляє цей випадок явно й виводить «немає даних».

## Компаратор

BY_DISTANCE_DESC_THEN_DATE = comparingDouble(getDistanceKm).reversed().thenComparing(getDate): основний критерій – дистанція за спаданням, другий – дата за зростанням. reversed() застосовано до компаратора дистанції до додавання другого критерію, тому дата не розвертається. Порядок операцій – sorted(...).limit(N), N = 5. Записи 2026-09-01 і 2026-09-05 мають однакову дистанцію 8.50 км і перевіряють другий критерій; шостий запис (6.80 км) перевіряє усікання. limit < 0 відхиляється IllegalArgumentException, 0 дає порожній список, завеликий N – усі записи.

## Optional

findByDate шукає перше тренування за датою. Дата 2026-09-08 зустрічається двічі (ходьба й велосипед) – повертається перший запис у початковому порядку (ходьба), що перевірено тестом. Для 2026-09-30 результат Optional.empty(), і звіт виводить «не знайдено» через map(...).orElse(...); метод ніколи не повертає null.

## Тестування

Перенесено 31 тест ЛР1–ЛР3 і додано 10 тестів TrainingQueriesTest: фільтр із межею 45 хв, порядок у map, групування з явним типом ключа, статистика й порожній вхід, top-5 із другим критерієм, межі ліміту (0, 100, −1), Optional для дубльованого й відсутнього ключа, еквівалентність циклу, відсутність мутації вхідної колекції, порожній вхід для всіх запитів.

*Лістинг 2 – src/test/java/ua/lpnu/kzp/TrainingQueriesTest.java*

```text
package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrainingQueriesTest {
    /* Контрольний набір: 8 записів, дубльована дата 09-08, рівна дистанція 8.50 у 09-01 і 09-05. */
    private final List<Training> trainings = List.of(
            Training.fromCsv("2026-09-01;біг;45;8.50;520"),
            Training.fromCsv("2026-09-02;велосипед;90;32.00;780"),
            Training.fromCsv("2026-09-03;плавання;40;1.50;410"),
            Training.fromCsv("2026-09-05;біг;30;8.50;350"),
            Training.fromCsv("2026-09-06;тренажери;60;0.00;450"),
            Training.fromCsv("2026-09-08;ходьба;75;6.80;300"),
            Training.fromCsv("2026-09-08;велосипед;120;41.30;820"),
            Training.fromCsv("2026-09-10;біг;45;10.00;610"));

    /* Перевіряє фільтр і межу 45 хв (45 входить, 40 - ні). */
    @Test
    void filtersLongTrainingsIncludingBoundary() {
        List<Training> result = TrainingQueries.longTrainings(trainings);
        assertEquals(6, result.size());
        assertTrue(result.stream().allMatch(training -> training.getMinutes() >= 45));
        assertTrue(result.contains(trainings.get(0)));
    }

    /* Перевіряє перетворення map і збереження порядку. */
    @Test
    void mapsDistancesInOriginalOrder() {
        assertEquals(List.of(8.5, 32.0, 1.5, 8.5, 0.0, 6.8, 41.3, 10.0),
                TrainingQueries.distances(trainings));
    }

    /* Перевіряє групування з явним типом ключа. */
    @Test
    void groupsDistanceByKind() {
        Map<TrainingKind, Double> result = TrainingQueries.distanceByKind(trainings);
        assertEquals(27.0, result.get(TrainingKind.RUNNING), 0.0001);
        assertEquals(73.3, result.get(TrainingKind.CYCLING), 0.0001);
        assertEquals(0.0, result.get(TrainingKind.GYM), 0.0001);
        assertEquals(5, result.size());
    }

    /* Перевіряє зведену статистику та порожній вхід. */
    @Test
    void summarizesCaloriesAndEmptyInput() {
        IntSummaryStatistics statistics = TrainingQueries.calorieStatistics(trainings);
        assertEquals(8, statistics.getCount());
        assertEquals(4240, statistics.getSum());
        assertEquals(300, statistics.getMin());
        assertEquals(820, statistics.getMax());
        assertEquals(530.0, statistics.getAverage(), 0.0001);
        assertEquals(0, TrainingQueries.calorieStatistics(List.of()).getCount());
    }

    /* Перевіряє top-5, другий критерій і усікання шостого запису. */
    @Test
    void ordersTopByDistanceThenDate() {
        List<Training> top = TrainingQueries.topByDistance(trainings, 5);
        assertEquals(List.of(41.3, 32.0, 10.0, 8.5, 8.5),
                top.stream().map(Training::getDistanceKm).toList());
        // За рівної дистанції 8.50 раніша дата стоїть першою.
        assertEquals(LocalDate.of(2026, 9, 1), top.get(3).getDate());
        assertEquals(LocalDate.of(2026, 9, 5), top.get(4).getDate());
    }

    /* Перевіряє нульовий, завеликий і від'ємний ліміт. */
    @Test
    void handlesLimitBoundaries() {
        assertEquals(List.of(), TrainingQueries.topByDistance(trainings, 0));
        assertEquals(8, TrainingQueries.topByDistance(trainings, 100).size());
        assertThrows(IllegalArgumentException.class, () -> TrainingQueries.topByDistance(trainings, -1));
    }

    /* Перевіряє Optional: дубльований ключ повертає перший запис, відсутній - empty. */
    @Test
    void findsFirstByDateOrEmpty() {
        Optional<Training> found = TrainingQueries.findByDate(trainings, LocalDate.of(2026, 9, 8));
        assertEquals(TrainingKind.WALKING, found.orElseThrow().getTrainingKind());
        assertTrue(TrainingQueries.findByDate(trainings, LocalDate.of(2026, 9, 30)).isEmpty());
        assertTrue(TrainingQueries.findByDate(List.of(), LocalDate.of(2026, 9, 8)).isEmpty());
    }

    /* Перевіряє еквівалентність потокового підсумку та циклу ЛР2. */
    @Test
    void streamSummaryMatchesLegacyLoop() {
        int minutes = 0;
        double distance = 0;
        int maxCalories = 0;
        for (Training training : trainings) {
            minutes += training.getMinutes();
            distance += training.getDistanceKm();
            maxCalories = Math.max(maxCalories, training.getCalories());
        }
        DurationDistance summary = DurationDistance.of(trainings);
        assertEquals(minutes, summary.totalMinutes());
        assertEquals(distance, summary.totalDistance(), 0.0001);
        assertEquals(maxCalories, summary.maxCalories());
        assertEquals(new DurationDistance(0, 0, 0.0, 0), DurationDistance.of(List.of()));
    }

    /* Перевіряє, що запити не змінюють вхідну колекцію. */
    @Test
    void queriesDoNotMutateInput() {
        List<Training> source = new ArrayList<>(trainings);
        List<Training> before = List.copyOf(source);
        TrainingQueries.longTrainings(source);
        TrainingQueries.distances(source);
        TrainingQueries.distanceByKind(source);
        TrainingQueries.calorieStatistics(source);
        TrainingQueries.topByDistance(source, 3);
        TrainingQueries.findByDate(source, LocalDate.of(2026, 9, 8));
        assertEquals(before, source);
    }

    /* Перевіряє порожній вхід для всіх запитів. */
    @Test
    void handlesEmptyInput() {
        assertTrue(TrainingQueries.longTrainings(List.of()).isEmpty());
        assertTrue(TrainingQueries.distances(List.of()).isEmpty());
        assertTrue(TrainingQueries.distanceByKind(List.of()).isEmpty());
        assertTrue(TrainingQueries.topByDistance(List.of(), 5).isEmpty());
    }
}
```

*Лістинг 3 – Результат mvnw.cmd -B clean verify*

```text
[INFO] Building kzp-lab-andrushko 1.3.0
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.092 s -- in ua.lpnu.kzp.TrainingHierarchyTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.117 s -- in ua.lpnu.kzp.TrainingProcessorTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.042 s -- in ua.lpnu.kzp.TrainingQueriesTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.027 s -- in ua.lpnu.kzp.TrainingTest
[INFO] Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: C:\Users\user\kzp-labs\lab04\target\kzp-lab-andrushko-1.3.0.jar
[INFO] BugInstance size is 0
[INFO] No errors/warnings found
[INFO] BUILD SUCCESS
```

## Інфраструктура

Команди: mvnw.cmd test, mvnw.cmd verify, mvnw.cmd package, java -jar target/kzp-lab-andrushko-1.3.0.jar. SpotBugs у фазі verify – без зауважень. CI на Ubuntu, Windows і macOS та JAR-артефакт: посилання на запуск – після push.

## GitHub Issues і Pull Request

*Таблиця 3 – План GitHub Issues → зміна → перевірка*

| Issue | Мітка | Зміна | Перевірка |
|---|---|---|---|
| Запити filter і map | feature | longTrainings, distances | тести межі й порядку |
| Групування та статистика | feature | distanceByKind, calorieStatistics | groupsDistanceByKind, summarizes… |
| Top-N зі складеним компаратором | feature | topByDistance | ordersTop…, handlesLimit… |
| Пошук через Optional | feature | findByDate | findsFirstByDateOrEmpty |
| Заміна циклів у звіті | feature | DurationDistance.of, totalLoad | streamSummaryMatchesLegacyLoop |
| Документація ЛР4 | docs | README, javadoc, REPORT | рев'ю PR |

## Результати запитів

*Лістинг 4 – Вивід версії 1.3.0*

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
Запити Stream API
1. Тривалість >= 45 хв: 6 записів
   2026-09-01 біг: 45 хв, 8.50 км, 520 ккал
   2026-09-02 велосипед: 90 хв, 32.00 км, 780 ккал
   2026-09-06 тренажери: 60 хв, 0.00 км, 450 ккал
   2026-09-08 ходьба: 75 хв, 6.80 км, 300 ккал
   2026-09-08 велосипед: 120 хв, 41.30 км, 820 ккал
   2026-09-10 біг: 45 хв, 10.00 км, 610 ккал
2. Дистанції, км: [8.50, 32.00, 1.50, 8.50, 0.00, 6.80, 41.30, 10.00]
3. Сумарна дистанція за видом: {біг=27.00, велосипед=73.30, ходьба=6.80, плавання=1.50, тренажери=0.00}
4. Статистика калорій: count=8, sum=4240, min=300, max=820, avg=530.00
5. Top-5 за дистанцією (спадання), потім за датою:
   2026-09-08 велосипед: 120 хв, 41.30 км, 820 ккал
   2026-09-02 велосипед: 90 хв, 32.00 км, 780 ккал
   2026-09-10 біг: 45 хв, 10.00 км, 610 ккал
   2026-09-01 біг: 45 хв, 8.50 км, 520 ккал
   2026-09-05 біг: 30 хв, 8.50 км, 350 ккал
Пошук за датою 2026-09-08: 2026-09-08 ходьба: 75 хв, 6.80 км, 300 ккал
Пошук за датою 2026-09-30: не знайдено
Звіт записано у out\report.txt
```

Перші 12 рядків звіту ідентичні попереднім версіям; розділ «Запити Stream API» додано наприкінці.

## Порівняння реалізацій

Потокова версія читабельніша там, де обчислення – послідовність кроків «відібрати → перетворити → зібрати»: намір видно з назв операцій, накопичувальні змінні зникли, групування в EnumMap займає один вираз. Водночас кількість рядків не є критерієм: форматування результатів залишилося звичайним кодом, а для простих сум потік лише трохи коротший за цикл. Важливіше, що запити стали чистими функціями без побічних ефектів, які легко тестувати окремо.

## Академічна доброчесність

Заповніть самостійно: використані інструменти ШІ або твердження, що вони не використовувалися; ролі, запити, прийняті й відхилені рекомендації; власний внесок і підтвердження розуміння всього поданого коду.

## Відповіді на контрольні питання

1. Чим потік відрізняється від колекції?

Колекція зберігає елементи; потік лише описує обчислення над джерелом і не зберігає даних, використовується один раз.

2. Які частини має типовий конвеєр Stream API?

Джерело (list.stream()), проміжні операції (filter, map, sorted) і кінцева операція (toList, collect, findFirst).

3. Чому проміжні операції називають лінивими?

Вони не виконуються, доки не викликано кінцеву операцію; лише вона запускає конвеєр.

4. Що робить filter і який тип має його предикат?

Залишає елементи, для яких Predicate<T> повертає true; у мене – training -> training.getMinutes() >= 45.

5. Чим map відрізняється від mapToDouble?

map повертає Stream<R> з об'єктами (Double пакується), mapToDouble – DoubleStream примітивів із методами sum, average.

6. Для чого застосовується distinct і від чого залежить його результат?

Прибирає повтори; результат залежить від коректних equals/hashCode, реалізованих у ЛР3.

7. Чому порядок sorted().limit(N) відрізняється від limit(N).sorted()?

Перший варіант дає N найкращих з усієї колекції, другий сортує лише перші N вхідних елементів.

8. Яку роль виконує кінцева операція?

Запускає обчислення й повертає результат або побічний ефект.

9. Коли застосовується Collectors.joining?

Для збирання рядків в один текст із роздільником, префіксом і суфіксом – у моєму звіті список дистанцій [8.50, 32.00, …].

10. Як groupingBy будує карту груп?

Для кожного елемента обчислює ключ (getTrainingKind) і додає елемент до групи цього ключа; фабрика карти й downstream-колектор задаються додатковими аргументами.

11. Як отримати суму в кожній групі через downstream collector?

groupingBy(Training::getTrainingKind, () -> new EnumMap<>(TrainingKind.class), summingDouble(Training::getDistanceKm)).

12. Які показники містить DoubleSummaryStatistics?

count, sum, min, max, average (у мене аналогічний IntSummaryStatistics).

13. Які значення має статистика для порожнього потоку?

count = 0, sum = 0, average = 0; min – максимальне можливе значення, max – мінімальне (для double ±Infinity), тому порожній випадок обробляється явно.

14. Як утворюється складений компаратор?

Comparator.comparingDouble(...) для основного ключа, .reversed() для напрямку, .thenComparing(...) для другого ключа.

15. Чому reversed() потрібно застосовувати до окремого ключа до thenComparing?

reversed() розвертає весь уже складений компаратор; викликаний після thenComparing, він розвернув би й дату.

16. Яку проблему розв'язує Optional у методі пошуку?

Явно показує можливу відсутність результату й змушує викликача обрати поведінку замість ризику NullPointerException.

17. Чим відрізняються orElse, orElseThrow і ifPresent?

orElse повертає значення за замовчуванням, orElseThrow кидає виняток, ifPresent виконує дію лише за наявності значення.

18. Чому накопичення результату через зовнішній змінний список у forEach небажане?

Це побічний ефект: код гірше читається, не безпечний для паралельних потоків; краще map(...).toList().

19. Які властивості мають бути перевірені під час рефакторингу циклу на потік?

Значення сум/мінімуму/максимуму, порядок, дублікати, порожній вхід, межі limit, відсутність мутації джерела, проходження старих тестів.

20. За яких умов паралельний потік може бути виправданим?

Великий обсяг даних, дорогі незалежні обчислення, асоціативні операції без спільного змінного стану та підтверджене вимірюванням прискорення.

## Висновки

Обчислення ЛР1–ЛР3 перенесено на Stream API, додано п'ять запитів і пошук через Optional. Результати збігаються з цикловою реалізацією (тест еквівалентності та незмінні рядки звіту), 41 тест проходить, SpotBugs без зауважень. Ці запити стануть основою ЛР5, де дані зберігатимуться в параметризованому сховищі й експортуватимуться в CSV.

