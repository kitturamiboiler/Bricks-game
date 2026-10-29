package game.bricks.domain.item;

public enum ItemType {
    HIDE("Hide ball"), SHRINK("Shrink"), SHIELD("Shield"), EXTEND("Extend");
    public final String label;

    ItemType(String label) {
        this.label = label;
    }
}
