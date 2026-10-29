package game.bricks.ui.swing;

import game.bricks.application.dto.GameSnapshot;
import game.bricks.domain.item.ItemType;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

public final class GamePanel extends JPanel {

    private static final Color NAVY = new Color(13, 30, 51);
    private static final Color BLUE = new Color(32, 112, 225);
    private static final Color ORANGE = new Color(233, 132, 40);
    private static final Color[] BRICKS = {new Color(239, 101, 107), new Color(242, 166, 69), new Color(234, 201, 84),
        new Color(73, 186, 147), new Color(66, 157, 220), new Color(134, 116, 208), new Color(187, 115, 189)};
    private GameSnapshot snapshot;
    private int localPlayer;

    public GamePanel() {
        setBackground(new Color(240, 244, 249));
    }

    public void show(GameSnapshot snapshot, int localPlayer) {
        this.snapshot = snapshot;
        this.localPlayer = localPlayer;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (snapshot == null)
            return;
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int count = snapshot.boards().size();
            double width = Math.min(
                (getWidth() - 32.0 * (count + 1)) / count,
                Math.max(100, getHeight() - 130.0) * 4 / 3
            );
            double fieldHeight = width * 3 / 4;
            double total = count * width + (count - 1) * 24;
            double start = (getWidth() - total) / 2;

            for (int i = 0; i < count; i++)
                drawBoard(g, snapshot.boards().get(i), i, start + i * (width + 24), 10, width, fieldHeight);
            if (snapshot.finished()) {
                g.setColor(new Color(13, 30, 51, 218));
                g.fillRect(0, 0, getWidth(), getHeight());
                String result = !snapshot.versus() ? "Game over" : snapshot.winner() < 0 ? "Draw" : snapshot.boards()
                    .get(snapshot.winner()).id() + " wins";
                g.setFont(getFont().deriveFont(Font.BOLD, 34));
                g.setColor(Color.WHITE);
                centered(g, result, getWidth() / 2.0, getHeight() / 2.0 - 22);
                g.setFont(getFont().deriveFont(17f));
                centered(g, "Play again below, or return to the menu.", getWidth() / 2.0, getHeight() / 2.0 + 15);
            }
        } finally {
            g.dispose();
        }
    }

    private void drawBoard(
        Graphics2D g,
        GameSnapshot.Board board,
        int index,
        double x,
        double y,
        double width,
        double height
    ) {
        Color accent = index == 0 ? BLUE : ORANGE;
        g.setFont(getFont().deriveFont(Font.BOLD, 16));
        g.setColor(new Color(24, 44, 67));
        g.drawString(
            "P" + (index + 1) + "  " + board.id() + (index == localPlayer ? "  (you)" : ""),
            (float) x,
            (float) (y + 18)
        );
        g.setFont(getFont().deriveFont(14f));
        g.drawString("Score " + String.format(Locale.ROOT, "%,d", board.score()), (float) x, (float) (y + 42));
        String lives = "Lives " + board.lives() + "  |  Level " + board.level();
        g.drawString(lives, (float) (x + width - g.getFontMetrics().stringWidth(lives)), (float) (y + 42));
        double top = y + 54;
        g.setColor(NAVY);
        g.fill(new RoundRectangle2D.Double(x, top, width, height, 12, 12));
        Graphics2D field = (Graphics2D) g.create();

        try {
            field.translate(x, top);
            field.scale(width / 800, height / 600);

            for (var brick : board.bricks()) {
                int row = Math.max(0, (int) ((brick.y() - 50) / 30));
                field.setColor(BRICKS[row % BRICKS.length]);
                field.fill(new RoundRectangle2D.Double(brick.x(), brick.y(), brick.width(), brick.height(), 5, 5));
            }

            field.setColor(accent);
            field.fill(new RoundRectangle2D.Double(
                board.paddleX(),
                board.paddleY(),
                board.paddleWidth(),
                board.paddleHeight(),
                8,
                8
            ));

            if (board.shield() > 0) {
                field.setColor(new Color(108, 229, 183));
                field.setStroke(new BasicStroke(3));
                field.draw(new RoundRectangle2D.Double(
                    board.paddleX() - 5,
                    board.paddleY() - 5,
                    board.paddleWidth() + 10,
                    board.paddleHeight() + 10,
                    12,
                    12
                ));
            }

            if (board.hidden() <= 0) {
                field.setColor(Color.WHITE);
                field.fillOval(
                    (int) (board.ballX() - board.radius()),
                    (int) (board.ballY() - board.radius()),
                    (int) (board.radius() * 2),
                    (int) (board.radius() * 2)
                );
            } else {
                field.setFont(getFont().deriveFont(Font.BOLD, 28));
                field.setColor(new Color(255, 192, 105));
                centered(field, String.format(Locale.ROOT, "Ball hidden %.1fs", board.hidden()), 400, 360);
            }

            for (var drop : board.drops()) {
                field.setColor(new Color(247, 249, 253));
                field.fillRoundRect((int) drop.x() - 25, (int) drop.y() - 15, 50, 30, 8, 8);
                field.setColor(NAVY);
                field.setFont(getFont().deriveFont(Font.BOLD, 19));
                centered(field, drop.type().substring(0, 3), drop.x(), drop.y() + 7);
            }
        } finally {
            field.dispose();
        }

        double slotTop = top + height + 10;
        double slotWidth = (width - 16) / 3;

        for (int i = 0; i < 3; i++) {
            double slotX = x + i * (slotWidth + 8);
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(slotX, slotTop, slotWidth, 46, 10, 10));
            g.setColor(new Color(214, 224, 236));
            g.draw(new RoundRectangle2D.Double(slotX, slotTop, slotWidth, 46, 10, 10));
            g.setFont(getFont().deriveFont(i == 0 ? Font.BOLD : Font.PLAIN, 13));
            g.setColor(i < board.inventory().size() ? NAVY : new Color(126, 141, 159));
            String label = i < board.inventory().size() ? ItemType.valueOf(board.inventory().get(i)).label : "Empty";
            centered(g, label, slotX + slotWidth / 2, slotTop + 28);
        }

        g.setFont(getFont().deriveFont(12f));
        g.setColor(new Color(78, 98, 121));
        String footer = board.cooldown() > 0 ? String.format(
            Locale.ROOT,
            "Cooldown %.1fs",
            board.cooldown()
        ) : "SPACE: use the oldest item";
        g.drawString(footer, (float) x, (float) (slotTop + 65));
    }

    private void centered(Graphics2D g, String text, double x, double y) {
        g.drawString(text, (float) (x - g.getFontMetrics().stringWidth(text) / 2.0), (float) y);
    }
}
