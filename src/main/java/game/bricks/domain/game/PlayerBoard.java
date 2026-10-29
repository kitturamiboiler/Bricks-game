package game.bricks.domain.game;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public final class PlayerBoard {

    private static final double STEP_SECONDS = 1.0 / 120;

    private final double width;
    private final double height;
    private int startingLives;
    private Difficulty difficulty = Difficulty.NORMAL;
    private final List<Hit> hits = new ArrayList<>();

    record Hit(double x, double y) {

    }

    private final Paddle paddle;
    private Ball ball;
    private BrickField brickField;
    private GameState state = GameState.READY;
    private int score;
    private int lives;
    private int level = 1;
    private double accumulatedSeconds;

    public PlayerBoard(double width, double height) {
        this(
            width,
            height,
            new Ball(width / 2, height / 2, 10, 240, -240),
            new Paddle((width - 100) / 2, height - 30, 100, 12),
            BrickField.create(width, 1),
            3
        );
    }

    public PlayerBoard(double width, double height, Difficulty difficulty) {
        this(width, height);
        this.difficulty = Objects.requireNonNull(difficulty);
        this.startingLives = difficulty.lives;
        this.lives = difficulty.lives;
        ball.scaleSpeed(difficulty.speed);
        paddle.resize(100 * difficulty.paddle, width);
        paddle.center(width);
    }

    void paddleScale(double scale) {
        paddle.resize(100 * difficulty.paddle * scale, width);
    }

    List<Hit> drainHits() {
        List<Hit> result = List.copyOf(hits);
        hits.clear();

        return result;
    }

    PlayerBoard(double width, double height, Ball ball, Paddle paddle, BrickField brickField, int lives) {
        if (!Double.isFinite(width) || width < 320 || !Double.isFinite(height) || height < 600 || lives <= 0) {
            throw new IllegalArgumentException("Board must be at least 320x600 with positive lives");
        }

        this.width = width;
        this.height = height;
        this.ball = Objects.requireNonNull(ball);
        this.paddle = Objects.requireNonNull(paddle);
        this.brickField = Objects.requireNonNull(brickField);
        this.startingLives = lives;
        this.lives = lives;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Ball getBall() {
        return ball;
    }

    public Paddle getPaddle() {
        return paddle;
    }

    public BrickField getBrickField() {
        return brickField;
    }

    public GameState getState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public int getLevel() {
        return level;
    }

    public void start() {
        if (state == GameState.READY) {
            state = GameState.RUNNING;
        }
    }

    public void restart() {
        ball = new Ball(width / 2, height / 2, 10, 240, -240);
        ball.scaleSpeed(difficulty.speed);
        paddle.resize(100 * difficulty.paddle, width);
        paddle.center(width);
        hits.clear();
        brickField = BrickField.create(width, 1);
        score = 0;
        lives = startingLives;
        level = 1;
        accumulatedSeconds = 0;
        state = GameState.READY;
    }

    public void update(double elapsedSeconds, boolean left, boolean right) {
        if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0 || elapsedSeconds > 1) {
            throw new IllegalArgumentException("Elapsed time must be finite and between 0 and 1 second");
        }

        if (state != GameState.RUNNING) {
            return;
        }

        accumulatedSeconds += elapsedSeconds;

        while (accumulatedSeconds + 1e-12 >= STEP_SECONDS && state == GameState.RUNNING) {
            step(left, right);
            accumulatedSeconds -= STEP_SECONDS;
        }
    }

    private void step(boolean left, boolean right) {
        paddle.move(left, right, STEP_SECONDS, width);
        ball.move(STEP_SECONDS);
        ball.reflectWalls(width);
        ball.bounceOff(paddle);

        if (ball.getY() + ball.getRadius() >= height) {
            lives--;

            if (lives == 0) {
                state = GameState.GAME_OVER;
            } else {
                ball.reset(width / 2, height / 2);
            }

            return;
        }

        if (brickField.hit(ball)) {
            score += 100;
            hits.add(new Hit(ball.getX(), ball.getY()));
        }

        if (brickField.isCleared()) {
            score += 500;
            level++;
            hits.clear();
            ball.increaseSpeed();
            brickField = BrickField.create(width, level);
            ball.reset(width / 2, height / 2);
        }
    }
}
