package ua.lpnu.kzp;

import java.time.LocalDate;

/** Тренування в приміщенні: плавання, тренажери. */
public final class IndoorTraining extends Training {
    /** Коефіцієнт інтенсивності для тренувань у приміщенні. */
    public static final double INTENSITY = 1.2;

    /**
     * Створює тренування в приміщенні.
     *
     * @param date дата
     * @param kind вид, для якого {@link TrainingKind#isOutdoor()} хибне
     * @param minutes тривалість, хв
     * @param distanceKm дистанція, км; для тренажерів допускається 0
     * @param calories калорії
     */
    public IndoorTraining(LocalDate date, TrainingKind kind, int minutes, double distanceKm, int calories) {
        super(date, kind, minutes, distanceKm, calories);
        if (kind.isOutdoor()) {
            throw new IllegalArgumentException("вид %s не є тренуванням у приміщенні".formatted(kind.label()));
        }
    }

    /** Навантаження = хвилини · 1.2 + калорії / 10 (дистанція не враховується). */
    @Override
    public double load() {
        return getMinutes() * INTENSITY + getCalories() / 10.0;
    }
}
