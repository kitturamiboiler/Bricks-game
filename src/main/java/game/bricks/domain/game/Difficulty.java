package game.bricks.domain.game;

public enum Difficulty {
    EASY("Easy", 0.8, 1.2, 5), NORMAL("Normal", 1, 1, 3), HARD("Hard", 1.2, 0.85, 3);

    public final String label;
    public final double speed;
    public final double paddle;
    public final int lives;

    Difficulty(String label, double speed, double paddle, int lives) {
        this.label = label;
        this.speed = speed;
        this.paddle = paddle;
        this.lives = lives;
    }

    @Override
    public String toString() {
        return label;
    }
}
