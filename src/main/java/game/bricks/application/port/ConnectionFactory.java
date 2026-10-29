package game.bricks.application.port;

import java.io.IOException;

public interface ConnectionFactory {
    GameServer host(int port) throws IOException;
    GameConnection connect(String address, int port) throws IOException;
}
