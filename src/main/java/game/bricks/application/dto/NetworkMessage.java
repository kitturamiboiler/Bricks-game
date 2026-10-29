package game.bricks.application.dto;

public sealed interface NetworkMessage {
    record Hello(String id) implements NetworkMessage {}
    record Lobby(String host, String guest, String difficulty, boolean hostReady, boolean guestReady) implements NetworkMessage {}
    record Ready(boolean ready) implements NetworkMessage {}
    record Input(long sequence, boolean left, boolean right, boolean useItem) implements NetworkMessage {}
    record State(GameSnapshot snapshot) implements NetworkMessage {}
    record Failure(String reason) implements NetworkMessage {}
    record Leave() implements NetworkMessage {}
}
