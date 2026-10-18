package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrainingTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 1);

    /* Перевіряє, що коректний конструктор зберігає всі поля. */
    @Test
    void constructorStoresAllFields() {
        Training training = new Training(DATE, "біг", 45, 8.5, 520);
        assertEquals(DATE, training.getDate());
        assertEquals("біг", training.getKind());
        assertEquals(45, training.getMinutes());
        assertEquals(8.5, training.getDistanceKm(), 0.0001);
        assertEquals(520, training.getCalories());
    }

    /* Перевіряє граничний коректний випадок: нульові хвилини, дистанція й калорії. */
    @Test
    void constructorAcceptsZeroBoundary() {
        Training training = new Training(DATE, "тренажери", 0, 0.0, 0);
        assertEquals(0, training.getMinutes());
    }

    /* Перевіряє відхилення порожнього та null-виду. */
    @Test
    void constructorRejectsBlankOrNullKind() {
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, " ", 45, 8.5, 520));
        assertThrows(NullPointerException.class, () -> new Training(DATE, null, 45, 8.5, 520));
        assertThrows(NullPointerException.class, () -> new Training(null, "біг", 45, 8.5, 520));
    }

    /* Перевіряє відхилення від'ємних значень. */
    @Test
    void constructorRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, "біг", -1, 8.5, 520));
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, "біг", 45, -0.1, 520));
        assertThrows(IllegalArgumentException.class, () -> new Training(DATE, "біг", 45, 8.5, -5));
    }

    /* Перевіряє відхилення NaN і нескінченної дистанції. */
    @Test
    void constructorRejectsNonFiniteDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Training(DATE, "біг", 45, Double.NaN, 520));
        assertThrows(IllegalArgumentException.class,
                () -> new Training(DATE, "біг", 45, Double.POSITIVE_INFINITY, 520));
    }

    /* Перевіряє створення сутності з коректного CSV-рядка. */
    @Test
    void fromCsvCreatesValidEntity() {
        Training training = Training.fromCsv("2026-09-02;велосипед;90;32.00;780");
        assertEquals(LocalDate.of(2026, 9, 2), training.getDate());
        assertEquals("велосипед", training.getKind());
        assertEquals(32.0, training.getDistanceKm(), 0.0001);
    }

    /* Перевіряє відхилення неправильної кількості полів, числа та дати. */
    @Test
    void fromCsvRejectsInvalidStructure() {
        assertThrows(IllegalArgumentException.class, () -> Training.fromCsv("2026-09-14;біг;40;7.00"));
        assertThrows(IllegalArgumentException.class,
                () -> Training.fromCsv("2026-09-13;плавання;сорок;1.20;380"));
        assertThrows(IllegalArgumentException.class, () -> Training.fromCsv("13.09.2026;біг;40;7.00;300"));
    }

    /* Перевіряє, що порушення інваріанта у CSV також відхиляється. */
    @Test
    void fromCsvRejectsInvariantViolation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Training.fromCsv("2026-09-11;біг;-20;4.00;300"));
        assertEquals("від'ємне числове значення", exception.getMessage());
    }

    /* Перевіряє toString із крапкою як десятковим роздільником. */
    @Test
    void toStringUsesRootLocale() {
        assertEquals("2026-09-01 біг: 45 хв, 8.50 км, 520 ккал",
                new Training(DATE, "біг", 45, 8.5, 520).toString());
    }

    /* Перевіряє значеннєву рівність record. */
    @Test
    void recordUsesValueEquality() {
        DurationDistance first = new DurationDistance(2, 135, 40.5, 780);
        assertEquals(first, new DurationDistance(2, 135, 40.5, 780));
        assertNotEquals(first, new DurationDistance(2, 135, 40.5, 781));
        assertEquals(135, first.totalMinutes());
    }

    /* Перевіряє, що record відхиляє від'ємні показники. */
    @Test
    void recordRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new DurationDistance(-1, 0, 0, 0));
    }

    /* Перевіряє, що хибний рядок не потрапляє до списку й не змінює статистику. */
    @Test
    void invalidLineDoesNotChangeStatistics() {
        List<Training> trainings = new ArrayList<>();
        List<String> errors = TrainingProcessor.parseAll(List.of(
                "2026-09-01;біг;45;8.50;520", "2026-09-11;біг;-20;4.00;900"), trainings);
        assertEquals(1, trainings.size());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("Рядок 2"));
        assertEquals(520, DurationDistance.of(trainings).maxCalories());
    }
}
