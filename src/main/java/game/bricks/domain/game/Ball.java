package game.bricks.domain.game;

public final class Ball {

    private double x;
    private double y;
    private double previousY;
    private final double radius;
    private double velocityX;
    private double velocityY;

    public Ball(double x, double y, double radius, double velocityX, double velocityY) {
        if (!Double.isFinite(x) || !Double.isFinite(y)
            || !Double.isFinite(radius) || radius <= 0
            || !Double.isFinite(velocityX) || !Double.isFinite(velocityY)) {
            throw new IllegalArgumentException("Ball coordinates and velocity must be finite; radius must be positive");
        }

        this.x = x;
        this.y = y;
        this.previousY = y;
        this.radius = radius;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    void move(double seconds) {
        previousY = y;
        x += velocityX * seconds;
        y += velocityY * seconds;
    }

    void reflectWalls(double width) {
        if (x < radius) {
            x = radius;
            velocityX = Math.abs(velocityX);
        } else if (x > width - radius) {
            x = width - radius;
            velocityX = -Math.abs(velocityX);
        }

        if (y < radius) {
            y = radius;
            velocityY = Math.abs(velocityY);
        }
    }

    void reset(double x, double y) {
        this.x = x;
        this.y = y;
        previousY = y;
        velocityY = -Math.abs(velocityY);
    }

    void scaleSpeed(double factor) {
        velocityX *= factor;
        velocityY *= factor;
    }

    void increaseSpeed() {
        double speed = Math.hypot(velocityX, velocityY);
        double factor = speed == 0 ? 1 : Math.min(1.1, 900 / speed);
        velocityX *= factor;
        velocityY *= factor;
    }

    boolean bounceOff(Paddle paddle) {
        if (velocityY <= 0 || previousY + radius > paddle.getY()
            || !intersects(paddle.getX(), paddle.getY(), paddle.getWidth(), paddle.getHeight())) {
            return false;
        }

        y = paddle.getY() - radius;
        velocityY = -Math.abs(velocityY);

        return true;
    }

    boolean bounceOff(Brick brick) {
        if (!brick.isAlive() || !intersects(brick.getX(), brick.getY(), brick.getWidth(), brick.getHeight())) {
            return false;
        }

        double fromLeft = x + radius - brick.getX();
        double fromRight = brick.getX() + brick.getWidth() - x + radius;
        double fromTop = y + radius - brick.getY();
        double fromBottom = brick.getY() + brick.getHeight() - y + radius;

        if (Math.min(fromLeft, fromRight) < Math.min(fromTop, fromBottom)) {
            if (fromLeft < fromRight) {
                x = brick.getX() - radius;
                velocityX = -Math.abs(velocityX);
            } else {
                x = brick.getX() + brick.getWidth() + radius;
                velocityX = Math.abs(velocityX);
            }
        } else if (fromTop < fromBottom) {
            y = brick.getY() - radius;
            velocityY = -Math.abs(velocityY);
        } else {
            y = brick.getY() + brick.getHeight() + radius;
            velocityY = Math.abs(velocityY);
        }

        return true;
    }

    private boolean intersects(double left, double top, double width, double height) {
        double nearestX = Math.clamp(x, left, left + width);
        double nearestY = Math.clamp(y, top, top + height);
        double distanceX = x - nearestX;
        double distanceY = y - nearestY;

        return distanceX * distanceX + distanceY * distanceY < radius * radius;
    }
}
