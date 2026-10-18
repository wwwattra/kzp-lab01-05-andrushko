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
