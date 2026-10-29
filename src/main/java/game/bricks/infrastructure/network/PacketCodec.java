package game.bricks.infrastructure.network;

import game.bricks.application.dto.GameSnapshot;
import game.bricks.application.dto.NetworkMessage;
import game.bricks.domain.game.Difficulty;
import game.bricks.domain.item.ItemType;
import game.bricks.domain.room.PlayerId;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PacketCodec {

    public static final int MAX_PACKET = 65536;

    public void write(OutputStream stream, NetworkMessage message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(1);

        switch (message) {
            case NetworkMessage.Hello m -> {
                out.writeByte(1);
                out.writeUTF(m.id());
            }

            case NetworkMessage.Lobby m -> {
                out.writeByte(2);
                out.writeUTF(m.host());
                out.writeUTF(m.guest());
                out.writeUTF(m.difficulty());
                out.writeBoolean(m.hostReady());
                out.writeBoolean(m.guestReady());
            }

            case NetworkMessage.Ready m -> {
                out.writeByte(3);
                out.writeBoolean(m.ready());
            }

            case NetworkMessage.Input m -> {
                out.writeByte(4);
                out.writeLong(m.sequence());
                out.writeBoolean(m.left());
                out.writeBoolean(m.right());
                out.writeBoolean(m.useItem());
            }

            case NetworkMessage.State m -> {
                out.writeByte(5);
                writeState(out, m.snapshot());
            }

            case NetworkMessage.Failure m -> {
                out.writeByte(6);
                out.writeUTF(m.reason());
            }

            case NetworkMessage.Leave ignored -> out.writeByte(7);
        }

        if (bytes.size() > MAX_PACKET)
            throw new IOException("The outgoing packet is too large.");
        DataOutputStream target = new DataOutputStream(stream);
        target.writeInt(bytes.size());
        target.write(bytes.toByteArray());
        target.flush();
    }

    public NetworkMessage read(InputStream stream) throws IOException {
        DataInputStream source = new DataInputStream(stream);
        int length = source.readInt();

        if (length < 2 || length > MAX_PACKET)
            throw new IOException("Invalid packet length.");
        byte[] bytes = source.readNBytes(length);

        if (bytes.length != length)
            throw new EOFException("Connection closed during a packet.");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

        if (in.readUnsignedByte() != 1)
            throw new IOException("The game versions do not match.");
        try {
            NetworkMessage message = switch (in.readUnsignedByte()) {
                case 1 -> new NetworkMessage.Hello(new PlayerId(in.readUTF()).value());
                case 2 -> {
                    String host = new PlayerId(in.readUTF()).value();
                    String guest = in.readUTF();

                    if (!guest.isEmpty())
                        new PlayerId(guest);
                    String difficulty = Difficulty.valueOf(in.readUTF()).name();
                    yield new NetworkMessage.Lobby(host, guest, difficulty, in.readBoolean(), in.readBoolean());
                }

                case 3 -> new NetworkMessage.Ready(in.readBoolean());
                case 4 -> {
                    long sequence = in.readLong();

                    if (sequence < 0)
                        throw new IOException("Invalid input sequence.");
                    yield new NetworkMessage.Input(sequence, in.readBoolean(), in.readBoolean(), in.readBoolean());
                }

                case 5 -> new NetworkMessage.State(readState(in));
                case 6 -> new NetworkMessage.Failure(in.readUTF());
                case 7 -> new NetworkMessage.Leave();
                default -> throw new IOException("Unknown message type.");
            };

            if (in.available() != 0)
                throw new IOException("Unexpected trailing packet data.");
            return message;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid game data.", e);
        }
    }

    private void writeState(DataOutputStream out, GameSnapshot state) throws IOException {
        out.writeUTF(state.matchId());
        out.writeBoolean(state.versus());
        out.writeUTF(state.difficulty());
        out.writeBoolean(state.finished());
        out.writeInt(state.winner());
        out.writeDouble(state.elapsed());
        out.writeInt(state.boards().size());

        for (var b : state.boards()) {
            out.writeUTF(b.id());

            for (double value : new double[]{b.ballX(), b.ballY(), b.radius(), b.paddleX(), b.paddleY(), b.paddleWidth(), b.paddleHeight()})
                out.writeDouble(value);
            out.writeInt(b.score());
            out.writeInt(b.lives());
            out.writeInt(b.level());
            out.writeDouble(b.hidden());
            out.writeDouble(b.shield());
            out.writeDouble(b.cooldown());
            out.writeInt(b.bricks().size());

            for (var tile : b.bricks()) {
                out.writeDouble(tile.x());
                out.writeDouble(tile.y());
                out.writeDouble(tile.width());
                out.writeDouble(tile.height());
            }

            out.writeInt(b.inventory().size());

            for (String type : b.inventory())
                out.writeUTF(type);
            out.writeInt(b.drops().size());

            for (var drop : b.drops()) {
                out.writeDouble(drop.x());
                out.writeDouble(drop.y());
                out.writeUTF(drop.type());
            }
        }
    }

    private GameSnapshot readState(DataInputStream in) throws IOException {
        String id = in.readUTF();
        UUID.fromString(id);
        boolean versus = in.readBoolean();
        String difficulty = Difficulty.valueOf(in.readUTF()).name();
        boolean finished = in.readBoolean();
        int winner = bounded(in.readInt(), -1, 1);
        double elapsed = number(in, 0, 86400);
        int count = bounded(in.readInt(), 1, 2);

        if (count != (versus ? 2 : 1) || winner >= count)
            throw new IOException("Invalid board count.");
        List<GameSnapshot.Board> boards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String player = new PlayerId(in.readUTF()).value();
            double x = number(in, -40, 840);
            double y = number(in, -40, 640);
            double radius = number(in, 1, 40);
            double px = number(in, 0, 800);
            double py = number(in, 0, 600);
            double pw = number(in, 1, 800);
            double ph = number(in, 1, 100);
            int score = bounded(in.readInt(), 0, Integer.MAX_VALUE);
            int lives = bounded(in.readInt(), 0, 5);
            int level = bounded(in.readInt(), 1, 100000);
            double hidden = number(in, 0, 2);
            double shield = number(in, 0, 5);
            double cooldown = number(in, 0, 3);
            int tiles = bounded(in.readInt(), 0, 70);
            List<GameSnapshot.Tile> bricks = new ArrayList<>();

            for (int j = 0; j < tiles; j++)
                bricks.add(new GameSnapshot.Tile(
                    number(in, 0, 800),
                    number(in, 0, 600),
                    number(in, 1, 800),
                    number(in, 1, 100)
                ));
            int items = bounded(in.readInt(), 0, 3);
            List<String> inventory = new ArrayList<>();

            for (int j = 0; j < items; j++)
                inventory.add(ItemType.valueOf(in.readUTF()).name());
            int size = bounded(in.readInt(), 0, 128);
            List<GameSnapshot.Drop> drops = new ArrayList<>();

            for (int j = 0; j < size; j++)
                drops.add(new GameSnapshot.Drop(
                    number(in, -40, 840),
                    number(in, -40, 640),
                    ItemType.valueOf(in.readUTF()).name()
                ));
            boards.add(new GameSnapshot.Board(
                player,
                x,
                y,
                radius,
                px,
                py,
                pw,
                ph,
                score,
                lives,
                level,
                hidden,
                shield,
                cooldown,
                bricks,
                inventory,
                drops
            ));
        }

        return new GameSnapshot(id, versus, difficulty, finished, winner, elapsed, boards);
    }

    private int bounded(int value, int min, int max) throws IOException {
        if (value < min || value > max)
            throw new IOException("Game data is outside the allowed range.");
        return value;
    }

    private double number(DataInputStream in, double min, double max) throws IOException {
        double value = in.readDouble();

        if (!Double.isFinite(value) || value < min || value > max)
            throw new IOException("Invalid position or timer value.");
        return value;
    }
}
