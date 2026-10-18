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
