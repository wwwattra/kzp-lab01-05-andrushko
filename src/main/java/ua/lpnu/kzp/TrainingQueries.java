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
