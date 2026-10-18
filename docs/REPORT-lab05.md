# Звіт до лабораторної роботи №5

з дисципліни «Кросплатформні засоби програмування», тема «Параметризоване сховище, винятки й CSV-експорт у Java-проєкті». Варіант 2. Спортивні тренування.
Виконав: студент групи КІ-302 Андрушко А. І. Прийняв: Олексів М. В.

## Тема, номер і варіант

Лабораторна робота № 5 «Параметризоване сховище, винятки й CSV-експорт у Java-проєкті». Варіант 2 – «Спортивні тренування». ОС – Windows 10 (64-bit), JDK Temurin 21.0.12, Maven 3.9.11 через Wrapper. Версія продукту – 1.4.0.

Посилання на GitHub-репозиторій: https://github.com/wwwattra/kzp-lab01-05-andrushko. Гілка lab05, Pull Request #37 (https://github.com/wwwattra/kzp-lab01-05-andrushko/pull/37), Git-тег v1.4.0 (https://github.com/wwwattra/kzp-lab01-05-andrushko/tree/v1.4.0).

## Мета роботи

Завершити Java-застосунок: замінити пряме зберігання в списку на параметризоване сховище Repository<T>, додати власний виняток DataStorageException із ланцюгом причин, описати CSV-стовпці анотацією @CsvColumn, реалізувати універсальний рефлексивний експортер, записати й прочитати CSV через Path/Files з явним UTF-8 та довести round-trip «об'єкт → CSV → об'єкт».

## Стан до і після цієї роботи

До роботи тренування існували лише в пам'яті як List<Training>, звіт виводився в текстовий файл. Після роботи продукт додатково: перетворює тренування на плоскі записи TrainingRecord, зберігає їх у Repository<TrainingRecord>, експортує у out/trainings.csv (параметр --export), читає файл назад і порівнює списки. Усі попередні показники, розділ навантаження й Stream-запити виводяться без змін.

## Repository<T>

Параметр типу T задається під час створення (Repository<TrainingRecord>), тому сховище приймає лише записи цього типу без приведень і raw types. Методи: add (відхиляє null), addAll(List<? extends T>), all() – незмінний знімок List.copyOf, find(Predicate<? super T>) – пошук за умовою, size(). Демонстрація у звіті: repository.find(record -> record.getKind() == TrainingKind.CYCLING) повертає 2 записи.

*Лістинг 1 – src/main/java/ua/lpnu/kzp/Repository.java*

```text
package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Типобезпечне сховище об'єктів у пам'яті.
 *
 * @param <T> тип записів
 */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /**
     * Додає непорожній запис до сховища.
     *
     * @param item запис
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "Запис не може бути null"));
    }

    /**
     * Додає всі записи зі списку.
     *
     * @param newItems записи
     */
    public void addAll(List<? extends T> newItems) {
        newItems.forEach(this::add);
    }

    /** @return незмінний знімок усіх записів */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову.
     *
     * @param condition умова для T або його супертипу
     * @return незмінний список знайдених записів
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "Умова не може бути null");
        return items.stream().filter(condition).toList();
    }

    /** @return кількість записів */
    public int size() {
        return items.size();
    }
}
```

## Модель даних

*Таблиця 1 – Поля TrainingRecord*

| Поле Java | Тип | @CsvColumn | Інваріант |
|---|---|---|---|
| calories | int | калорії | >= 0 |
| date | LocalDate | дата | не null, ISO YYYY-MM-DD |
| description | String | опис | не null, може містити кому, лапки, перенесення |
| distanceKm | double | дистанція | >= 0, скінченна |
| durationMinutes | int | тривалість | >= 0 |
| kind | TrainingKind (enum) | вид | не null |

Уточнення варіанта. Таблиця ЛР5 визначає TrainingRecord: date, kind, durationMinutes, distanceKm, pulse. Поле пульсу відсутнє у формату вхідних даних ЛР1, тому використано calories. Оскільки серед полів варіанта немає вільного тексту, додано поле description (текстовий опис тренування з toString, напр. «2026-09-01 біг: 45 хв, 8.50 км, 520 ккал»), яке містить коми й перевіряє CSV-екранування. Ключ round-trip – date + kind. equals/hashCode використовують усі шість полів.

*Лістинг 2 – src/main/java/ua/lpnu/kzp/TrainingRecord.java*

```text
package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Плоский запис тренування для CSV-експорту (варіант 2, TrainingRecord).
 *
 * <p>Поле {@code pulse} з таблиці ЛР5 замінено на {@code calories}, бо формат вхідних даних
 * закріплено в ЛР1. Поле {@code description} містить текстовий опис із комами й потрібне для
 * перевірки CSV-екранування.
 */
public final class TrainingRecord {
    /** Кількість стовпців у CSV-експорті. */
    public static final int COLUMN_COUNT = 6;

    @CsvColumn("дата")
    private final LocalDate date;

    @CsvColumn("вид")
    private final TrainingKind kind;

    @CsvColumn("тривалість")
    private final int durationMinutes;

    @CsvColumn("дистанція")
    private final double distanceKm;

    @CsvColumn("калорії")
    private final int calories;

    @CsvColumn("опис")
    private final String description;

    /**
     * Створює перевірений запис.
     *
     * @param date дата
     * @param kind вид
     * @param durationMinutes тривалість, хв
     * @param distanceKm дистанція, км
     * @param calories калорії
     * @param description текстовий опис (може містити коми, лапки, перенесення)
     */
    public TrainingRecord(LocalDate date, TrainingKind kind, int durationMinutes,
            double distanceKm, int calories, String description) {
        this.date = Objects.requireNonNull(date, "Дата не може бути null");
        this.kind = Objects.requireNonNull(kind, "Вид не може бути null");
        this.description = Objects.requireNonNull(description, "Опис не може бути null");
        if (durationMinutes < 0 || distanceKm < 0 || calories < 0 || !Double.isFinite(distanceKm)) {
            throw new IllegalArgumentException("від'ємне або нескінченне числове значення");
        }
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
        this.calories = calories;
    }

    /**
     * Перетворює поліморфне тренування на плоский запис.
     *
     * @param training тренування будь-якого підтипу
     * @return запис з описом {@link Training#toString()}
     */
    public static TrainingRecord from(Training training) {
        return new TrainingRecord(training.getDate(), training.getTrainingKind(), training.getMinutes(),
                training.getDistanceKm(), training.getCalories(), training.toString());
    }

    /**
     * Відновлює запис із CSV-полів у порядку заголовка.
     *
     * @param header назви стовпців (перший запис документа)
     * @param values значення одного запису
     * @return відновлений запис
     * @throws DataStorageException якщо кількість полів, число, дата або enum некоректні
     */
    public static TrainingRecord fromFields(List<String> header, List<String> values)
            throws DataStorageException {
        if (values.size() != COLUMN_COUNT || header.size() != COLUMN_COUNT) {
            throw new DataStorageException("Очікується %d полів CSV, отримано %d"
                    .formatted(COLUMN_COUNT, values.size()));
        }
        try {
            return new TrainingRecord(
                    LocalDate.parse(value(header, values, "дата")),
                    TrainingKind.valueOf(value(header, values, "вид")),
                    Integer.parseInt(value(header, values, "тривалість")),
                    Double.parseDouble(value(header, values, "дистанція")),
                    Integer.parseInt(value(header, values, "калорії")),
                    value(header, values, "опис"));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new DataStorageException("Некоректний рядок тренування: %s".formatted(values), exception);
        }
    }

    /* Повертає значення стовпця за назвою заголовка. */
    private static String value(List<String> header, List<String> values, String column)
            throws DataStorageException {
        int index = header.indexOf(column);
        if (index < 0) {
            throw new DataStorageException("У заголовку немає стовпця %s".formatted(column));
        }
        return values.get(index);
    }

    /** @return дата */
    public LocalDate getDate() {
        return date;
    }

    /** @return вид */
    public TrainingKind getKind() {
        return kind;
    }

    /** @return тривалість, хв */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** @return дистанція, км */
    public double getDistanceKm() {
        return distanceKm;
    }

    /** @return опис */
    public String getDescription() {
        return description;
    }

    /** Порівнює записи за всіма полями. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TrainingRecord record)) {
            return false;
        }
        return durationMinutes == record.durationMinutes
                && Double.compare(distanceKm, record.distanceKm) == 0
                && calories == record.calories
                && date.equals(record.date)
                && kind == record.kind
                && description.equals(record.description);
    }

    /** Повертає хеш-код, узгоджений із {@code equals}. */
    @Override
    public int hashCode() {
        return Objects.hash(date, kind, durationMinutes, distanceKm, calories, description);
    }

    /** Повертає ключ round-trip (дата + вид) і основні значення. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s/%s: %d хв, %.2f км", date, kind, durationMinutes, distanceKm);
    }
}
```

## @CsvColumn

@Retention(RUNTIME) залишає анотацію в класі під час виконання, тому рефлексія читає її через getAnnotation; @Target(FIELD) дозволяє ставити її лише над полями. Єдиний елемент String value() – заголовок стовпця. Анотація не виконує експорт – вона лише описує контракт. Порядок стовпців детермінований: сортування Java-імен полів (calories, date, description, distanceKm, durationMinutes, kind).

*Лістинг 3 – src/main/java/ua/lpnu/kzp/CsvColumn.java*

```text
package ua.lpnu.kzp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Позначає поле, яке експортується як CSV-стовпець. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /** @return заголовок CSV-стовпця */
    String value();
}
```

## Рефлексивний експортер

CsvExporter.write(Path, Class<T>, List<? extends T>) не знає жодного доменного класу: проходить ланцюжок суперкласів до Object, відбирає поля з @CsvColumn (getDeclaredFields + isAnnotationPresent), сортує їх, відкриває доступ trySetAccessible (відмова – DataStorageException), будує заголовок і рядки зі значень field.get(record). Клас без анотованих полів відхиляється з поясненням. Екранування: значення з комою, лапкою, \r або \n береться в подвійні лапки, внутрішні лапки подвоюються. Файл записується Files.writeString з UTF-8 і TRUNCATE_EXISTING. Універсальність перевірено тестом на іншому класі OtherRecord.

*Лістинг 4 – src/main/java/ua/lpnu/kzp/CsvExporter.java*

```text
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
```

*Лістинг 5 – src/main/java/ua/lpnu/kzp/CsvParser.java*

```text
package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/** Розбирає CSV-документ машиною станів із підтримкою лапок і перенесень рядка в полях. */
public final class CsvParser {
    /* Забороняє створення екземплярів статичного парсера. */
    private CsvParser() {
    }

    /**
     * Розбирає один CSV-запис.
     *
     * @param line текст одного запису
     * @return значення полів
     * @throws DataStorageException якщо текст містить не один запис або має помилку формату
     */
    public static List<String> parse(String line) throws DataStorageException {
        List<List<String>> records = parseDocument(line);
        if (records.size() != 1) {
            throw new DataStorageException("Очікується один CSV-рядок");
        }
        return records.get(0);
    }

    /**
     * Розбирає CSV-документ у список записів (не фізичних рядків).
     *
     * @param text увесь текст файла
     * @return записи, кожен - список значень полів
     * @throws DataStorageException якщо лапки незакриті або після них стоїть зайвий символ
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        if (text == null) {
            throw new DataStorageException("CSV-документ не може бути null");
        }
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;
        // quoted - поле всередині лапок; quoteClosed - лапки закрито й далі допустима
        // лише кома або кінець запису.
        for (int index = 0; index < text.length(); index++) {
            char symbol = text.charAt(index);
            if (symbol == '"' && !quoted && !quoteClosed && current.length() == 0) {
                quoted = true;
            } else if (symbol == '"' && quoted && index + 1 < text.length()
                    && text.charAt(index + 1) == '"') {
                current.append('"');
                index++;
            } else if (symbol == '"' && quoted) {
                quoted = false;
                quoteClosed = true;
            } else if (symbol == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
                quoteClosed = false;
            } else if ((symbol == '\r' || symbol == '\n') && !quoted) {
                fields.add(current.toString());
                records.add(List.copyOf(fields));
                fields = new ArrayList<>();
                current.setLength(0);
                quoteClosed = false;
                if (symbol == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                if (quoteClosed) {
                    throw new DataStorageException("Символ після закритих CSV-лапок у позиції " + index);
                }
                current.append(symbol);
            }
        }
        if (quoted) {
            throw new DataStorageException("Незакриті CSV-лапки");
        }
        if (!fields.isEmpty() || current.length() > 0) {
            fields.add(current.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }
}
```

## Формат CSV

Кодування UTF-8, роздільник – кома, перший рядок – заголовок з @CsvColumn:

*Лістинг 6 – Створений файл out/trainings.csv*

```text
калорії,дата,опис,дистанція,тривалість,вид
520,2026-09-01,"2026-09-01 біг: 45 хв, 8.50 км, 520 ккал",8.5,45,RUNNING
780,2026-09-02,"2026-09-02 велосипед: 90 хв, 32.00 км, 780 ккал",32.0,90,CYCLING
410,2026-09-03,"2026-09-03 плавання: 40 хв, 1.50 км, 410 ккал",1.5,40,SWIMMING
350,2026-09-05,"2026-09-05 біг: 30 хв, 8.50 км, 350 ккал",8.5,30,RUNNING
450,2026-09-06,"2026-09-06 тренажери: 60 хв, 0.00 км, 450 ккал",0.0,60,GYM
300,2026-09-08,"2026-09-08 ходьба: 75 хв, 6.80 км, 300 ккал",6.8,75,WALKING
820,2026-09-08,"2026-09-08 велосипед: 120 хв, 41.30 км, 820 ккал",41.3,120,CYCLING
610,2026-09-10,"2026-09-10 біг: 45 хв, 10.00 км, 610 ккал",10.0,45,RUNNING
```

Приклад екранованого рядка: поле опису 2026-09-01 біг: 45 хв, 8.50 км, 520 ккал містить коми й записане в лапках. Тест roundTripPreservesAllRecords додатково перевіряє значення з лапками та перенесенням рядка: "Парк ""Сихів"", 2 кола + новий рядок + другий рядок опису".

## Імпорт і round-trip

TrainingStorage.load читає весь файл Files.readString(path, UTF_8), розбирає його машиною станів CsvParser.parseDocument, яка відрізняє кому й перенесення рядка всередині та поза лапками і повертає список записів (а не фізичних рядків). Перший запис – заголовок; кожен наступний відновлюється TrainingRecord.fromFields(header, values), що знаходить значення за назвою стовпця, розбирає LocalDate, enum TrainingKind.valueOf, int і double. Порівняння repository.all().equals(restored) перевіряє поля, порядок, enum, дати, числа й текст з комами.

*Лістинг 7 – src/main/java/ua/lpnu/kzp/TrainingStorage.java*

```text
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
```

## Помилки

*Таблиця 2 – Рівні помилок*

| Рівень | Приклад | Реакція | Тест |
|---|---|---|---|
| Модель | від'ємна тривалість | IllegalArgumentException у конструкторі | TrainingTest |
| Формат | 2 поля замість 6 | DataStorageException «Очікується 6 полів…» | rejectsCorruptedRows |
| Значення | «багато» у числі, YOGA у enum | DataStorageException, cause – NumberFormat/IllegalArgument | rejectsCorruptedRows |
| CSV | незакриті лапки, символ після лапок | DataStorageException | parserRejectsBrokenQuotes |
| Файл | запис у каталог, читання відсутнього файла | DataStorageException, cause – IOException | wrapsFileErrorWithCause |
| Рефлексія | клас без @CsvColumn | DataStorageException | exporterIsNotBoundToDomain |

DataStorageException – checked-виняток із конструкторами (message) і (message, cause). Порожніх catch немає: виняток або передається з контекстом, або в Main перетворюється на повідомлення з причиною.

## Тестування

Перенесено 41 тест ЛР1–ЛР4 і додано 9 тестів StorageTest: сховище, пошук і незмінність знімка; анотація під час виконання та порядок стовпців; екранування коми, лапок і перенесення; round-trip із multiline-полем (6 CSV-записів = заголовок + 5, хоча фізичних рядків більше); порожній набір – лише заголовок; експорт іншого класу й клас без анотацій; хибна кількість полів, число й enum із перевіркою getCause(); помилки парсера; помилки файлової системи з IOException як cause.

*Лістинг 8 – src/test/java/ua/lpnu/kzp/StorageTest.java*

```text
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
```

*Лістинг 9 – Результат mvnw.cmd -B clean verify*

```text
[INFO] Building kzp-lab-andrushko 1.4.0
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.201 s -- in ua.lpnu.kzp.StorageTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.035 s -- in ua.lpnu.kzp.TrainingHierarchyTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.035 s -- in ua.lpnu.kzp.TrainingProcessorTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in ua.lpnu.kzp.TrainingQueriesTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in ua.lpnu.kzp.TrainingTest
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: C:\Users\user\kzp-labs\lab05\target\kzp-lab-andrushko-1.4.0.jar
[INFO] BugInstance size is 0
[INFO] No errors/warnings found
[INFO] BUILD SUCCESS
```

## Інфраструктура

Команди: mvnw.cmd test, mvnw.cmd verify, mvnw.cmd package, java -jar target/kzp-lab-andrushko-1.4.0.jar [--export out/trainings.csv]. CSV і target не додаються в репозиторій (.gitignore: target/, out/). JAR публікується як CI-артефакт:

GitHub Actions, запуск для PR #37: https://github.com/wwwattra/kzp-lab01-05-andrushko/actions/runs/39140240935

*Таблиця 3 – Результати CI на трьох ОС*

| Job | Результат |
|---|---|
| verify (windows-latest) | success ✔ |
| verify (macos-latest) | success ✔ |
| verify (ubuntu-latest) | success ✔ |

*Таблиця 4 – Артефакти workflow (JAR)*

| Артефакт | Розмір, КБ |
|---|---|
| jar-ubuntu-latest-18 | 62.2 |
| jar-macos-latest-18 | 62.2 |
| jar-windows-latest-18 | 62.3 |

Артефакти доступні для завантаження на сторінці запуску https://github.com/wwwattra/kzp-lab01-05-andrushko/actions/runs/39140240935 (розділ Artifacts).

## GitHub Issues і Pull Request

*Таблиця 5 – GitHub Issues → зміна → перевірка*

| Issue | Мітка | Зміна | Перевірка |
|---|---|---|---|
| #31 Repository<T> | feature | add, all, find | repositoryStores… |
| #32 Анотація @CsvColumn | feature | RUNTIME, FIELD | headerComesFromAnnotations… |
| #33 Рефлексивний експортер | feature | CsvExporter, escape | escapes…, exporterIsNotBound… |
| #34 Парсер і round-trip | feature | CsvParser, TrainingStorage | roundTripPreserves… |
| #35 Власний виняток | feature | DataStorageException з cause | wrapsFileErrorWithCause |
| #36 Документація ЛР5 | docs | README, javadoc, REPORT | рев'ю PR |

Pull Request #37 «ЛР5: Repository<T>, @CsvColumn, CSV-експорт і round-trip» (https://github.com/wwwattra/kzp-lab01-05-andrushko/pull/37) містить опис змін, результати перевірок і рядки Closes для Issues #31, #32, #33, #34, #35, #36. Після зеленого CI PR злито в main (merge commit), Issues закрилися автоматично, на коміт злиття поставлено тег v1.4.0.

## Порівняння стану

*Лістинг 10 – Вивід версії 1.4.0*

```text
Звіт: спортивні тренування
Показник                             Значення
Коректних записів                           8
Сумарний час, хв                          505
Сумарна дистанція, км                  108.60
Найбільша витрата калорій, ккал           820
Пропущено рядків: 5
Рядок 6: порожній рядок
Рядок 10: від'ємне числове значення
Рядок 11: порожня дата або вид тренування
Рядок 12: числове поле має помилковий формат
Рядок 13: очікується 5 полів, отримано 4
Навантаження (поліморфний розрахунок load), бали
  біг (надворі)                        349.00
  велосипед (надворі)                  589.90
  ходьба (надворі)                     125.40
  плавання (у приміщенні)               89.00
  тренажери (у приміщенні)             117.00
  Усього                              1270.30
Унікальних тренувань (HashSet): 8
Запити Stream API
1. Тривалість >= 45 хв: 6 записів
   2026-09-01 біг: 45 хв, 8.50 км, 520 ккал
   2026-09-02 велосипед: 90 хв, 32.00 км, 780 ккал
   2026-09-06 тренажери: 60 хв, 0.00 км, 450 ккал
   2026-09-08 ходьба: 75 хв, 6.80 км, 300 ккал
   2026-09-08 велосипед: 120 хв, 41.30 км, 820 ккал
   2026-09-10 біг: 45 хв, 10.00 км, 610 ккал
2. Дистанції, км: [8.50, 32.00, 1.50, 8.50, 0.00, 6.80, 41.30, 10.00]
3. Сумарна дистанція за видом: {біг=27.00, велосипед=73.30, ходьба=6.80, плавання=1.50, тренажери=0.00}
4. Статистика калорій: count=8, sum=4240, min=300, max=820, avg=530.00
5. Top-5 за дистанцією (спадання), потім за датою:
   2026-09-08 велосипед: 120 хв, 41.30 км, 820 ккал
   2026-09-02 велосипед: 90 хв, 32.00 км, 780 ккал
   2026-09-10 біг: 45 хв, 10.00 км, 610 ккал
   2026-09-01 біг: 45 хв, 8.50 км, 520 ккал
   2026-09-05 біг: 30 хв, 8.50 км, 350 ккал
Пошук за датою 2026-09-08: 2026-09-08 ходьба: 75 хв, 6.80 км, 300 ккал
Пошук за датою 2026-09-30: не знайдено
Звіт записано у out\report.txt
CSV-експорт: out\trainings.csv
Заголовок: калорії,дата,опис,дистанція,тривалість,вид
Записів у сховищі: 8, відновлено: 8
Велосипедних записів (Repository.find): 2
Round-trip: true
```

Усі розділи звіту попередніх робіт збереглися без змін; наприкінці додано результати експорту: заголовок, кількість записів у сховищі та відновлених (8 = 8), результат Repository.find і Round-trip: true.

## Академічна доброчесність

Заповніть самостійно: використані інструменти ШІ або твердження, що вони не використовувалися; ролі, запити, прийняті й відхилені рекомендації; власний внесок і підтвердження розуміння всього поданого коду.

## Відповіді на контрольні питання

1. Яку проблему розв'язує параметр типу в Repository<T>?

Одне сховище працює з будь-яким типом, а компілятор гарантує, що в Repository<TrainingRecord> потрапляють лише записи тренувань – без приведень.

2. Чим параметризований клас відрізняється від raw type?

Raw type Repository вимикає перевірку типів: можна додати будь-який Object і отримати ClassCastException під час виконання.

3. Навіщо Predicate<? super T> у методі пошуку?

Щоб приймати умови для T або його супертипу, наприклад Predicate<Object>.

4. Чому all() має повертати незмінний знімок?

Зовнішній код не повинен змінювати внутрішній список сховища в обхід add; спроба clear() дає UnsupportedOperationException (перевірено тестом).

5. Що таке checked exception і коли він доречний у цій роботі?

Виняток, який компілятор вимагає обробити або оголосити в throws. Доречний для очікуваних помилок збереження й формату – DataStorageException.

6. Для чого зберігати cause власного винятку?

Щоб не загубити першопричину (IOException, NumberFormatException) і мати повну діагностику; тести перевіряють getCause().

7. Чим Path і Files кращі за складання шляху рядком?

Path сам обирає роздільник ОС, а Files дає атомарні операції з явним кодуванням і зрозумілими винятками.

8. Чому кодування потрібно задавати і під час запису, і під час читання?

Обидві сторони мають використати однакове кодування; інакше українські символи зіпсуються.

9. Що роблять @Retention(RUNTIME) і @Target(FIELD)?

Перший зберігає анотацію для рефлексії під час виконання, другий дозволяє ставити її лише над полями.

10. Чому анотація не виконує експорт сама?

Анотація – лише метадані; поведінку реалізує експортер, який їх читає.

11. Чим getDeclaredFields() відрізняється від отримання публічних полів?

getDeclaredFields повертає всі поля класу, зокрема private, але без успадкованих; getFields – лише public, включно з успадкованими.

12. Навіщо перевіряти trySetAccessible()?

Доступ до private-поля може бути заборонено модулями; метод повертає false замість винятку, і ми повідомляємо про проблему власним винятком.

13. Чому порядок полів для CSV потрібно зробити детермінованим?

Порядок getDeclaredFields не гарантований специфікацією; без сортування заголовок міг би відрізнятися між JVM.

14. Чому split(",") не підходить для загального CSV?

Він розірве поле "2026-09-01 біг: 45 хв, 8.50 км" по комі всередині лапок.

15. Як екрануються кома, лапка й перенесення рядка в CSV?

Значення береться в подвійні лапки, а кожна внутрішня лапка подвоюється.

16. Чому порожній список потребує окремого параметра Class<T>?

Через стирання типів список не знає свого T, а без записів неможливо взяти клас першого елемента; заголовок будується з переданого класу.

17. Чим універсальний експортер відрізняється від предметного fromCsv?

Експортер працює з будь-яким класом через рефлексію, а fromFields знає типи полів свого домену (LocalDate, enum) і їхні правила.

18. Які рівні помилок розрізняє програма?

Модель, формат, значення, CSV-синтаксис, файл і рефлексія (див. таблицю).

19. Що доводить тест round-trip?

Що запис і читання не втрачають і не спотворюють даних: відновлений список дорівнює початковому за всіма полями й порядком.

20. Чому виняток не можна поглинати порожнім catch?

Помилка зникне без сліду, а програма продовжить роботу з неправильним станом, наприклад, із порожнім списком замість пошкодженого файла.

## Висновки

Java-продукт отримав параметризоване сховище, власний виняток із причиною, анотацію CSV-стовпців, універсальний рефлексивний експортер і документний парсер. Дані записуються й читаються в UTF-8, спеціальні символи екрануються, round-trip повертає рівні об'єкти. 50 тестів проходять, SpotBugs без зауважень. Створений CSV є узгодженим вхідним текстовим файлом для наступного (Python) програмного блоку.

