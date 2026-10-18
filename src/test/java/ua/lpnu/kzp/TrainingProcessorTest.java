package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrainingProcessorTest {
    /* Перевіряє, що коректний рядок з українським видом проходить перевірку. */
    @Test
    void acceptsValidLineWithUkrainianText() {
        assertNull(TrainingProcessor.validate("2026-09-01;біг;45;8.50;520"));
    }

    /* Перевіряє відхилення порожнього рядка. */
    @Test
    void rejectsBlankLine() {
        assertEquals("порожній рядок", TrainingProcessor.validate("   "));
    }

    /* Перевіряє відхилення неправильної кількості полів. */
    @Test
    void rejectsWrongFieldCount() {
        assertTrue(TrainingProcessor.validate("2026-09-14;біг;40;7.00").startsWith("очікується 5"));
    }

    /* Перевіряє відхилення нечислового поля. */
    @Test
    void rejectsNonNumericField() {
        assertEquals("числове поле має помилковий формат",
                TrainingProcessor.validate("2026-09-13;плавання;сорок;1.20;380"));
    }

    /* Перевіряє відхилення від'ємного значення. */
    @Test
    void rejectsNegativeValue() {
        assertEquals("від'ємне числове значення",
                TrainingProcessor.validate("2026-09-11;біг;-20;4.00;300"));
    }

    /* Перевіряє відхилення порожнього текстового поля. */
    @Test
    void rejectsEmptyKind() {
        assertEquals("порожня дата або вид тренування",
                TrainingProcessor.validate("2026-09-12;;50;6.00;400"));
    }

    /* Перевіряє обчислення сум і максимуму для кількох записів. */
    @Test
    void computesStatsForSeveralRecords() {
        double[] stats = TrainingProcessor.computeStats(List.of(
                "2026-09-01;біг;45;8.50;520",
                "2026-09-02;велосипед;90;32.00;780"));
        assertEquals(2, stats[TrainingProcessor.COUNT], 0.0001);
        assertEquals(135, stats[TrainingProcessor.TOTAL_MINUTES], 0.0001);
        assertEquals(40.5, stats[TrainingProcessor.TOTAL_DISTANCE], 0.0001);
        assertEquals(780, stats[TrainingProcessor.MAX_CALORIES], 0.0001);
    }

    /* Перевіряє файл без коректних записів. */
    @Test
    void handlesFileWithoutValidRecords() {
        List<String> valid = new ArrayList<>();
        List<String> errors = TrainingProcessor.splitValid(List.of("", "a;b"), valid);
        assertTrue(valid.isEmpty());
        assertEquals(2, errors.size());
        assertEquals(0, TrainingProcessor.computeStats(valid)[TrainingProcessor.COUNT], 0.0001);
    }

    /* Перевіряє, що звіт не залежить від локалі та використовує крапку. */
    @Test
    void formatsReportWithDot() {
        String report = TrainingProcessor.formatReport(new double[] {1, 45, 8.5, 520}, List.of());
        assertTrue(report.contains("8.50"));
    }

    /* Перевіряє запис і читання звіту в UTF-8. */
    @Test
    void writesAndReadsUtf8(@TempDir Path directory) throws Exception {
        Path output = directory.resolve("out").resolve("report.txt");
        FileReport.writeReport(output, "Звіт: біг\n");
        assertEquals(List.of("Звіт: біг"), FileReport.readLines(output));
        assertTrue(Files.exists(output));
    }
}
