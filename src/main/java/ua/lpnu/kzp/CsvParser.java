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
