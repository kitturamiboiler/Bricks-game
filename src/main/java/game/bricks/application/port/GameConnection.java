package game.bricks.application.port;

import game.bricks.application.dto.NetworkMessage;

import java.io.IOException;
import java.util.function.Consumer;

public interface GameConnection extends AutoCloseable {

    void start(Consumer<NetworkMessage> receive, Consumer<Exception> failure);

    void send(NetworkMessage message) throws IOException;

    @Override
    void close();
}
