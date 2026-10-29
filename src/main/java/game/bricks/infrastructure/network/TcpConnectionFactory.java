package game.bricks.infrastructure.network;

import game.bricks.application.port.*;

import java.io.IOException;

public final class TcpConnectionFactory implements ConnectionFactory {

    @Override
    public GameServer host(int port) throws IOException {
        return new TcpHost(port);
    }

    @Override
    public GameConnection connect(String address, int port) throws IOException {
        return TcpConnection.connect(address, port);
    }
}
