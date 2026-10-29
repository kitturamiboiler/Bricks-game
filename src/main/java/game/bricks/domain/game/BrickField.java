package game.bricks.domain.game;

import java.util.ArrayList;
import java.util.List;

public final class BrickField {

    private final List<Brick> bricks;

    BrickField(List<Brick> bricks) {
        if (bricks.isEmpty()) {
            throw new IllegalArgumentException("A brick field must contain at least one brick");
        }

        this.bricks = List.copyOf(bricks);
    }

    static BrickField create(double boardWidth, int level) {
        int rows = level == 1 ? 6 : 7;
        int columns = 10;
        double gap = 5;
        double width = (boardWidth - 40 - gap * (columns - 1)) / columns;
        List<Brick> bricks = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                bricks.add(new Brick(20 + column * (width + gap), 50 + row * 30, width, 25));
            }
        }

        return new BrickField(bricks);
    }

    public List<Brick> getBricks() {
        return bricks;
    }

    boolean hit(Ball ball) {
        for (Brick brick : bricks) {
            if (ball.bounceOff(brick)) {
                brick.destroy();
                // Resolve one contact per physics step so adjacent bricks cannot cancel a bounce.
                return true;
            }
        }

        return false;
    }

    boolean isCleared() {
        return bricks.stream().noneMatch(Brick::isAlive);
    }
}
