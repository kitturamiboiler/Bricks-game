package game.bricks.application.port;

import java.util.List;
import java.util.function.Consumer;

public interface GameServer extends AutoCloseable {

    int port();
    List<String> addresses();

    void accept(Consumer<GameConnection> receive, Consumer<Exception> failure);

    @Override
    void close();
}
