package ua.lpnu.kzp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Консольна програма обробки журналу спортивних тренувань (варіант 2). */
public final class Main {
    /** Версія програми, узгоджена з pom.xml і Git-тегом. */
    public static final String VERSION = "1.3.0";

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
            List<Training> trainings = new ArrayList<>();
            List<String> errors = TrainingProcessor.parseAll(lines, trainings);
            if (trainings.isEmpty()) {
                out.printf("У файлі %s немає жодного коректного запису%n", input);
            }
            String report = TrainingProcessor.formatReport(DurationDistance.of(trainings), errors)
                    + TrainingProcessor.formatLoadSection(trainings)
                    + TrainingProcessor.formatQueriesSection(trainings,
                            List.of(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 30)));
            out.print(report);
            FileReport.writeReport(output, report);
            out.printf("Звіт записано у %s%n", output);
        } catch (IOException exception) {
            out.printf("Помилка роботи з файлом: %s%n", exception.getMessage());
        }
    }
}
