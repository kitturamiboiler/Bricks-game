package game.bricks.domain.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BallTest {

    @Test
    void movementUsesElapsedTimeRatherThanFrameCount() {
        Ball thirty = new Ball(100, 100, 10, 240, 240);
        Ball sixty = new Ball(100, 100, 10, 240, 240);

        for (int i = 0; i < 30; i++)
            thirty.move(1.0 / 30);
        for (int i = 0; i < 60; i++)
            sixty.move(1.0 / 60);

        assertEquals(340, thirty.getX(), 1e-8);
        assertEquals(thirty.getX(), sixty.getX(), 1e-8);
        assertEquals(thirty.getY(), sixty.getY(), 1e-8);
    }

    @Test
    void stageSpeedSurvivesResetOnBothAxes() {
        Ball ball = new Ball(400, 300, 10, 240, 240);
        ball.increaseSpeed();
        ball.reset(400, 300);

        assertEquals(264, ball.getVelocityX(), 1e-8);
        assertEquals(-264, ball.getVelocityY(), 1e-8);
    }

    @Test
    void wallsCorrectPositionAndDoNotReflectAtBottom() {
        Ball ball = new Ball(8, 8, 10, -240, -240);
        ball.reflectWalls(800);

        assertEquals(10, ball.getX());
        assertEquals(10, ball.getY());
        assertEquals(240, ball.getVelocityX());
        assertEquals(240, ball.getVelocityY());

        Ball bottom = new Ball(400, 605, 10, 240, 240);
        bottom.reflectWalls(800);
        assertEquals(240, bottom.getVelocityY());
    }

    @Test
    void paddleReflectsOnlyADownwardBallReachingItsTop() {
        Paddle paddle = new Paddle(350, 570, 100, 12);
        Ball ball = new Ball(400, 550, 10, 0, 240);
        ball.move(0.05);

        assertTrue(ball.bounceOff(paddle));
        assertEquals(560, ball.getY());
        assertEquals(-240, ball.getVelocityY());
        assertFalse(ball.bounceOff(paddle));
        assertFalse(new Ball(400, 580, 10, 0, 240).bounceOff(paddle));
    }

    @Test
    void adjacentBrickContactsCannotCancelTheBounce() {
        Ball ball = new Ball(80, 45, 10, 0, 240);
        Brick first = new Brick(2.5, 50, 75, 25);
        Brick second = new Brick(82.5, 50, 75, 25);
        BrickField field = new BrickField(List.of(first, second));

        assertTrue(field.hit(ball));
        assertTrue(ball.getVelocityY() < 0);
        assertEquals(1, field.getBricks().stream().filter(brick -> !brick.isAlive()).count());
    }

    @Test
    void sideContactReflectsHorizontalVelocity() {
        Ball ball = new Ball(95, 62, 10, 240, 40);

        assertTrue(ball.bounceOff(new Brick(100, 50, 75, 25)));
        assertEquals(-240, ball.getVelocityX());
        assertEquals(40, ball.getVelocityY());
        assertEquals(90, ball.getX());
    }

    @Test
    void circleDoesNotHitAnEmptyRectangleCorner() {
        Ball ball = new Ball(91, 41, 10, 240, 240);
        assertFalse(ball.bounceOff(new Brick(100, 50, 75, 25)));
    }

    @Test
    void stageSpeedIsCappedForDiscreteCollisionSteps() {
        Ball ball = new Ball(400, 300, 10, 240, -240);

        for (int i = 0; i < 100; i++)
            ball.increaseSpeed();
        assertEquals(900, Math.hypot(ball.getVelocityX(), ball.getVelocityY()), 1e-8);
    }

    @Test
    void rejectsNonfiniteValuesAndNonpositiveSizes() {
        assertThrows(IllegalArgumentException.class, () -> new Ball(0, 0, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Ball(Double.NaN, 0, 10, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Ball(0, 0, 10, Double.POSITIVE_INFINITY, 1));
        assertThrows(IllegalArgumentException.class, () -> new Paddle(0, 0, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> new Brick(0, 0, 10, Double.NaN));
    }
}
