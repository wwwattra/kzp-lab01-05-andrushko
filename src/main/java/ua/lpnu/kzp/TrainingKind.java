package ua.lpnu.kzp;

/** Фіксований набір видів тренувань із підписом для звіту та місцем проведення. */
public enum TrainingKind {
    /** Біг на відкритому повітрі. */
    RUNNING("біг", true),
    /** Велосипедна поїздка. */
    CYCLING("велосипед", true),
    /** Піша прогулянка. */
    WALKING("ходьба", true),
    /** Плавання в басейні. */
    SWIMMING("плавання", false),
    /** Заняття на тренажерах у залі. */
    GYM("тренажери", false);

    private final String label;
    private final boolean outdoor;

    /* Конструктор enum використовується JVM для створення фіксованих констант. */
    TrainingKind(String label, boolean outdoor) {
        this.label = label;
        this.outdoor = outdoor;
    }

    /** @return текстове подання виду, яке зберігається у CSV і звіті */
    public String label() {
        return label;
    }

    /** @return {@code true}, якщо тренування проходить на відкритому повітрі */
    public boolean isOutdoor() {
        return outdoor;
    }

    /**
     * Знаходить вид за підписом із CSV.
     *
     * @param label підпис, наприклад {@code біг}
     * @return відповідна константа
     * @throws IllegalArgumentException якщо такого виду немає
     */
    public static TrainingKind fromLabel(String label) {
        for (TrainingKind kind : values()) {
            if (kind.label.equals(label)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("невідомий вид тренування: %s".formatted(label));
    }
}
