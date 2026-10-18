package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Експортує анотовані поля будь-якого плоского класу у CSV з кодуванням UTF-8. */
public final class CsvExporter {
    /* Забороняє створення екземплярів статичного експортера. */
    private CsvExporter() {
    }

    /**
     * Записує анотовані поля записів у UTF-8 CSV.
     *
     * @param <T> тип записів
     * @param path шлях до файла
     * @param type клас записів (потрібен для заголовка навіть без записів)
     * @param records записи для експорту
     * @throws DataStorageException якщо поля неможливо прочитати або файл записати
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(path, "Шлях не може бути null");
        Objects.requireNonNull(records, "Записи не можуть бути null");
        String csv = toCsv(type, records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, csv, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося записати CSV: %s".formatted(path), exception);
        }
    }

    /**
     * Формує текст CSV без запису у файл.
     *
     * @param <T> тип записів
     * @param type клас записів
     * @param records записи
     * @return текст із заголовком і рядками
     * @throws DataStorageException якщо клас не має анотованих полів або поле недоступне
     */
    public static <T> String toCsv(Class<T> type, List<? extends T> records) throws DataStorageException {
        List<Field> fields = annotatedFields(type);
        // Заголовок і значення будуються з одного детерміновано відсортованого набору полів.
        StringBuilder csv = new StringBuilder(fields.stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(",")))
                .append(System.lineSeparator());
        for (T record : records) {
            if (record == null) {
                throw new DataStorageException("Список містить null-запис");
            }
            List<String> values = new ArrayList<>();
            for (Field field : fields) {
                try {
                    values.add(escape(field.get(record)));
                } catch (IllegalAccessException exception) {
                    throw new DataStorageException(
                            "Не вдалося прочитати поле %s".formatted(field.getName()), exception);
                }
            }
            csv.append(String.join(",", values)).append(System.lineSeparator());
        }
        return csv.toString();
    }

    /**
     * Повертає назви стовпців у порядку експорту.
     *
     * @param type клас записів
     * @return заголовки з {@link CsvColumn}
     * @throws DataStorageException якщо клас не має анотованих полів
     */
    public static List<String> header(Class<?> type) throws DataStorageException {
        return annotatedFields(type).stream()
                .map(field -> field.getAnnotation(CsvColumn.class).value())
                .toList();
    }

    /* Повертає доступні анотовані поля класу та його суперкласів у стабільному порядку. */
    private static List<Field> annotatedFields(Class<?> type) throws DataStorageException {
        Objects.requireNonNull(type, "Тип не може бути null");
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            fields.addAll(Arrays.stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                    .toList());
        }
        if (fields.isEmpty()) {
            throw new DataStorageException("Клас %s не має CSV-полів".formatted(type.getSimpleName()));
        }
        fields.sort(Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (!field.trySetAccessible()) {
                throw new DataStorageException("Немає доступу до поля %s".formatted(field.getName()));
            }
        }
        return List.copyOf(fields);
    }

    /* Екранує значення за правилами CSV: кома, лапка або перенесення - у лапки. */
    static String escape(Object value) {
        String text = Objects.toString(value, "");
        boolean quoted = text.contains(",") || text.contains("\"")
                || text.contains("\r") || text.contains("\n");
        return quoted ? "\"%s\"".formatted(text.replace("\"", "\"\"")) : text;
    }
}
