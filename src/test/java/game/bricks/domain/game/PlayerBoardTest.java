package game.bricks.domain.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerBoardTest {

    @Test
    void waitingBoardDoesNotMoveUntilStarted() {
        PlayerBoard board = new PlayerBoard(800, 600);
        board.update(1, true, false);

        assertEquals(GameState.READY, board.getState());
        assertEquals(400, board.getBall().getX());
        assertEquals(350, board.getPaddle().getX());
        board.start();
        board.update(1.0 / 60, false, false);
        assertEquals(GameState.RUNNING, board.getState());
        assertNotEquals(400, board.getBall().getX());
    }

    @Test
    void losingLastLifeFreezesAllFurtherUpdates() {
        PlayerBoard board = fixture(
            new Ball(100, 589, 10, 240, 240),
            new BrickField(List.of(new Brick(20, 50, 60, 25))),
            1
        );
        board.start();
        board.update(1, false, true);

        assertEquals(GameState.GAME_OVER, board.getState());
        assertEquals(0, board.getLives());
        double x = board.getBall().getX();
        double y = board.getBall().getY();
        double paddleX = board.getPaddle().getX();
        board.start();
        board.update(1, true, false);

        assertEquals(0, board.getLives());
        assertEquals(x, board.getBall().getX());
        assertEquals(y, board.getBall().getY());
        assertEquals(paddleX, board.getPaddle().getX());
        assertEquals(0, board.getScore());
    }

    @Test
    void losingALifeServesInsideBoardAndKeepsSpeed() {
        PlayerBoard board = fixture(
            new Ball(100, 589, 10, 264, 264),
            new BrickField(List.of(new Brick(20, 50, 60, 25))),
            3
        );
        board.start();
        board.update(1.0 / 120, false, false);

        assertEquals(2, board.getLives());
        assertEquals(400, board.getBall().getX());
        assertEquals(300, board.getBall().getY());
        assertEquals(264, board.getBall().getVelocityX());
        assertEquals(-264, board.getBall().getVelocityY());
    }

    @Test
    void clearingStageUsesBoardCenterAndPreservesIncreasedSpeed() {
        PlayerBoard board = fixture(
            new Ball(400, 85, 10, 240, -240),
            new BrickField(List.of(new Brick(350, 50, 100, 25))),
            3
        );
        board.start();
        board.update(1.0 / 120, false, false);

        assertAll(
            () -> assertEquals(2, board.getLevel()),
            () -> assertEquals(600, board.getScore()),
            () -> assertEquals(400, board.getBall().getX()),
            () -> assertEquals(300, board.getBall().getY()),
            () -> assertEquals(264, board.getBall().getVelocityX(), 1e-8),
            () -> assertEquals(-264, board.getBall().getVelocityY(), 1e-8),
            () -> assertEquals(70, board.getBrickField().getBricks().size())

        );
    }

    @Test
    void thirtyAndSixtyFrameUpdatesProduceSameGameState() {
        PlayerBoard thirty = new PlayerBoard(800, 600);
        PlayerBoard sixty = new PlayerBoard(800, 600);
        thirty.start();
        sixty.start();

        for (int i = 0; i < 30; i++)
            thirty.update(1.0 / 30, false, true);
        for (int i = 0; i < 60; i++)
            sixty.update(1.0 / 60, false, true);

        assertEquals(thirty.getBall().getX(), sixty.getBall().getX(), 1e-8);
        assertEquals(thirty.getBall().getY(), sixty.getBall().getY(), 1e-8);
        assertEquals(thirty.getPaddle().getX(), sixty.getPaddle().getX(), 1e-8);
        assertEquals(thirty.getLives(), sixty.getLives());
        assertEquals(thirty.getScore(), sixty.getScore());
    }

    @Test
    void oppositeKeysCancelAndPaddleStaysWithinBoard() {
        Paddle paddle = new Paddle(350, 570, 100, 12);
        paddle.move(true, true, 1, 800);
        assertEquals(350, paddle.getX());
        paddle.move(true, false, 10, 800);
        assertEquals(0, paddle.getX());
        paddle.move(false, true, 10, 800);
        assertEquals(700, paddle.getX());
    }

    @Test
    void restartRestoresNewGameAndRequiresStart() {
        PlayerBoard board = fixture(
            new Ball(400, 85, 10, 240, -240),
            new BrickField(List.of(new Brick(350, 50, 100, 25))),
            3
        );
        board.start();
        board.update(1.0 / 120, false, false);
        board.restart();

        assertEquals(GameState.READY, board.getState());
        assertEquals(0, board.getScore());
        assertEquals(3, board.getLives());
        assertEquals(1, board.getLevel());
        assertEquals(60, board.getBrickField().getBricks().size());
        assertEquals(240, board.getBall().getVelocityX());
        assertEquals(-240, board.getBall().getVelocityY());
    }

    @Test
    void rejectsInvalidBoardAndTimeValues() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerBoard(0, 600));
        assertThrows(IllegalArgumentException.class, () -> new PlayerBoard(800, Double.NaN));
        PlayerBoard board = new PlayerBoard(800, 600);
        assertThrows(IllegalArgumentException.class, () -> board.update(-1, false, false));
        assertThrows(IllegalArgumentException.class, () -> board.update(2, false, false));
        assertThrows(IllegalArgumentException.class, () -> board.update(Double.POSITIVE_INFINITY, false, false));
        assertThrows(IllegalArgumentException.class, () -> board.update(Double.NaN, false, false));
    }

    private PlayerBoard fixture(Ball ball, BrickField field, int lives) {
        return new PlayerBoard(800, 600, ball, new Paddle(350, 570, 100, 12), field, lives);
    }
}
