package game.bricks.infrastructure.network;

import java.io.IOException;

import game.bricks.application.port.GameConnection;
import game.bricks.application.port.GameServer;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class TcpHost implements GameServer {

    private final ServerSocket server;
    private volatile boolean closed;

    public TcpHost(int port) throws IOException {
        server = new ServerSocket();

        try {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            server.close();
            throw e;
        }
    }

    public int port() {
        return server.getLocalPort();
    }

    public void accept(Consumer<GameConnection> receive, Consumer<Exception> failure) {
        Thread.ofVirtual().name("lan-accept").start(() -> {
            try {
                while (!closed) {
                    Socket socket = server.accept();
                    receive.accept(new TcpConnection(socket));
                }
            } catch (Exception e) {
                if (!closed)
                    failure.accept(e);
            }
        });
    }

    public List<String> addresses() {
        List<String> result = new ArrayList<>();

        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!network.isUp() || network.isLoopback())
                    continue;
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address)
                        result.add(address.getHostAddress() + ":" + port());
                }
            }
        } catch (SocketException ignored) { /* Loopback remains available for local testing. */ }

        if (result.isEmpty())
            result.add("127.0.0.1:" + port());
        return List.copyOf(result);
    }

    @Override
    public void close() {
        closed = true;

        try {
            server.close();
        } catch (IOException ignored) { /* Already closed. */ }
    }
}
