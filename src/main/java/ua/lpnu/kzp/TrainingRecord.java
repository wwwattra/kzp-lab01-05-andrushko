package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Плоский запис тренування для CSV-експорту (варіант 2, TrainingRecord).
 *
 * <p>Поле {@code pulse} з таблиці ЛР5 замінено на {@code calories}, бо формат вхідних даних
 * закріплено в ЛР1. Поле {@code description} містить текстовий опис із комами й потрібне для
 * перевірки CSV-екранування.
 */
public final class TrainingRecord {
    /** Кількість стовпців у CSV-експорті. */
    public static final int COLUMN_COUNT = 6;

    @CsvColumn("дата")
    private final LocalDate date;

    @CsvColumn("вид")
    private final TrainingKind kind;

    @CsvColumn("тривалість")
    private final int durationMinutes;

    @CsvColumn("дистанція")
    private final double distanceKm;

    @CsvColumn("калорії")
    private final int calories;

    @CsvColumn("опис")
    private final String description;

    /**
     * Створює перевірений запис.
     *
     * @param date дата
     * @param kind вид
     * @param durationMinutes тривалість, хв
     * @param distanceKm дистанція, км
     * @param calories калорії
     * @param description текстовий опис (може містити коми, лапки, перенесення)
     */
    public TrainingRecord(LocalDate date, TrainingKind kind, int durationMinutes,
            double distanceKm, int calories, String description) {
        this.date = Objects.requireNonNull(date, "Дата не може бути null");
        this.kind = Objects.requireNonNull(kind, "Вид не може бути null");
        this.description = Objects.requireNonNull(description, "Опис не може бути null");
        if (durationMinutes < 0 || distanceKm < 0 || calories < 0 || !Double.isFinite(distanceKm)) {
            throw new IllegalArgumentException("від'ємне або нескінченне числове значення");
        }
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
        this.calories = calories;
    }

    /**
     * Перетворює поліморфне тренування на плоский запис.
     *
     * @param training тренування будь-якого підтипу
     * @return запис з описом {@link Training#toString()}
     */
    public static TrainingRecord from(Training training) {
        return new TrainingRecord(training.getDate(), training.getTrainingKind(), training.getMinutes(),
                training.getDistanceKm(), training.getCalories(), training.toString());
    }

    /**
     * Відновлює запис із CSV-полів у порядку заголовка.
     *
     * @param header назви стовпців (перший запис документа)
     * @param values значення одного запису
     * @return відновлений запис
     * @throws DataStorageException якщо кількість полів, число, дата або enum некоректні
     */
    public static TrainingRecord fromFields(List<String> header, List<String> values)
            throws DataStorageException {
        if (values.size() != COLUMN_COUNT || header.size() != COLUMN_COUNT) {
            throw new DataStorageException("Очікується %d полів CSV, отримано %d"
                    .formatted(COLUMN_COUNT, values.size()));
        }
        try {
            return new TrainingRecord(
                    LocalDate.parse(value(header, values, "дата")),
                    TrainingKind.valueOf(value(header, values, "вид")),
                    Integer.parseInt(value(header, values, "тривалість")),
                    Double.parseDouble(value(header, values, "дистанція")),
                    Integer.parseInt(value(header, values, "калорії")),
                    value(header, values, "опис"));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new DataStorageException("Некоректний рядок тренування: %s".formatted(values), exception);
        }
    }

    /* Повертає значення стовпця за назвою заголовка. */
    private static String value(List<String> header, List<String> values, String column)
            throws DataStorageException {
        int index = header.indexOf(column);
        if (index < 0) {
            throw new DataStorageException("У заголовку немає стовпця %s".formatted(column));
        }
        return values.get(index);
    }

    /** @return дата */
    public LocalDate getDate() {
        return date;
    }

    /** @return вид */
    public TrainingKind getKind() {
        return kind;
    }

    /** @return тривалість, хв */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** @return дистанція, км */
    public double getDistanceKm() {
        return distanceKm;
    }

    /** @return опис */
    public String getDescription() {
        return description;
    }

    /** Порівнює записи за всіма полями. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TrainingRecord record)) {
            return false;
        }
        return durationMinutes == record.durationMinutes
                && Double.compare(distanceKm, record.distanceKm) == 0
                && calories == record.calories
                && date.equals(record.date)
                && kind == record.kind
                && description.equals(record.description);
    }

    /** Повертає хеш-код, узгоджений із {@code equals}. */
    @Override
    public int hashCode() {
        return Objects.hash(date, kind, durationMinutes, distanceKm, calories, description);
    }

    /** Повертає ключ round-trip (дата + вид) і основні значення. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s/%s: %d хв, %.2f км", date, kind, durationMinutes, distanceKm);
    }
}
