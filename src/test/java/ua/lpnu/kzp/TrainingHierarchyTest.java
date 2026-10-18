package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrainingHierarchyTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 8);

    /* Перевіряє, що фабрика обирає підтип за видом. */
    @Test
    void factoryChoosesSubtypeByKind() {
        assertInstanceOf(OutdoorTraining.class, Training.create(DATE, "біг", 45, 8.5, 520));
        assertInstanceOf(IndoorTraining.class, Training.create(DATE, "плавання", 40, 1.5, 410));
    }

    /* Перевіряє формули поліморфного методу кожного підтипу. */
    @Test
    void subtypesCalculateLoadDifferently() {
        Training outdoor = Training.create(DATE, "біг", 60, 10.0, 600);
        Training indoor = Training.create(DATE, "тренажери", 60, 0.0, 600);
        assertEquals(60 + 30 + 60, outdoor.load(), 0.0001);
        assertEquals(72 + 60, indoor.load(), 0.0001);
        assertNotEquals(outdoor.load(), indoor.load());
    }

    /* Перевіряє власне правило підтипу: надворі дистанція має бути додатною. */
    @Test
    void outdoorRejectsZeroDistance() {
        assertThrows(IllegalArgumentException.class, () -> Training.create(DATE, "ходьба", 30, 0.0, 100));
    }

    /* Перевіряє, що підтип не приймає вид іншої категорії. */
    @Test
    void subtypeRejectsForeignKind() {
        assertThrows(IllegalArgumentException.class,
                () -> new IndoorTraining(DATE, TrainingKind.RUNNING, 30, 5.0, 300));
        assertThrows(IllegalArgumentException.class,
                () -> new OutdoorTraining(DATE, TrainingKind.GYM, 30, 1.0, 300));
    }

    /* Перевіряє, що базовий тип відхиляє неправильний спільний стан. */
    @Test
    void baseTypeRejectsInvalidCommonState() {
        assertThrows(IllegalArgumentException.class, () -> Training.create(DATE, "плавання", -5, 1.0, 100));
        assertThrows(NullPointerException.class,
                () -> new IndoorTraining(DATE, null, 30, 1.0, 300));
    }

    /* Перевіряє пошук enum за підписом і невідомий вид. */
    @Test
    void enumFindsByLabel() {
        assertEquals(TrainingKind.CYCLING, TrainingKind.fromLabel("велосипед"));
        assertTrue(TrainingKind.CYCLING.isOutdoor());
        assertThrows(IllegalArgumentException.class, () -> TrainingKind.fromLabel("йога"));
    }

    /* Перевіряє, що рівні об'єкти мають однаковий hashCode і не дублюються в HashSet. */
    @Test
    void equalObjectsHaveOneSetEntry() {
        Training first = Training.create(DATE, "біг", 45, 8.5, 520);
        Training second = Training.fromCsv("2026-09-08;біг;45;8.50;520");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        Set<Training> set = new HashSet<>(List.of(first, second));
        assertEquals(1, set.size());
    }

    /* Перевіряє, що логічно рівний об'єкт знаходиться як ключ HashMap. */
    @Test
    void equalObjectWorksAsMapKey() {
        Map<Training, String> notes = new HashMap<>();
        notes.put(Training.create(DATE, "плавання", 40, 1.5, 410), "басейн");
        assertEquals("басейн", notes.get(Training.create(DATE, "плавання", 40, 1.5, 410)));
    }

    /* Перевіряє, що поліморфна сума не залежить від порядку і не перевіряє підтип. */
    @Test
    void polymorphicTotalUsesBaseType() {
        List<Training> trainings = List.of(
                Training.create(DATE, "біг", 60, 10.0, 600),
                Training.create(DATE, "тренажери", 60, 0.0, 600));
        assertEquals(150 + 132, TrainingProcessor.totalLoad(trainings), 0.0001);
    }
}
