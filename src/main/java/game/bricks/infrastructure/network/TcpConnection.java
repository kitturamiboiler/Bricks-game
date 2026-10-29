package game.bricks.infrastructure.network;

import game.bricks.application.dto.NetworkMessage;
import game.bricks.application.port.GameConnection;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class TcpConnection implements GameConnection {
    private final Socket socket;
    private final PacketCodec codec = new PacketCodec();
    private final ArrayBlockingQueue<NetworkMessage> outgoing = new ArrayBlockingQueue<>(64);
    private final AtomicBoolean closed = new AtomicBoolean();
    private Thread reader, writer;
    public TcpConnection(Socket socket) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(15000);
    }
    public static TcpConnection connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        try { socket.connect(new InetSocketAddress(host, port), 4000); return new TcpConnection(socket); }
        catch (IOException | RuntimeException e) { socket.close(); throw e; }
    }
    public void start(Consumer<NetworkMessage> receive, Consumer<Exception> failure) {
        writer = Thread.ofVirtual().name("lan-send").start(() -> {
            try { while (!closed.get()) codec.write(socket.getOutputStream(), outgoing.take()); }
            catch (Exception e) { fail(e, failure); }
        });
        reader = Thread.ofVirtual().name("lan-receive").start(() -> {
            try { while (!closed.get()) receive.accept(codec.read(socket.getInputStream())); }
            catch (Exception e) { fail(e, failure); }
        });
    }
    private void fail(Exception e, Consumer<Exception> failure) {
        if (closed.compareAndSet(false, true)) {
            shutdown(); failure.accept(e);
        }
    }
    @Override public void send(NetworkMessage message) throws IOException {
        if (closed.get() || !outgoing.offer(message)) throw new IOException("The connection is closed or the send queue is full.");
    }
    @Override public void close() {
        if (closed.compareAndSet(false, true)) shutdown();
    }
    private void shutdown() {
        try { socket.close(); } catch (IOException ignored) { /* Already disconnected. */ }
        if (reader != null) reader.interrupt();
        if (writer != null) writer.interrupt();
    }
}
