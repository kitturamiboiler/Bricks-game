package game.bricks.domain.item;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

public final class Inventory {

    private final ArrayDeque<ItemType> items = new ArrayDeque<>();

    public boolean add(ItemType type) {
        Objects.requireNonNull(type);

        if (items.size() == 3)
            return false;
        items.addLast(type);

        return true;
    }

    public ItemType first() {
        return items.peekFirst();
    }

    public ItemType consume() {
        return items.pollFirst();
    }

    public List<ItemType> items() {
        return List.copyOf(items);
    }
}
