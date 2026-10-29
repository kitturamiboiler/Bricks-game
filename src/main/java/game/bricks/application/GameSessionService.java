package game.bricks.application;

import game.bricks.application.dto.*;
import game.bricks.domain.game.*;
import game.bricks.domain.room.*;
import game.bricks.application.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class GameSessionService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GameSessionService.class);
    private final RankingService ranking;
    private final ConnectionFactory connections;
    private final ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "game-loop"); thread.setDaemon(true); return thread;
    });
    private final ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
    private Consumer<SessionView> listener = view -> {};
    private String mode = "IDLE", status = "Choose your player ID and difficulty.", address = "", playerId = "";
    private int localPlayer;
    private long generation, sequence, peerSequence = -1;
    private long lastTick = System.nanoTime();
    private double networkElapsed;
    private boolean[] left = new boolean[2], right = new boolean[2];
    private boolean usePending;
    private Room room;
    private Match match;
    private GameServer host;
    private GameConnection peer;
    private NetworkMessage.Lobby remoteLobby;
    private GameSnapshot remoteState;
    private String saved = "";
    private boolean closed;

    public GameSessionService(RankingService ranking, ConnectionFactory connections) {
        this.ranking = java.util.Objects.requireNonNull(ranking);
        this.connections = java.util.Objects.requireNonNull(connections);
        loop.scheduleWithFixedDelay(this::tick, 16, 16, TimeUnit.MILLISECONDS);
    }
    public synchronized void setListener(Consumer<SessionView> listener) { this.listener = listener; publish(); }
    public synchronized SessionView view() {
        NetworkMessage.Lobby lobby = room == null ? remoteLobby : new NetworkMessage.Lobby(room.host(), room.guest(), room.difficulty().name(), room.hostReady(), room.guestReady());
        return new SessionView(mode, status, address, localPlayer, lobby, match == null ? remoteState : GameSnapshot.from(match));
    }
    public synchronized List<String> addresses() { return host == null ? List.of(address) : host.addresses(); }

    public synchronized void single(String id, Difficulty difficulty) {
        id = new PlayerId(id).value();
        reset(); playerId = id; mode = "SINGLE"; match = new Match(id, null, difficulty);
        status = "Move with LEFT / RIGHT and use items with SPACE.";
        log.info("Single match started: {}", id); publish();
    }

    public synchronized void host(String id, Difficulty difficulty, int port) throws IOException {
        id = new PlayerId(id).value();
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535.");
        reset();
        host = connections.host(port);
        mode = "HOST"; playerId = id; room = new Room(id, difficulty);
        address = host.addresses().getFirst();
        status = "Port " + host.port() + " is open for LAN play.";
        long token = generation;
        host.accept(connection -> accepted(connection, token), error -> disconnected(null, token, error));
        log.info("LAN server opened on {}", host.port()); publish();
    }

    private synchronized void accepted(GameConnection connection, long token) {
        if (token != generation || peer != null) {
            connection.close(); return;
        }
        peer = connection;
        connection.start(message -> receive(connection, token, message), error -> disconnected(connection, token, error));
    }

    public synchronized void join(String id, String endpoint) {
        id = new PlayerId(id).value();
        String[] parts = endpoint.trim().split(":", -1);
        if (parts.length != 2 || parts[0].isBlank()) throw new IllegalArgumentException("Enter the address as IP:port.");
        int port;
        try { port = Integer.parseInt(parts[1]); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Check the port number."); }
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535.");
        reset(); mode = "CONNECTING"; playerId = id; address = endpoint.trim(); localPlayer = 1;
        status = "Connecting to the server..."; publish();
        long token = generation;
        io.execute(() -> {
            try {
                GameConnection connection = connections.connect(parts[0], port);
                synchronized (this) {
                    if (token != generation) { connection.close(); return; }
                    peer = connection; mode = "CLIENT";
                    connection.send(new NetworkMessage.Hello(playerId));
                    connection.start(message -> receive(connection, token, message), error -> disconnected(connection, token, error));
                }
            } catch (Exception e) { disconnected(null, token, e); }
        });
    }

    private synchronized void receive(GameConnection connection, long token, NetworkMessage message) {
        if (token != generation || connection != peer) return;
        try {
            if (mode.equals("HOST")) {
                switch (message) {
                    case NetworkMessage.Hello hello -> { room.join(hello.id()); status = "The match starts when both players are ready."; }
                    case NetworkMessage.Ready ready -> { if (room.guest().isEmpty()) throw new IOException("Send a valid ID first."); readyPlayer(1, ready.ready()); }
                    case NetworkMessage.Input input -> {
                        if (input.sequence() <= peerSequence) return;
                        peerSequence = input.sequence(); left[1] = input.left(); right[1] = input.right();
                        if (input.useItem() && match != null) status = match.useItem(1);
                    }
                    case NetworkMessage.Leave ignored -> { disconnected(connection, token, new IOException("The guest left.")); return; }
                    default -> throw new IOException("Unexpected client message.");
                }
            } else if (mode.equals("CLIENT")) {
                switch (message) {
                    case NetworkMessage.Lobby lobby -> {
                        if (!lobby.guest().equals(playerId)) throw new IOException("The room ID does not match.");
                        remoteLobby = lobby; remoteState = null; status = "Connected. The match starts when both players are ready.";
                    }
                    case NetworkMessage.State state -> {
                        if (remoteLobby == null || state.snapshot().boards().size() != 2
                                || !state.snapshot().boards().get(1).id().equals(playerId)) throw new IOException("Invalid match state.");
                        remoteState = state.snapshot(); status = remoteState.finished() ? "Match finished. Play again or return to the menu." : "LEFT / RIGHT: move | SPACE: item";
                    }
                    case NetworkMessage.Failure failure -> { disconnected(connection, token, new IOException(failure.reason())); return; }
                    default -> throw new IOException("Unexpected server message.");
                }
            }
            publish();
        } catch (Exception e) { disconnected(connection, token, e); }
    }

    public synchronized void ready() {
        if (mode.equals("HOST")) readyPlayer(0, match != null && match.isFinished() || !room.hostReady());
        else if (mode.equals("CLIENT") && remoteLobby != null) {
            send(new NetworkMessage.Ready(remoteState != null || !remoteLobby.guestReady()));
        }
        publish();
    }
    private void readyPlayer(int player, boolean value) {
        if (match != null && !match.isFinished()) return;
        if (match != null) { match = null; room.resetReady(); saved = ""; }
        room.ready(player, value);
        if (room.canStart()) {
            match = new Match(room.host(), room.guest(), room.difficulty());
            left = new boolean[2]; right = new boolean[2]; status = "LAN match started";
            log.info("LAN match started");
        }
    }
    public synchronized void keys(boolean leftPressed, boolean rightPressed) { left[localPlayer] = leftPressed; right[localPlayer] = rightPressed; }
    public synchronized void useItem() {
        if (mode.equals("CLIENT")) usePending = true;
        else if (match != null) { status = match.useItem(0); publish(); }
    }
    public synchronized void restart() {
        if (mode.equals("SINGLE") && match != null) single(playerId, match.difficulty());
        else ready();
    }

    private synchronized void tick() {
        long now = System.nanoTime();
        double seconds = Math.min(1, (now - lastTick) / 1_000_000_000.0);
        lastTick = now;
        if (closed) return;
        try {
            if (match != null && !match.isFinished()) {
                match.update(seconds, left, right);
                if (match.isFinished() && !saved.equals(match.id())) {
                    saved = match.id();
                    var result = match.result(); long token = generation;
                    status = "Match finished. Saving results.";
                    io.execute(() -> {
                        try { ranking.save(result); }
                        catch (RuntimeException e) {
                            synchronized (this) { if (token == generation) { status = "Match finished, but the result could not be saved: " + e.getMessage(); publish(); } }
                            log.error("Record save failed", e);
                        }
                    });
                }
            }
            networkElapsed += seconds;
            if (peer != null && networkElapsed >= 0.05) {
                networkElapsed = 0;
                if (mode.equals("HOST") && !room.guest().isEmpty()) {
                    send(match == null ? new NetworkMessage.Lobby(room.host(), room.guest(), room.difficulty().name(), room.hostReady(), room.guestReady())
                            : new NetworkMessage.State(GameSnapshot.from(match)));
                } else if (mode.equals("CLIENT")) {
                    send(new NetworkMessage.Input(sequence++, left[1], right[1], usePending)); usePending = false;
                }
            }
            if (match != null) publish();
        } catch (RuntimeException e) { status = "Game error: " + e.getMessage(); log.error("Game loop failed", e); reset(); publish(); }
    }

    private void send(NetworkMessage message) {
        if (peer == null) return;
        try { peer.send(message); }
        catch (IOException e) { disconnected(peer, generation, e); }
    }

    private synchronized void disconnected(GameConnection connection, long token, Exception error) {
        if (token != generation || closed || (connection != null && connection != peer)) return;
        String reason = error.getMessage() == null ? "The connection was closed." : error.getMessage();
        log.info("LAN connection ended: {}", reason);
        if (mode.equals("HOST") && host != null) {
            if (peer != null) peer.close(); peer = null;
            room = new Room(room.host(), room.difficulty()); match = null; peerSequence = -1;
            left = new boolean[2]; right = new boolean[2];
            status = "Guest disconnected: " + reason + " - Waiting for another guest.";
        } else { reset(); status = "Connection failed or closed: " + reason + " - Check the address, LAN and firewall."; }
        publish();
    }
    public synchronized void leave() { reset(); status = "Returned to the menu."; publish(); }
    private void reset() {
        generation++;
        if (peer != null) peer.close();
        if (host != null) host.close();
        peer = null; host = null; room = null; match = null; remoteLobby = null; remoteState = null;
        mode = "IDLE"; address = ""; localPlayer = 0; sequence = 0; peerSequence = -1; saved = "";
        left = new boolean[2]; right = new boolean[2]; usePending = false; networkElapsed = 0;
    }
    private void publish() { listener.accept(view()); }
    @Override public synchronized void close() { closed = true; reset(); loop.shutdownNow(); io.shutdownNow(); }
}
