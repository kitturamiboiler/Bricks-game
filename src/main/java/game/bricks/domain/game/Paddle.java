package game.bricks.domain.game;

public final class Paddle {

    private double x;
    private final double y;
    private double width;
    private final double height;

    public Paddle(double x, double y, double width, double height) {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(width) || width <= 0
                || !Double.isFinite(height) || height <= 0) {
            throw new IllegalArgumentException("Paddle coordinates must be finite and size must be positive");
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

    void move(boolean left, boolean right, double seconds, double boardWidth) {
        int direction = (right ? 1 : 0) - (left ? 1 : 0);
        x = Math.clamp(x + direction * 420 * seconds, 0, boardWidth - width);
    }

    void resize(double width, double boardWidth) {
        double center = x + this.width / 2;
        this.width = width;
        x = Math.clamp(center - width / 2, 0, boardWidth - width);
    }

    void center(double boardWidth) {
        x = (boardWidth - width) / 2;
    }
}
