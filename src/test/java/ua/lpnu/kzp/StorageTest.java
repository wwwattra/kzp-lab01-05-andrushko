package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageTest {
    @TempDir
    Path directory;

    private final List<TrainingRecord> original = List.of(
            TrainingRecord.from(Training.fromCsv("2026-09-01;біг;45;8.50;520")),
            TrainingRecord.from(Training.fromCsv("2026-09-02;велосипед;90;32.00;780")),
            TrainingRecord.from(Training.fromCsv("2026-09-03;плавання;40;1.50;410")),
            TrainingRecord.from(Training.fromCsv("2026-09-06;тренажери;60;0.00;450")),
            new TrainingRecord(LocalDate.of(2026, 9, 8), TrainingKind.WALKING, 75, 6.8, 300,
                    "Парк \"Сихів\", 2 кола\nдругий рядок опису"));

    /* Плоский клас іншого домену для перевірки універсальності експортера. */
    static final class OtherRecord {
        @CsvColumn("код")
        private final String code = "A-1";
        @CsvColumn("кількість")
        private final int amount = 3;
    }

    /* Клас без анотацій. */
    static final class NoColumns {
        private final int value = 1;
    }

    /* Перевіряє типобезпечне сховище, пошук і незмінність знімка. */
    @Test
    void repositoryStoresFindsAndReturnsSnapshot() {
        Repository<TrainingRecord> repository = new Repository<>();
        repository.addAll(original);
        assertEquals(5, repository.all().size());
        assertEquals(1, repository.find(record -> record.getKind() == TrainingKind.CYCLING).size());
        assertThrows(UnsupportedOperationException.class, () -> repository.all().clear());
        assertThrows(NullPointerException.class, () -> repository.add(null));
    }

    /* Перевіряє анотацію під час виконання і детермінований порядок стовпців. */
    @Test
    void headerComesFromAnnotationsSortedByFieldName() throws Exception {
        assertEquals(List.of("калорії", "дата", "опис", "дистанція", "тривалість", "вид"),
                CsvExporter.header(TrainingRecord.class));
        CsvColumn column = TrainingRecord.class.getDeclaredField("date").getAnnotation(CsvColumn.class);
        assertEquals("дата", column.value());
    }

    /* Перевіряє екранування коми, лапок і перенесення рядка. */
    @Test
    void escapesCommaQuotesAndNewline() {
        assertEquals("просто", CsvExporter.escape("просто"));
        assertEquals("\"а, б\"", CsvExporter.escape("а, б"));
        assertEquals("\"Парк \"\"Сихів\"\"\"", CsvExporter.escape("Парк \"Сихів\""));
        assertEquals("\"1\n2\"", CsvExporter.escape("1\n2"));
    }

    /* Перевіряє round-trip, у тому числі multiline-поле. */
    @Test
    void roundTripPreservesAllRecords() throws Exception {
        Path path = directory.resolve("trainings.csv");
        Repository<TrainingRecord> repository = new Repository<>();
        repository.addAll(original);
        TrainingStorage.save(path, repository);
        String csv = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"Парк \"\"Сихів\"\", 2 кола\nдругий рядок опису\""));
        List<TrainingRecord> restored = TrainingStorage.load(path);
        assertEquals(original, restored);
        // 1 заголовок + 5 записів, хоча фізичних рядків більше через multiline-поле.
        assertEquals(6, CsvParser.parseDocument(csv).size());
    }

    /* Перевіряє експорт порожнього набору: лише заголовок. */
    @Test
    void exportsEmptyRepositoryAsHeaderOnly() throws Exception {
        Path path = directory.resolve("empty.csv");
        TrainingStorage.save(path, new Repository<>());
        assertEquals(List.of(), TrainingStorage.load(path));
        assertEquals(1, CsvParser.parseDocument(Files.readString(path, StandardCharsets.UTF_8)).size());
    }

    /* Перевіряє, що експортер працює з іншим класом і відхиляє клас без анотацій. */
    @Test
    void exporterIsNotBoundToDomain() throws Exception {
        String csv = CsvExporter.toCsv(OtherRecord.class, List.of(new OtherRecord()));
        assertEquals(List.of(List.of("кількість", "код"), List.of("3", "A-1")), CsvParser.parseDocument(csv));
        assertThrows(DataStorageException.class, () -> CsvExporter.toCsv(NoColumns.class, List.of()));
    }

    /* Перевіряє хибну кількість полів, хибне число та хибний enum. */
    @Test
    void rejectsCorruptedRows() {
        List<String> header = List.of("калорії", "дата", "опис", "дистанція", "тривалість", "вид");
        assertThrows(DataStorageException.class,
                () -> TrainingRecord.fromFields(header, List.of("520", "2026-09-01")));
        DataStorageException number = assertThrows(DataStorageException.class,
                () -> TrainingRecord.fromFields(header,
                        List.of("багато", "2026-09-01", "о", "8.5", "45", "RUNNING")));
        assertInstanceOf(NumberFormatException.class, number.getCause());
        DataStorageException kind = assertThrows(DataStorageException.class,
                () -> TrainingRecord.fromFields(header,
                        List.of("520", "2026-09-01", "о", "8.5", "45", "YOGA")));
        assertInstanceOf(IllegalArgumentException.class, kind.getCause());
    }

    /* Перевіряє помилки парсера: незакриті лапки та символ після лапок. */
    @Test
    void parserRejectsBrokenQuotes() {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"не закрито,1"));
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"а\"б,1"));
        assertThrows(DataStorageException.class, () -> CsvParser.parse("1\n2"));
    }

    /* Перевіряє збереження першопричини помилки файлової системи. */
    @Test
    void wrapsFileErrorWithCause() throws Exception {
        Path notAFile = directory.resolve("folder");
        Files.createDirectory(notAFile);
        DataStorageException write = assertThrows(DataStorageException.class,
                () -> TrainingStorage.save(notAFile, new Repository<>()));
        assertInstanceOf(IOException.class, write.getCause());
        DataStorageException read = assertThrows(DataStorageException.class,
                () -> TrainingStorage.load(directory.resolve("missing.csv")));
        assertInstanceOf(IOException.class, read.getCause());
    }
}
