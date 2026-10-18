package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
