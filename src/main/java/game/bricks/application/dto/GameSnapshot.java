package game.bricks.application.dto;

import game.bricks.domain.game.Match;
import java.util.ArrayList;
import java.util.List;

public record GameSnapshot(String matchId, boolean versus, String difficulty, boolean finished,
                           int winner, double elapsed, List<Board> boards) {
    public record Tile(double x, double y, double width, double height) {}
    public record Drop(double x, double y, String type) {}
    public record Board(String id, double ballX, double ballY, double radius, double paddleX,
                        double paddleY, double paddleWidth, double paddleHeight, int score, int lives,
                        int level, double hidden, double shield, double cooldown,
                        List<Tile> bricks, List<String> inventory, List<Drop> drops) {
        public Board { bricks = List.copyOf(bricks); inventory = List.copyOf(inventory); drops = List.copyOf(drops); }
    }
    public GameSnapshot { boards = List.copyOf(boards); }

    public static GameSnapshot from(Match match) {
        List<Board> boards = new ArrayList<>();
        for (int i = 0; i < match.boards().size(); i++) {
            var board = match.boards().get(i);
            var ball = board.getBall();
            var paddle = board.getPaddle();
            List<Tile> tiles = board.getBrickField().getBricks().stream().filter(b -> b.isAlive())
                    .map(b -> new Tile(b.getX(), b.getY(), b.getWidth(), b.getHeight())).toList();
            List<Drop> drops = match.drops(i).stream().map(d -> new Drop(d.x(), d.y(), d.type().name())).toList();
            boards.add(new Board(match.ids().get(i), ball.getX(), ball.getY(), ball.getRadius(),
                    paddle.getX(), paddle.getY(), paddle.getWidth(), paddle.getHeight(), board.getScore(),
                    board.getLives(), board.getLevel(), match.hidden(i), match.shield(i), match.cooldown(i),
                    tiles, match.inventory(i).stream().map(Enum::name).toList(), drops));
        }
        return new GameSnapshot(match.id(), match.isVersus(), match.difficulty().name(), match.isFinished(),
                match.winner(), match.elapsed(), boards);
    }
}
