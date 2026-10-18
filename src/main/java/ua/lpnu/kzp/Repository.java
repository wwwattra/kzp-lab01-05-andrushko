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
