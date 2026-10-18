package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Перевіряє рядки про спортивні тренування, обчислює показники та формує звіт.
 *
 * <p>Формат рядка: {@code date;kind;minutes;distanceKm;calories}.
 */
public final class TrainingProcessor {
    /** Кількість полів у коректному рядку. */
    public static final int FIELD_COUNT = 5;

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
        if (line == null || line.isBlank()) {
            return "порожній рядок";
        }
        // -1 зберігає порожні поля наприкінці рядка.
        String[] fields = line.split(";", -1);
        if (fields.length != FIELD_COUNT) {
            return "очікується %d полів, отримано %d".formatted(FIELD_COUNT, fields.length);
        }
        if (fields[0].isBlank() || fields[1].isBlank()) {
            return "порожня дата або вид тренування";
        }
        try {
            int minutes = Integer.parseInt(fields[2].trim());
            double distance = Double.parseDouble(fields[3].trim());
            int calories = Integer.parseInt(fields[4].trim());
            if (minutes < 0 || distance < 0 || calories < 0) {
                return "від'ємне числове значення";
            }
            if (!Double.isFinite(distance)) {
                return "дистанція має бути скінченним числом";
            }
        } catch (NumberFormatException exception) {
            return "числове поле має помилковий формат";
        }
        return null;
    }

    /**
     * Обчислює чотири показники варіанта за коректними рядками.
     *
     * @param validLines рядки, що пройшли {@link #validate(String)}
     * @return масив {кількість, сумарний час, сумарна дистанція, максимум калорій}
     */
    public static double[] computeStats(List<String> validLines) {
        double[] stats = new double[4];
        for (String line : validLines) {
            String[] fields = line.split(";", -1);
            stats[COUNT]++;
            stats[TOTAL_MINUTES] += Integer.parseInt(fields[2].trim());
            stats[TOTAL_DISTANCE] += Double.parseDouble(fields[3].trim());
            stats[MAX_CALORIES] = Math.max(stats[MAX_CALORIES], Integer.parseInt(fields[4].trim()));
        }
        return stats;
    }

    /**
     * Розділяє вхідні рядки на коректні та повідомлення про помилки.
     *
     * @param lines усі рядки файла
     * @param valid список, до якого додаються коректні рядки
     * @return повідомлення про пропущені рядки з номером і причиною
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
     * Формує текст звіту, незалежний від локалі операційної системи.
     *
     * @param stats показники з {@link #computeStats(List)}
     * @param errors повідомлення про пропущені рядки
     * @return готовий текст звіту
     */
    public static String formatReport(double[] stats, List<String> errors) {
        StringBuilder report = new StringBuilder();
        String nl = System.lineSeparator();
        report.append("Звіт: спортивні тренування").append(nl);
        report.append(String.format(Locale.ROOT, "%-34s %10s%n", "Показник", "Значення"));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n",
                "Коректних записів", (long) stats[COUNT]));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n",
                "Сумарний час, хв", (long) stats[TOTAL_MINUTES]));
        report.append(String.format(Locale.ROOT, "%-34s %10.2f%n",
                "Сумарна дистанція, км", stats[TOTAL_DISTANCE]));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n",
                "Найбільша витрата калорій, ккал", (long) stats[MAX_CALORIES]));
        report.append(String.format(Locale.ROOT, "Пропущено рядків: %d%n", errors.size()));
        for (String error : errors) {
            report.append(error).append(nl);
        }
        return report.toString();
    }
}
