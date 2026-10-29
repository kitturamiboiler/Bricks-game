package game.bricks.domain.game;

public final class Brick {

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private boolean alive = true;

    public Brick(double x, double y, double width, double height) {
        if (!Double.isFinite(x) || !Double.isFinite(y)
            || !Double.isFinite(width) || width <= 0
            || !Double.isFinite(height) || height <= 0) {
            throw new IllegalArgumentException("Brick coordinates must be finite and size must be positive");
        }

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean isAlive() {
        return alive;
    }

    void destroy() {
        alive = false;
    }
}
