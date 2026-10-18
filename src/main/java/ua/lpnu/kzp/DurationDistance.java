package ua.lpnu.kzp;

import java.util.List;

/**
 * Незмінний підсумок показників варіанта 2.
 *
 * @param count кількість коректних записів
 * @param totalMinutes сумарний час, хв
 * @param totalDistance сумарна дистанція, км
 * @param maxCalories найбільша витрата калорій
 */
public record DurationDistance(int count, int totalMinutes, double totalDistance, int maxCalories) {
    /** Перевіряє допустимість підсумкових показників. */
    public DurationDistance {
        if (count < 0 || totalMinutes < 0 || maxCalories < 0
                || totalDistance < 0 || !Double.isFinite(totalDistance)) {
            throw new IllegalArgumentException("Показники не можуть бути від'ємними");
        }
    }

    /**
     * Обчислює підсумок для списку тренувань потоковими операціями.
     *
     * @param trainings коректні тренування
     * @return підсумкові показники
     */
    public static DurationDistance of(List<Training> trainings) {
        // Після ЛР4 ручний цикл замінено числовими потоками; результат не змінився.
        return new DurationDistance(
                trainings.size(),
                trainings.stream().mapToInt(Training::getMinutes).sum(),
                trainings.stream().mapToDouble(Training::getDistanceKm).sum(),
                trainings.stream().mapToInt(Training::getCalories).max().orElse(0));
    }
}
