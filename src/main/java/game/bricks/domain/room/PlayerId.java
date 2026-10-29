package game.bricks.domain.room;

public record PlayerId(String value) {

    public PlayerId {
        if (value == null || !value.trim().matches("[a-zA-Z0-9]{2,12}")) {
            throw new IllegalArgumentException("Use 2-12 English letters or digits for your ID.");
        }

        value = value.trim();
    }
}
