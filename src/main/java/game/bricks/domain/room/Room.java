package game.bricks.domain.room;

import game.bricks.domain.game.Difficulty;
import java.util.Objects;

public final class Room {
    private final PlayerId host;
    private PlayerId guest;
    private final Difficulty difficulty;
    private boolean hostReady;
    private boolean guestReady;

    public Room(String host, Difficulty difficulty) {
        this.host = new PlayerId(host);
        this.difficulty = Objects.requireNonNull(difficulty);
    }

    public void join(String id) {
        PlayerId candidate = new PlayerId(id);
        if (guest != null)
            throw new IllegalStateException("The room is full.");
        if (host.value().equalsIgnoreCase(candidate.value()))
            throw new IllegalArgumentException("Choose an ID different from the host.");
        guest = candidate;
    }

    public void ready(int player, boolean ready) {
        if (player == 0) hostReady = ready;
        else if (player == 1 && guest != null) guestReady = ready;
        else throw new IllegalArgumentException("Unknown player.");
    }

    public boolean canStart() { return guest != null && hostReady && guestReady; }
    public void resetReady() { hostReady = false; guestReady = false; }
    public String host() { return host.value(); }
    public String guest() { return guest == null ? "" : guest.value(); }
    public Difficulty difficulty() { return difficulty; }
    public boolean hostReady() { return hostReady; }
    public boolean guestReady() { return guestReady; }
}
