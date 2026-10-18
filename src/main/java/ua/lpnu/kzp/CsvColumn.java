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
