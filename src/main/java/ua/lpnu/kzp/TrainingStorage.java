package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Зберігає записи тренувань у CSV і відновлює їх назад (round-trip). */
public final class TrainingStorage {
    /* Забороняє створення екземплярів службового класу. */
    private TrainingStorage() {
    }

    /**
     * Експортує вміст сховища у CSV.
     *
     * @param path шлях до файла
     * @param repository сховище записів
     * @throws DataStorageException якщо експорт неможливий
     */
    public static void save(Path path, Repository<TrainingRecord> repository) throws DataStorageException {
        CsvExporter.write(path, TrainingRecord.class, repository.all());
    }

    /**
     * Читає CSV у UTF-8 та відновлює записи.
     *
     * @param path шлях до файла
     * @return відновлені записи у порядку файла
     * @throws DataStorageException якщо файл неможливо прочитати або дані пошкоджено
     */
    public static List<TrainingRecord> load(Path path) throws DataStorageException {
        String csv;
        try {
            csv = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(path), exception);
        }
        List<List<String>> rows = CsvParser.parseDocument(csv);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> header = rows.get(0);
        List<TrainingRecord> restored = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            restored.add(TrainingRecord.fromFields(header, rows.get(index)));
        }
        return List.copyOf(restored);
    }
}
