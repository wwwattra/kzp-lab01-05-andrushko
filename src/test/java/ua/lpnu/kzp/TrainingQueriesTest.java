package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrainingQueriesTest {
    /* Контрольний набір: 8 записів, дубльована дата 09-08, рівна дистанція 8.50 у 09-01 і 09-05. */
    private final List<Training> trainings = List.of(
            Training.fromCsv("2026-09-01;біг;45;8.50;520"),
            Training.fromCsv("2026-09-02;велосипед;90;32.00;780"),
            Training.fromCsv("2026-09-03;плавання;40;1.50;410"),
            Training.fromCsv("2026-09-05;біг;30;8.50;350"),
            Training.fromCsv("2026-09-06;тренажери;60;0.00;450"),
            Training.fromCsv("2026-09-08;ходьба;75;6.80;300"),
            Training.fromCsv("2026-09-08;велосипед;120;41.30;820"),
            Training.fromCsv("2026-09-10;біг;45;10.00;610"));

    /* Перевіряє фільтр і межу 45 хв (45 входить, 40 - ні). */
    @Test
    void filtersLongTrainingsIncludingBoundary() {
        List<Training> result = TrainingQueries.longTrainings(trainings);
        assertEquals(6, result.size());
        assertTrue(result.stream().allMatch(training -> training.getMinutes() >= 45));
        assertTrue(result.contains(trainings.get(0)));
    }

    /* Перевіряє перетворення map і збереження порядку. */
    @Test
    void mapsDistancesInOriginalOrder() {
        assertEquals(List.of(8.5, 32.0, 1.5, 8.5, 0.0, 6.8, 41.3, 10.0),
                TrainingQueries.distances(trainings));
    }

    /* Перевіряє групування з явним типом ключа. */
    @Test
    void groupsDistanceByKind() {
        Map<TrainingKind, Double> result = TrainingQueries.distanceByKind(trainings);
        assertEquals(27.0, result.get(TrainingKind.RUNNING), 0.0001);
        assertEquals(73.3, result.get(TrainingKind.CYCLING), 0.0001);
        assertEquals(0.0, result.get(TrainingKind.GYM), 0.0001);
        assertEquals(5, result.size());
    }

    /* Перевіряє зведену статистику та порожній вхід. */
    @Test
    void summarizesCaloriesAndEmptyInput() {
        IntSummaryStatistics statistics = TrainingQueries.calorieStatistics(trainings);
        assertEquals(8, statistics.getCount());
        assertEquals(4240, statistics.getSum());
        assertEquals(300, statistics.getMin());
        assertEquals(820, statistics.getMax());
        assertEquals(530.0, statistics.getAverage(), 0.0001);
        assertEquals(0, TrainingQueries.calorieStatistics(List.of()).getCount());
    }

    /* Перевіряє top-5, другий критерій і усікання шостого запису. */
    @Test
    void ordersTopByDistanceThenDate() {
        List<Training> top = TrainingQueries.topByDistance(trainings, 5);
        assertEquals(List.of(41.3, 32.0, 10.0, 8.5, 8.5),
                top.stream().map(Training::getDistanceKm).toList());
        // За рівної дистанції 8.50 раніша дата стоїть першою.
        assertEquals(LocalDate.of(2026, 9, 1), top.get(3).getDate());
        assertEquals(LocalDate.of(2026, 9, 5), top.get(4).getDate());
    }

    /* Перевіряє нульовий, завеликий і від'ємний ліміт. */
    @Test
    void handlesLimitBoundaries() {
        assertEquals(List.of(), TrainingQueries.topByDistance(trainings, 0));
        assertEquals(8, TrainingQueries.topByDistance(trainings, 100).size());
        assertThrows(IllegalArgumentException.class, () -> TrainingQueries.topByDistance(trainings, -1));
    }

    /* Перевіряє Optional: дубльований ключ повертає перший запис, відсутній - empty. */
    @Test
    void findsFirstByDateOrEmpty() {
        Optional<Training> found = TrainingQueries.findByDate(trainings, LocalDate.of(2026, 9, 8));
        assertEquals(TrainingKind.WALKING, found.orElseThrow().getTrainingKind());
        assertTrue(TrainingQueries.findByDate(trainings, LocalDate.of(2026, 9, 30)).isEmpty());
        assertTrue(TrainingQueries.findByDate(List.of(), LocalDate.of(2026, 9, 8)).isEmpty());
    }

    /* Перевіряє еквівалентність потокового підсумку та циклу ЛР2. */
    @Test
    void streamSummaryMatchesLegacyLoop() {
        int minutes = 0;
        double distance = 0;
        int maxCalories = 0;
        for (Training training : trainings) {
            minutes += training.getMinutes();
            distance += training.getDistanceKm();
            maxCalories = Math.max(maxCalories, training.getCalories());
        }
        DurationDistance summary = DurationDistance.of(trainings);
        assertEquals(minutes, summary.totalMinutes());
        assertEquals(distance, summary.totalDistance(), 0.0001);
        assertEquals(maxCalories, summary.maxCalories());
        assertEquals(new DurationDistance(0, 0, 0.0, 0), DurationDistance.of(List.of()));
    }

    /* Перевіряє, що запити не змінюють вхідну колекцію. */
    @Test
    void queriesDoNotMutateInput() {
        List<Training> source = new ArrayList<>(trainings);
        List<Training> before = List.copyOf(source);
        TrainingQueries.longTrainings(source);
        TrainingQueries.distances(source);
        TrainingQueries.distanceByKind(source);
        TrainingQueries.calorieStatistics(source);
        TrainingQueries.topByDistance(source, 3);
        TrainingQueries.findByDate(source, LocalDate.of(2026, 9, 8));
        assertEquals(before, source);
    }

    /* Перевіряє порожній вхід для всіх запитів. */
    @Test
    void handlesEmptyInput() {
        assertTrue(TrainingQueries.longTrainings(List.of()).isEmpty());
        assertTrue(TrainingQueries.distances(List.of()).isEmpty());
        assertTrue(TrainingQueries.distanceByKind(List.of()).isEmpty());
        assertTrue(TrainingQueries.topByDistance(List.of(), 5).isEmpty());
    }
}
