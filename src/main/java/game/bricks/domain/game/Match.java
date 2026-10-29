package game.bricks.domain.game;

import game.bricks.domain.item.Inventory;
import game.bricks.domain.item.ItemType;
import game.bricks.domain.room.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class Match {
    public static final double DURATION = 180;
    public record DropView(double x, double y, ItemType type) {}
    private static final class Drop {
        double x, y;
        ItemType type;
        Drop(double x, double y, ItemType type) { this.x = x; this.y = y; this.type = type; }
    }
    private final String id = UUID.randomUUID().toString();
    private final boolean versus;
    private final Difficulty difficulty;
    private final List<String> ids;
    private final List<PlayerBoard> boards;
    private final Inventory[] inventories = {new Inventory(), new Inventory()};
    private final List<List<Drop>> drops = List.of(new ArrayList<>(), new ArrayList<>());
    private final double[] hidden = new double[2], immune = new double[2], shrink = new double[2],
            extend = new double[2], shield = new double[2], cooldown = new double[2];
    private final Random[] random;
    private double elapsed;
    private boolean finished;
    private int winner = -1;

    public Match(String first, String second, Difficulty difficulty) {
        this(first, second, difficulty, new Random().nextLong());
    }

    public Match(String first, String second, Difficulty difficulty, long seed) {
        first = new PlayerId(first).value();
        versus = second != null;
        if (versus) {
            second = new PlayerId(second).value();
            if (first.equalsIgnoreCase(second)) throw new IllegalArgumentException("Players must have different IDs.");
        }
        this.difficulty = java.util.Objects.requireNonNull(difficulty);
        ids = versus ? List.of(first, second) : List.of(first);
        boards = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            PlayerBoard board = new PlayerBoard(800, 600, difficulty);
            board.start();
            boards.add(board);
        }
        random = new Random[]{new Random(seed), new Random(seed)};
    }

    public void update(double seconds, boolean[] left, boolean[] right) {
        if (!Double.isFinite(seconds) || seconds < 0 || seconds > 1) throw new IllegalArgumentException("Invalid elapsed time.");
        if (finished) return;
        double step = versus ? Math.min(seconds, DURATION - elapsed) : seconds;
        elapsed += step;
        for (int i = 0; i < boards.size(); i++) {
            PlayerBoard board = boards.get(i);
            board.paddleScale(shrink[i] > elapsed ? 0.7 : extend[i] > elapsed ? 1.3 : 1);
            board.update(step, left[i], right[i]);
            for (PlayerBoard.Hit hit : board.drainHits()) {
                if (random[i].nextDouble() < 0.15) {
                    ItemType type = versus ? ItemType.values()[random[i].nextInt(4)] : ItemType.EXTEND;
                    drops.get(i).add(new Drop(hit.x(), hit.y(), type));
                }
            }
            var iterator = drops.get(i).iterator();
            while (iterator.hasNext()) {
                Drop drop = iterator.next();
                double previousY = drop.y;
                drop.y += 180 * step;
                Paddle paddle = board.getPaddle();
                if (previousY <= paddle.getY() && drop.y >= paddle.getY()
                        && drop.x >= paddle.getX() && drop.x <= paddle.getX() + paddle.getWidth()) {
                    collect(i, drop.type);
                    iterator.remove();
                } else if (drop.y > 620) iterator.remove();
            }
        }
        if (!versus) {
            finished = boards.getFirst().getState() == GameState.GAME_OVER;
            winner = finished ? 0 : -1;
        } else {
            boolean firstLost = boards.get(0).getLives() == 0;
            boolean secondLost = boards.get(1).getLives() == 0;
            if (firstLost || secondLost || elapsed >= DURATION) {
                finished = true;
                if (firstLost != secondLost) winner = firstLost ? 1 : 0;
                else {
                    int comparison = Integer.compare(boards.get(0).getScore(), boards.get(1).getScore());
                    if (comparison == 0) comparison = Integer.compare(boards.get(0).getLives(), boards.get(1).getLives());
                    winner = comparison == 0 ? -1 : comparison > 0 ? 0 : 1;
                }
            }
        }
    }

    boolean collect(int player, ItemType item) { return inventories[player].add(item); }

    public String useItem(int player) {
        if (player < 0 || player >= boards.size() || finished) return "Items cannot be used right now.";
        ItemType type = inventories[player].first();
        if (type == null) return "Your inventory is empty.";
        if (cooldown[player] > elapsed) return "Item cooldown is still active.";
        int target = versus ? 1 - player : player;
        if (type == ItemType.HIDE && (hidden[target] > elapsed || immune[target] > elapsed)) return "The opponent is immune to Hide ball.";
        if (type == ItemType.SHRINK && shrink[target] > elapsed) return "Shrink is already active.";

        inventories[player].consume();
        cooldown[player] = elapsed + 3;
        if ((type == ItemType.HIDE || type == ItemType.SHRINK) && shield[target] > elapsed) {
            shield[target] = 0;
            return "The opponent's shield blocked the attack.";
        }
        switch (type) {
            case HIDE -> { hidden[target] = elapsed + 2; immune[target] = elapsed + 5; }
            case SHRINK -> {
                if (extend[target] > elapsed) { extend[target] = 0; shrink[target] = 0; }
                else shrink[target] = elapsed + 4;
            }
            case EXTEND -> {
                if (shrink[player] > elapsed) { shrink[player] = 0; extend[player] = 0; }
                else extend[player] = elapsed + 5;
            }
            case SHIELD -> {
                if (hidden[player] > elapsed) immune[player] = elapsed + 3;
                hidden[player] = 0;
                shrink[player] = 0;
                shield[player] = elapsed + 5;
            }
        }
        return type.label + " used";
    }

    public boolean isFinished() { return finished; }
    public boolean isVersus() { return versus; }
    public Difficulty difficulty() { return difficulty; }
    public double elapsed() { return elapsed; }
    public int winner() { return winner; }
    public String id() { return id; }
    public List<PlayerBoard> boards() { return List.copyOf(boards); }
    public List<String> ids() { return ids; }
    public List<ItemType> inventory(int player) { return inventories[player].items(); }
    public double hidden(int player) { return Math.max(0, hidden[player] - elapsed); }
    public double shield(int player) { return Math.max(0, shield[player] - elapsed); }
    public double cooldown(int player) { return Math.max(0, cooldown[player] - elapsed); }
    public List<DropView> drops(int player) { return drops.get(player).stream().map(d -> new DropView(d.x, d.y, d.type)).toList(); }
    public MatchResult result() {
        if (!finished) throw new IllegalStateException("The match has not finished.");
        List<MatchResult.Player> players = new ArrayList<>();
        for (int i = 0; i < boards.size(); i++) {
            PlayerBoard board = boards.get(i);
            players.add(new MatchResult.Player(ids.get(i), board.getScore(), board.getLives(), board.getLevel()));
        }
        return new MatchResult(id, versus, difficulty, elapsed, winner, players);
    }
}
