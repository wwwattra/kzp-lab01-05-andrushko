# Звіт до лабораторної роботи №1

з дисципліни «Кросплатформні засоби програмування», тема «Розгортання Java-проєкту та базова обробка даних». Варіант 2. Спортивні тренування.
Виконав: студент групи КІ-302 Андрушко А. І. Прийняв: Олексів М. В.

## Тема, номер і варіант

Лабораторна робота № 1 «Розгортання Java-проєкту та базова обробка даних». Варіант 2 – «Спортивні тренування». Операційна система розробки – Windows 10 (64-bit), JDK – Eclipse Temurin 21.0.12, Maven 3.9.11 (через Maven Wrapper 3.3.4).

Посилання на GitHub-репозиторій: https://github.com/wwwattra/kzp-lab01-05-andrushko. Гілка lab01, Pull Request #9 (https://github.com/wwwattra/kzp-lab01-05-andrushko/pull/9), Git-тег v1.0.0 (https://github.com/wwwattra/kzp-lab01-05-andrushko/tree/v1.0.0).

## Мета роботи

Пройти повний шлях від створення репозиторію та налаштування інфраструктури до першої версії консольної програми, яку можна відтворено зібрати й запустити на Windows, macOS та Ubuntu. У результаті створено Maven-проєкт із JUnit 5, статичним аналізатором SpotBugs, виконуваним JAR (maven-shade-plugin), Maven Wrapper і робочим процесом GitHub Actions для трьох ОС; реалізовано програму, що читає журнал тренувань, перевіряє записи, обчислює чотири показники варіанта та виводить звіт у консоль і файл.

## Постановка задачі

Вхідний файл data/input.csv у кодуванні UTF-8 містить по одному запису в рядку, поля розділено крапкою з комою. Формат запису варіанта 2:

```text
date:String; kind:String; minutes:int; distanceKm:double; calories:int
```

Показники звіту: 1) кількість коректних записів; 2) сумарний час тренувань, хв; 3) сумарна дистанція, км; 4) найбільша витрата калорій, ккал. Програма підтримує режими --help, --version, звичайний запуск, а також аргументи --input <файл> і --output <файл> (типово data/input.csv та out/report.txt).

Перевірки рядка: порожній рядок; кількість полів (рівно 5); порожня дата або вид; нечислове значення в числовому полі; від'ємні хвилини, дистанція або калорії; нескінченна дистанція; файл без жодного коректного запису. Хибний рядок не зупиняє програму – формується повідомлення з номером рядка та причиною.

## Структура програми

*Таблиця 1 – Файли проєкту*

| Файл | Призначення |
|---|---|
| pom.xml | координати ua.lpnu.kzp:kzp-lab-andrushko:1.0.0, Java 21, UTF-8, JUnit, SpotBugs, Shade |
| mvnw, mvnw.cmd, .mvn/wrapper/ | Maven Wrapper для відтворюваної збірки |
| src/main/java/ua/lpnu/kzp/Main.java | точка входу, розбір аргументів, вивід у консоль і файл |
| src/main/java/ua/lpnu/kzp/TrainingProcessor.java | перевірка рядків, обчислення показників, форматування звіту |
| src/main/java/ua/lpnu/kzp/FileReport.java | читання й запис файлів через Path і явний UTF-8 |
| src/test/java/ua/lpnu/kzp/TrainingProcessorTest.java | 10 тестів JUnit 5 |
| data/input.csv | приклад даних: 8 коректних і 5 хибних рядків |
| .github/workflows/ci.yml | CI: verify на трьох ОС і публікація JAR |
| .github/ISSUE_TEMPLATE/*.yml, PULL_REQUEST_TEMPLATE.md | форми Issues і чек-лист PR |
| .gitignore, .gitattributes, .editorconfig, README.md | службові правила й документація |

Потік даних: FileReport.readLines (UTF-8) → TrainingProcessor.splitValid (кожен рядок проходить validate: прочитати → перевірити → перетворити) → computeStats (масив із чотирьох показників) → formatReport (один текст звіту з Locale.ROOT) → вивід у консоль і FileReport.writeReport. Читання, обчислення та форматування розділено на окремі методи, щоб у ЛР2 замінити рядки й масиви власними класами.

*Лістинг 1 – src/main/java/ua/lpnu/kzp/TrainingProcessor.java*

```text
package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Перевіряє рядки про спортивні тренування, обчислює показники та формує звіт.
 *
 * <p>Формат рядка: {@code date;kind;minutes;distanceKm;calories}.
 */
public final class TrainingProcessor {
    /** Кількість полів у коректному рядку. */
    public static final int FIELD_COUNT = 5;

    /** Індекс показника «кількість коректних записів» у масиві статистики. */
    public static final int COUNT = 0;
    /** Індекс показника «сумарний час, хв». */
    public static final int TOTAL_MINUTES = 1;
    /** Індекс показника «сумарна дистанція, км». */
    public static final int TOTAL_DISTANCE = 2;
    /** Індекс показника «найбільша витрата калорій». */
    public static final int MAX_CALORIES = 3;

    /* Забороняє створення екземплярів службового класу. */
    private TrainingProcessor() {
    }

    /**
     * Перевіряє один рядок і повертає причину помилки.
     *
     * @param line вхідний рядок
     * @return текст причини або {@code null}, якщо рядок коректний
     */
    public static String validate(String line) {
        if (line == null || line.isBlank()) {
            return "порожній рядок";
        }
        // -1 зберігає порожні поля наприкінці рядка.
        String[] fields = line.split(";", -1);
        if (fields.length != FIELD_COUNT) {
            return "очікується %d полів, отримано %d".formatted(FIELD_COUNT, fields.length);
        }
        if (fields[0].isBlank() || fields[1].isBlank()) {
            return "порожня дата або вид тренування";
        }
        try {
            int minutes = Integer.parseInt(fields[2].trim());
            double distance = Double.parseDouble(fields[3].trim());
            int calories = Integer.parseInt(fields[4].trim());
            if (minutes < 0 || distance < 0 || calories < 0) {
                return "від'ємне числове значення";
            }
            if (!Double.isFinite(distance)) {
                return "дистанція має бути скінченним числом";
            }
        } catch (NumberFormatException exception) {
            return "числове поле має помилковий формат";
        }
        return null;
    }

    /**
     * Обчислює чотири показники варіанта за коректними рядками.
     *
     * @param validLines рядки, що пройшли {@link #validate(String)}
     * @return масив {кількість, сумарний час, сумарна дистанція, максимум калорій}
     */
    public static double[] computeStats(List<String> validLines) {
        double[] stats = new double[4];
        for (String line : validLines) {
            String[] fields = line.split(";", -1);
            stats[COUNT]++;
            stats[TOTAL_MINUTES] += Integer.parseInt(fields[2].trim());
            stats[TOTAL_DISTANCE] += Double.parseDouble(fields[3].trim());
            stats[MAX_CALORIES] = Math.max(stats[MAX_CALORIES], Integer.parseInt(fields[4].trim()));
        }
        return stats;
    }

    /**
     * Розділяє вхідні рядки на коректні та повідомлення про помилки.
     *
     * @param lines усі рядки файла
     * @param valid список, до якого додаються коректні рядки
     * @return повідомлення про пропущені рядки з номером і причиною
     */
    public static List<String> splitValid(List<String> lines, List<String> valid) {
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String reason = validate(lines.get(index));
            if (reason == null) {
                valid.add(lines.get(index));
            } else {
                errors.add("Рядок %d: %s".formatted(index + 1, reason));
            }
        }
        return errors;
    }

    /**
     * Формує текст звіту, незалежний від локалі операційної системи.
     *
     * @param stats показники з {@link #computeStats(List)}
     * @param errors повідомлення про пропущені рядки
     * @return готовий текст звіту
     */
    public static String formatReport(double[] stats, List<String> errors) {
        StringBuilder report = new StringBuilder();
        String nl = System.lineSeparator();
        report.append("Звіт: спортивні тренування").append(nl);
        report.append(String.format(Locale.ROOT, "%-34s %10s%n", "Показник", "Значення"));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n",
                "Коректних записів", (long) stats[COUNT]));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n",
                "Сумарний час, хв", (long) stats[TOTAL_MINUTES]));
        report.append(String.format(Locale.ROOT, "%-34s %10.2f%n",
                "Сумарна дистанція, км", stats[TOTAL_DISTANCE]));
        report.append(String.format(Locale.ROOT, "%-34s %10d%n",
                "Найбільша витрата калорій, ккал", (long) stats[MAX_CALORIES]));
        report.append(String.format(Locale.ROOT, "Пропущено рядків: %d%n", errors.size()));
        for (String error : errors) {
            report.append(error).append(nl);
        }
        return report.toString();
    }
}
```

*Лістинг 2 – src/main/java/ua/lpnu/kzp/Main.java*

```text
package ua.lpnu.kzp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Консольна програма обробки журналу спортивних тренувань (варіант 2). */
public final class Main {
    /** Версія програми, узгоджена з pom.xml і Git-тегом. */
    public static final String VERSION = "1.0.0";

    /* Забороняє створення екземплярів класу точки входу. */
    private Main() {
    }

    /**
     * Точка входу до програми.
     *
     * @param args {@code --help}, {@code --version}, {@code --input <файл>}, {@code --output <файл>}
     */
    public static void main(String[] args) {
        // Потік із явним UTF-8 закривається try-with-resources (виправлення SpotBugs OS_OPEN_STREAM).
        try (PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, StandardCharsets.UTF_8)) {
            run(args, out);
        }
    }

    /**
     * Розбирає аргументи, обробляє файл і виводить звіт у переданий потік.
     *
     * @param args аргументи командного рядка
     * @param out потік для повідомлень користувачу
     */
    static void run(String[] args, PrintStream out) {
        Path input = Path.of("data", "input.csv");
        Path output = Path.of("out", "report.txt");
        for (int index = 0; index < args.length; index++) {
            if ("--help".equals(args[index])) {
                out.printf("Використання: java -jar kzp-lab-andrushko.jar "
                        + "[--help] [--version] [--input <файл>] [--output <файл>]%n");
                return;
            } else if ("--version".equals(args[index])) {
                String build = System.getenv("GITHUB_RUN_NUMBER");
                out.printf("kzp-lab-andrushko %s%s%n", VERSION,
                        build == null ? "" : " (CI build " + build + ")");
                return;
            } else if ("--input".equals(args[index]) && index + 1 < args.length) {
                input = Path.of(args[++index]);
            } else if ("--output".equals(args[index]) && index + 1 < args.length) {
                output = Path.of(args[++index]);
            } else {
                out.printf("Невідомий аргумент: %s%n", args[index]);
                return;
            }
        }
        try {
            List<String> lines = FileReport.readLines(input);
            List<String> valid = new ArrayList<>();
            List<String> errors = TrainingProcessor.splitValid(lines, valid);
            if (valid.isEmpty()) {
                out.printf("У файлі %s немає жодного коректного запису%n", input);
            }
            String report = TrainingProcessor.formatReport(
                    TrainingProcessor.computeStats(valid), errors);
            out.print(report);
            FileReport.writeReport(output, report);
            out.printf("Звіт записано у %s%n", output);
        } catch (IOException exception) {
            out.printf("Помилка роботи з файлом: %s%n", exception.getMessage());
        }
    }
}
```

*Лістинг 3 – src/main/java/ua/lpnu/kzp/FileReport.java*

```text
package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Ізолює кросплатформне читання та запис текстових файлів. */
public final class FileReport {
    /* Забороняє створення екземплярів службового класу. */
    private FileReport() {
    }

    /**
     * Читає рядки у фіксованому кодуванні UTF-8.
     *
     * @param input шлях до вхідного файла
     * @return рядки файла
     * @throws IOException якщо файл неможливо прочитати
     */
    public static List<String> readLines(Path input) throws IOException {
        // Явне кодування не залежить від налаштувань операційної системи.
        return Files.readAllLines(input, StandardCharsets.UTF_8);
    }

    /**
     * Створює каталог призначення і записує звіт.
     *
     * @param output шлях до файла звіту
     * @param report готовий текст звіту
     * @throws IOException якщо каталог або файл неможливо створити
     */
    public static void writeReport(Path output, String report) throws IOException {
        // Після клонування репозиторію каталог out може ще не існувати.
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}
```

## Інфраструктура

pom.xml задає координати артефакта, maven.compiler.release=21 і кодування UTF-8. JUnit 5.12.2 підключено зі scope=test, тому бібліотека не потрапляє у JAR і не копіюється в репозиторій. SpotBugs 4.9.7.0 прив'язано до фази verify (ціль check), maven-shade-plugin 3.6.0 – до фази package, у маніфесті вказано ua.lpnu.kzp.Main. Maven Wrapper згенеровано командою mvn wrapper:wrapper; він завантажує Maven 3.9.11.

*Лістинг 4 – pom.xml*

```text
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>ua.lpnu.kzp</groupId>
    <artifactId>kzp-lab-andrushko</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.12.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>
            <plugin>
                <groupId>com.github.spotbugs</groupId>
                <artifactId>spotbugs-maven-plugin</artifactId>
                <version>4.9.7.0</version>
                <executions>
                    <execution>
                        <phase>verify</phase>
                        <goals><goal>check</goal></goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>ua.lpnu.kzp.Main</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Робочий процес GitHub Actions запускає verify у матриці ubuntu-latest, windows-latest, macos-latest (на Unix – ./mvnw, на Windows – mvnw.cmd), кешує залежності Maven і публікує JAR як артефакт jar-<ос>-<номер запуску>. Номер запуску GITHUB_RUN_NUMBER також виводиться програмою в режимі --version.

*Лістинг 5 – .github/workflows/ci.yml*

```text
# Один workflow перевіряє однаковий commit на трьох операційних системах.
name: CI

on:
  push:
  pull_request:

permissions:
  contents: read

jobs:
  verify:
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Show CI build number
        run: echo "CI build ${{ github.run_number }}"

      - name: Verify on Unix
        if: runner.os != 'Windows'
        run: |
          chmod +x mvnw
          ./mvnw -B verify

      - name: Verify on Windows
        if: runner.os == 'Windows'
        run: .\mvnw.cmd -B verify

      - name: Upload JAR
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: jar-${{ matrix.os }}-${{ github.run_number }}
          path: target/*.jar
          if-no-files-found: error
```

## GitHub Issues і Pull Request

Роботу сплановано й виконано через такі GitHub Issues (номери присвоєно GitHub):

*Таблиця 2 – GitHub Issues → зміна → перевірка*

| Issue | Мітка | Зміна | Перевірка |
|---|---|---|---|
| #1 Налаштування лабораторної роботи №1 | infra | структура каталогів, службові файли | README, .gitignore |
| #2 Налаштувати Maven-проєкт | infra | pom.xml, Maven Wrapper | mvnw.cmd test |
| #3 Налаштувати аналізатор і пакування | infra | SpotBugs у verify, Shade | mvnw.cmd verify, JAR |
| #4 Налаштувати GitHub Actions | infra | ci.yml, матриця 3 ОС | зелений CI |
| #5 Реалізувати читання й перевірку записів | feature | TrainingProcessor.validate | тести хибних рядків |
| #6 Сформувати звіт | feature | formatReport, FileReport | консоль + out/report.txt |
| #7 [bug] SpotBugs OS_OPEN_STREAM у Main | bug | PrintStream у try-with-resources | verify без помилок |
| #8 Додати документацію | docs | README, javadoc, REPORT | рев'ю PR |

Pull Request #9 «ЛР1: Maven-проєкт і базова обробка журналу тренувань» (https://github.com/wwwattra/kzp-lab01-05-andrushko/pull/9) містить опис змін, результати перевірок і рядки Closes для Issues #1, #2, #3, #4, #5, #6, #7, #8. Після зеленого CI PR злито в main (merge commit), Issues закрилися автоматично, на коміт злиття поставлено тег v1.0.0.

Знайдений дефект. Під час першого запуску mvnw.cmd verify SpotBugs зупинив збірку:

*Лістинг 6 – Перший запуск verify*

```text
[ERROR] Medium: ua.lpnu.kzp.Main.main(String[]) may fail to close stream [ua.lpnu.kzp.Main] At Main.java:[line 27] OS_OPEN_STREAM
[INFO] BUILD FAILURE
```

Кроки відтворення: виконати mvnw.cmd verify. Очікуваний результат: BUILD SUCCESS. Фактичний результат: BUILD FAILURE через незакритий PrintStream. Виправлення: потік створюється в try-with-resources, а логіка програми винесена в метод run(args, out). Після виправлення SpotBugs повідомляє «No errors/warnings found».

## Приклади роботи

*Лістинг 7 – Вхідні дані data/input.csv*

```text
2026-09-01;біг;45;8.50;520
2026-09-02;велосипед;90;32.00;780
2026-09-03;плавання;40;1.50;410
2026-09-05;біг;30;8.50;350
2026-09-06;тренажери;60;0.00;450

2026-09-08;ходьба;75;6.80;300
2026-09-08;велосипед;120;41.30;820
2026-09-10;біг;45;10.00;610
2026-09-11;біг;-20;4.00;300
2026-09-12;;50;6.00;400
2026-09-13;плавання;сорок;1.20;380
2026-09-14;біг;40;7.00
```

Рядок 6 порожній; рядки 10–13 містять від'ємне значення, порожній вид, нечислове поле «сорок» та неправильну кількість полів.

*Лістинг 8 – Консольний вивід java -jar target/kzp-lab-andrushko-1.0.0.jar*

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
Звіт записано у out\report.txt
```

*Лістинг 9 – Режим --help*

```text
Використання: java -jar kzp-lab-andrushko.jar [--help] [--version] [--input <файл>] [--output <файл>]
```

*Лістинг 10 – Режим --version*

```text
kzp-lab-andrushko 1.0.0
```

Файл out/report.txt містить той самий текст звіту, що й консоль (без останнього рядка «Звіт записано у …»), бо обидва приймачі отримують один рядок, сформований formatReport. Перевірка вручну: 45 + 90 + 40 + 30 + 60 + 75 + 120 + 45 = 505 хв; 8.5 + 32 + 1.5 + 8.5 + 0 + 6.8 + 41.3 + 10 = 108.6 км; максимум калорій – 820.

## Тестування

Виконані команди: mvnw.cmd test, mvnw.cmd verify, mvnw.cmd package, запуск JAR у трьох режимах. Тести покривають коректний рядок з українським текстом, порожній рядок, неправильну кількість полів, нечислове поле, від'ємне значення, порожній вид, обчислення сум і максимуму для кількох записів, файл без коректних записів, форматування з крапкою незалежно від локалі та запис/читання UTF-8 через тимчасовий каталог.

*Лістинг 11 – Результат mvnw.cmd -B clean verify*

```text
[INFO] Building kzp-lab-andrushko 1.0.0
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.143 s -- in ua.lpnu.kzp.TrainingProcessorTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: C:\Users\user\kzp-labs\lab01\target\kzp-lab-andrushko-1.0.0.jar
[INFO] BugInstance size is 0
[INFO] No errors/warnings found
[INFO] BUILD SUCCESS
```

*Лістинг 12 – src/test/java/ua/lpnu/kzp/TrainingProcessorTest.java*

```text
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
```

GitHub Actions, запуск для PR #9: https://github.com/wwwattra/kzp-lab01-05-andrushko/actions/runs/39139529780

*Таблиця 3 – Результати CI на трьох ОС*

| Job | Результат |
|---|---|
| verify (macos-latest) | success ✔ |
| verify (windows-latest) | success ✔ |
| verify (ubuntu-latest) | success ✔ |

*Таблиця 4 – Артефакти workflow (JAR)*

| Артефакт | Розмір, КБ |
|---|---|
| jar-ubuntu-latest-2 | 13.6 |
| jar-windows-latest-2 | 13.6 |
| jar-macos-latest-2 | 13.6 |

Артефакти доступні для завантаження на сторінці запуску https://github.com/wwwattra/kzp-lab01-05-andrushko/actions/runs/39139529780 (розділ Artifacts).

## Документація

Javadoc мають публічні класи Main, TrainingProcessor, FileReport, метод main, run, усі публічні методи (validate, splitValid, computeStats, formatReport, readLines, writeReport) і константи індексів показників. README.md описує призначення, формат входу, команди збирання та запуску.

## Академічна доброчесність

Заповніть самостійно: використані інструменти ШІ або твердження, що вони не використовувалися; ролі, запити, прийняті й відхилені рекомендації; власний внесок і підтвердження розуміння всього поданого коду.

## Відповіді на контрольні питання

1. Яке призначення файла pom.xml?

Це опис проєкту для Maven: координати артефакта (groupId, artifactId, version), версія Java, кодування, залежності та плагіни збірки. Maven нічого не вгадує з каталогів – усе, що потрібно для збирання, тестування й пакування, задано в pom.xml.

2. Чим відрізняються фази test, verify і package у Maven?

test компілює код і запускає модульні тести. package після тестів створює JAR. verify іде після package і виконує додаткові перевірки якості – у моєму проєкті SpotBugs. Виклик пізнішої фази автоматично виконує всі попередні.

3. Навіщо потрібен Maven Wrapper?

Щоб будь-хто міг зібрати проєкт однією і тією ж версією Maven без глобальної установки. Скрипти mvnw/mvnw.cmd завантажують версію з maven-wrapper.properties (3.9.11) під час першого запуску. JDK при цьому все одно потрібен.

4. Яку роль відіграє метод main?

Це точка входу: JVM викликає public static void main(String[] args) під час запуску класу або JAR. У моїй програмі main створює UTF-8 потік виводу й передає аргументи методу run.

5. Чим примітивний тип відрізняється від String?

Примітив (int, double, boolean) зберігає саме значення і не може бути null. String – посилальний тип: змінна містить посилання на незмінний об'єкт рядка, має методи (split, isBlank) і може бути null.

6. Чому середнє потрібно обчислювати в double, навіть якщо сума й кількість мають тип int?

Ділення int на int – цілочислове й відкидає дробову частину: 7 / 2 = 3. Приведення (double) total / count дає 3.5.

7. Що станеться, якщо Integer.parseInt отримати рядок "abc"?

Буде кинуто NumberFormatException. У моїй програмі цей виняток перехоплюється для одного рядка, і формується повідомлення «Рядок 12: числове поле має помилковий формат».

8. Чому не можна мовчки пропускати рядок із помилкою?

Користувач не дізнається, що дані втрачено, а показники стануть неправильними без пояснень. Тому звіт містить номер рядка й причину пропуску.

9. Як String.split(";", -1) обробляє порожнє останнє поле?

Від'ємний ліміт зберігає порожні поля наприкінці, тому a;b; дає 3 елементи. Без -1 Java відкинула б порожній хвіст, і рядок із порожнім останнім полем виглядав би як рядок з меншою кількістю полів.

10. Чому для шляхів краще використовувати Path.of, а не "data\\input.csv"?

Path.of("data", "input.csv") сам підставляє роздільник поточної ОС. Рядок із зворотною скісною рискою працює лише на Windows.

11. Навіщо явно задавати StandardCharsets.UTF_8?

Стандартне кодування залежить від ОС (на Windows часто windows-1251 або 1252). Без явного UTF-8 українські символи прочиталися б неправильно.

12. Чим %n відрізняється від \n у форматованому виводі?

%n підставляє роздільник рядка поточної ОС (CRLF на Windows, LF на Unix), а \n – завжди LF.

13. Які дані потрібно перевірити у вашому варіанті?

Кількість полів (5), непорожні дату й вид, числові хвилини/дистанцію/калорії, невід'ємні значення, скінченну дистанцію, порожні рядки та наявність хоча б одного коректного запису.

14. Які тести виявлять помилку в обчисленні середнього?

Тест на кілька записів, у якому сума не ділиться націло на кількість (очікуване значення з дробовою частиною), а також тест на порожній набір (ділення на нуль). У моєму варіанті середнього немає, тому аналогічно перевіряються суми й максимум.

15. Яке призначення статичного аналізатора?

Він шукає типові дефекти без запуску програми. SpotBugs аналізує байт-код і в цій роботі знайшов реальну проблему – незакритий потік OS_OPEN_STREAM.

16. Яку роль виконує GitHub Actions?

Автоматично запускає ту саму команду verify на Windows, macOS та Ubuntu для кожного push і PR, тобто виконує continuous integration, і публікує JAR як артефакт.

17. Що має містити GitHub Issue на дефект?

Опис, кроки відтворення, очікуваний і фактичний результати. Форма bug.yml робить ці поля обов'язковими.

18. Що Ви повинні вказати у розділі звіту про академічну доброчесність?

Які інструменти ШІ використовувалися (або що не використовувалися), ролі, запити, прийняті рекомендації, виправлені помилки та підтвердження розуміння коду.

19. Яку роботу виконує агент DevOps, а яку – агент Валідатор?

DevOps консультує щодо pom.xml, Wrapper, аналізатора, JAR і Actions та допомагає діагностувати помилки збірки. Валідатор складає крайові випадки, перевіряє результати й оформлює дефекти як Issues.

20. Який рядок вашої програми найскладніше пояснити і чому?

line.split(";", -1): без розуміння другого аргументу неочевидно, чому рядок 2026-09-14;біг;40;7.00; дав би 5 полів, а без -1 – лише 4.

## Висновки

Створено публічний за структурою Maven-проєкт із Wrapper, JUnit 5, SpotBugs, виконуваним JAR і CI на трьох ОС. Програма коректно обробляє 8 записів журналу тренувань, відкидає 5 хибних рядків із поясненням і формує однаковий звіт у консоль та файл. Усі 10 тестів проходять, SpotBugs не має зауважень після виправлення знайденого дефекту. Масиви рядків і паралельні змінні цієї версії стануть основою для ЛР2, де їх буде замінено класом-сутністю та record.

