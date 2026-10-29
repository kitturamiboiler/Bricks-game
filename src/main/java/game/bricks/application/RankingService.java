package game.bricks.application;

import game.bricks.domain.game.Difficulty;
import game.bricks.domain.game.MatchResult;
import game.bricks.domain.ranking.RankingRepository;

import java.util.*;

public final class RankingService {

    public record Single(String id, int score, int level, double elapsed) {

    }

    public record Standing(String id, int wins, int draws, int losses) {

    }

    private final RankingRepository repository;

    public RankingService(RankingRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void save(MatchResult result) {
        repository.save(result);
    }

    public List<Single> single(Difficulty difficulty) {
        Comparator<Single> order = Comparator.comparingInt(Single::score).reversed()
            .thenComparing(Comparator.comparingInt(Single::level).reversed()).thenComparingDouble(Single::elapsed)
            .thenComparing(Single::id);
        Map<String, Single> best = new HashMap<>();

        for (var result : repository.load())
            if (!result.versus() && result.difficulty() == difficulty) {
                var player = result.players().getFirst();
                Single entry = new Single(player.id(), player.score(), player.level(), result.elapsed());
                best.merge(player.id(), entry, (a, b) -> order.compare(a, b) <= 0 ? a : b);
            }
        return best.values().stream().sorted(order).limit(10).toList();
    }

    public List<Standing> versus(Difficulty difficulty) {
        Map<String, int[]> totals = new HashMap<>();

        for (var result : repository.load())
            if (result.versus() && result.difficulty() == difficulty) {
                for (int i = 0; i < 2; i++) {
                    int[] count = totals.computeIfAbsent(result.players().get(i).id(), key -> new int[3]);
                    count[result.winner() == -1 ? 1 : result.winner() == i ? 0 : 2]++;
                }
            }
        return totals.entrySet().stream()
            .map(e -> new Standing(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
            .sorted(Comparator.comparingInt(Standing::wins).reversed().thenComparingInt(Standing::losses)
                .thenComparing(Standing::id)).limit(10).toList();
    }
}
