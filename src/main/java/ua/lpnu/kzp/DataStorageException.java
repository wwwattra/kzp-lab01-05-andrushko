package ua.lpnu.kzp;

/** Повідомляє про помилку зберігання, формату CSV або рефлексії. */
public class DataStorageException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток із повідомленням.
     *
     * @param message опис операції, що не вдалася
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Створює виняток із повідомленням і першопричиною.
     *
     * @param message опис операції, що не вдалася
     * @param cause початкова технічна помилка
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
