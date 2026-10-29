package game.bricks.domain.ranking;

import game.bricks.domain.game.MatchResult;

import java.util.List;

public interface RankingRepository {

    List<MatchResult> load();

    void save(MatchResult result);
}
