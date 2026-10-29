package game.bricks.domain.game;

import game.bricks.domain.room.PlayerId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record MatchResult(
    String id,
    boolean versus,
    Difficulty difficulty,
    double elapsed,
    int winner,
    List<Player> players
) {

    public record Player(String id, int score, int lives, int level) {

        public Player {
            id = new PlayerId(id).value();

            if (score < 0 || lives < 0 || level < 1)
                throw new IllegalArgumentException("Invalid match result.");
        }
    }

    public MatchResult {
        UUID.fromString(id);
        Objects.requireNonNull(difficulty);
        players = List.copyOf(players);

        if (players.size() != (versus ? 2 : 1) || winner < -1 || winner >= players.size()
            || !Double.isFinite(elapsed) || elapsed < 0 || elapsed > 86400) {
            throw new IllegalArgumentException("Invalid match result.");
        }

        if (versus && players.get(0).id().equalsIgnoreCase(players.get(1).id())) {
            throw new IllegalArgumentException("Duplicate player ID.");
        }
    }
}
