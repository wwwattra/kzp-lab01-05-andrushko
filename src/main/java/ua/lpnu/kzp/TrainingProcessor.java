package ua.lpnu.kzp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Читає список тренувань із рядків і формує звіт.
 *
 * <p>Після ЛР2 правила перевірки зосереджено в {@link Training}; методи ЛР1 збережено як
 * тонкий фасад, щоб попередні тести проходили без змін.
 */
public final class TrainingProcessor {
    /** Кількість полів у коректному рядку. */
    public static final int FIELD_COUNT = Training.FIELD_COUNT;
    /** Індекс показника «кількість коректних записів» у масиві статистики. */
    public static final int COUNT = 0;
    /** Індекс показника «сумарний час, хв». */
    public static final int TOTAL_MINUTES = 1;
    /** Індекс показника «сумарна дистанція, км». */
    public static final int TOTAL_DISTANCE = 2;
    /** Індекс показника «найбільша витрата калорій». */
    public static final int MAX_CALORIES = 3;

    /* Забороняє створення екземплярів службового класу. */
    private TrainingProcessor() {
    }

    /**
     * Перевіряє один рядок і повертає причину помилки.
     *
     * @param line вхідний рядок
     * @return текст причини або {@code null}, якщо рядок коректний
     */
    public static String validate(String line) {
        try {
            Training.fromCsv(line);
            return null;
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
    }

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

    /**
     * Розділяє рядки на коректні та помилки (сумісність із ЛР1).
     *
     * @param lines усі рядки файла
     * @param valid список, до якого додаються коректні рядки
     * @return повідомлення про пропущені рядки
     */
    public static List<String> splitValid(List<String> lines, List<String> valid) {
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String reason = validate(lines.get(index));
            if (reason == null) {
                valid.add(lines.get(index));
            } else {
                errors.add("Рядок %d: %s".formatted(index + 1, reason));
            }
        }
        return errors;
    }

    /**
     * Обчислює показники за коректними рядками (сумісність із ЛР1).
     *
     * @param validLines коректні рядки
     * @return масив {кількість, сумарний час, сумарна дистанція, максимум калорій}
     */
    public static double[] computeStats(List<String> validLines) {
        List<Training> trainings = new ArrayList<>();
        for (String line : validLines) {
            trainings.add(Training.fromCsv(line));
        }
        DurationDistance summary = DurationDistance.of(trainings);
        return new double[] {summary.count(), summary.totalMinutes(),
            summary.totalDistance(), summary.maxCalories()};
    }

    /**
     * Обчислює сумарне навантаження через поліморфний метод без перевірки підтипу.
     *
     * @param trainings тренування різних підтипів
     * @return сума {@link Training#load()}
     */
    public static double totalLoad(List<Training> trainings) {
        // Реалізацію load() обирає JVM за фактичним підтипом об'єкта.
        return trainings.stream().mapToDouble(Training::load).sum();
    }

    /**
     * Формує розділ звіту про навантаження за видами та унікальні записи.
     *
     * @param trainings коректні тренування
     * @return текст розділу
     */
    public static String formatLoadSection(List<Training> trainings) {
        Map<TrainingKind, Double> byKind = trainings.stream()
                .collect(Collectors.groupingBy(Training::getTrainingKind,
                        () -> new EnumMap<>(TrainingKind.class),
                        Collectors.summingDouble(Training::load)));
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

    /**
     * Формує розділ звіту з результатами п'яти Stream-запитів і пошуку Optional.
     *
     * @param trainings коректні тренування
     * @param searchDates дати для демонстрації знайденого й відсутнього результату
     * @return текст розділу
     */
    public static String formatQueriesSection(List<Training> trainings, List<LocalDate> searchDates) {
        StringBuilder section = new StringBuilder();
        section.append(String.format(Locale.ROOT, "Запити Stream API%n"));
        List<Training> longOnes = TrainingQueries.longTrainings(trainings);
        section.append(String.format(Locale.ROOT, "1. Тривалість >= %d хв: %d записів%n",
                TrainingQueries.MIN_MINUTES, longOnes.size()));
        longOnes.forEach(training -> section.append("   ").append(training).append(System.lineSeparator()));
        section.append(String.format(Locale.ROOT, "2. Дистанції, км: %s%n",
                TrainingQueries.distances(trainings).stream()
                        .map(distance -> String.format(Locale.ROOT, "%.2f", distance))
                        .collect(Collectors.joining(", ", "[", "]"))));
        section.append(String.format(Locale.ROOT, "3. Сумарна дистанція за видом: %s%n",
                TrainingQueries.distanceByKind(trainings).entrySet().stream()
                        .map(entry -> String.format(Locale.ROOT, "%s=%.2f",
                                entry.getKey().label(), entry.getValue()))
                        .collect(Collectors.joining(", ", "{", "}"))));
        IntSummaryStatistics calories = TrainingQueries.calorieStatistics(trainings);
        if (calories.getCount() == 0) {
            section.append(String.format(Locale.ROOT, "4. Статистика калорій: немає даних%n"));
        } else {
            section.append(String.format(Locale.ROOT,
                    "4. Статистика калорій: count=%d, sum=%d, min=%d, max=%d, avg=%.2f%n",
                    calories.getCount(), calories.getSum(), calories.getMin(),
                    calories.getMax(), calories.getAverage()));
        }
        section.append(String.format(Locale.ROOT, "5. Top-%d за дистанцією (спадання), потім за датою:%n",
                TrainingQueries.TOP_N));
        TrainingQueries.topByDistance(trainings, TrainingQueries.TOP_N)
                .forEach(training -> section.append("   ").append(training).append(System.lineSeparator()));
        for (LocalDate date : searchDates) {
            section.append(String.format(Locale.ROOT, "Пошук за датою %s: %s%n", date,
                    TrainingQueries.findByDate(trainings, date)
                            .map(Training::toString)
                            .orElse("не знайдено")));
        }
        return section.toString();
    }

    /**
     * Формує звіт із масиву показників (сумісність із ЛР1).
     *
     * @param stats показники
     * @param errors повідомлення про пропущені рядки
     * @return текст звіту
     */
    public static String formatReport(double[] stats, List<String> errors) {
        return formatReport(new DurationDistance((int) stats[COUNT], (int) stats[TOTAL_MINUTES],
                stats[TOTAL_DISTANCE], (int) stats[MAX_CALORIES]), errors);
    }

    /**
     * Формує текст звіту, незалежний від локалі операційної системи.
     *
     * @param summary підсумкові показники
     * @param errors повідомлення про пропущені рядки
     * @return текст звіту
     */
    public static String formatReport(DurationDistance summary, List<String> errors) {
        StringBuilder report = new StringBuilder();
        String nl = System.lineSeparator();
        report.append("Звіт: спортивні тренування").append(nl);
        report.append(String.format(Locale.ROOT, "%-34s %10s%n", "Показник", "Значення"));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n", "Коректних записів", summary.count()));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n", "Сумарний час, хв", summary.totalMinutes()));
        report.append(String.format(Locale.ROOT, "%-34s %10.2f%n",
                "Сумарна дистанція, км", summary.totalDistance()));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n",
                "Найбільша витрата калорій, ккал", summary.maxCalories()));
        report.append(String.format(Locale.ROOT, "Пропущено рядків: %d%n", errors.size()));
        for (String error : errors) {
            report.append(error).append(nl);
        }
        return report.toString();
    }
}
