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
     * Обчислює підсумок для списку тренувань.
     *
     * @param trainings коректні тренування
     * @return підсумкові показники
     */
    public static DurationDistance of(List<Training> trainings) {
        int minutes = 0;
        double distance = 0.0;
        int maxCalories = 0;
        for (Training training : trainings) {
            minutes += training.getMinutes();
            distance += training.getDistanceKm();
            maxCalories = Math.max(maxCalories, training.getCalories());
        }
        return new DurationDistance(trainings.size(), minutes, distance, maxCalories);
    }
}
