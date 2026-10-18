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
