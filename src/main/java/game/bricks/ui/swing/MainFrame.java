package game.bricks.ui.swing;

import game.bricks.application.GameSessionService;
import game.bricks.application.RankingService;
import game.bricks.application.dto.SessionView;
import game.bricks.domain.game.Difficulty;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class MainFrame extends JFrame {
    private static final Color BLUE = new Color(32, 112, 225), NAVY = new Color(24, 44, 67), BG = new Color(240, 244, 249);
    private final GameSessionService session;
    private final RankingService ranking;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JTextField player = new JTextField("PLAYER01"), endpoint = new JTextField("192.168.0.12:5000");
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values()), rankDifficulty = new JComboBox<>(Difficulty.values());
    private final JSpinner port;
    private final JComboBox<String> addresses = new JComboBox<>();
    private final JLabel status = new JLabel("Choose your player ID and difficulty."), players = new JLabel("Open a server or join using your friend's address."), gameTitle = new JLabel("Single player");
    private final JButton ready, retry, open, connect;
    private final GamePanel game = new GamePanel();
    private final DefaultTableModel singles = model("Rank", "ID", "Score", "Level", "Time"), standings = model("Rank", "ID", "Wins", "Draws", "Losses");
    private final JLabel rankStatus = new JLabel("Records stored on this PC.");
    private String current = "MENU";
    private long rankGeneration;

    public MainFrame(String title, int width, int height, int defaultPort, GameSessionService session, RankingService ranking) {
        super(title);
        this.session = session; this.ranking = ranking;
        difficulty.setSelectedItem(Difficulty.NORMAL); rankDifficulty.setSelectedItem(Difficulty.NORMAL);
        player.setName("player-id"); endpoint.setName("endpoint"); difficulty.setName("difficulty");
        port = new JSpinner(new SpinnerNumberModel(defaultPort, 1, 65535, 1));
        port.setName("port");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 720)); setSize(Math.max(1000, width), Math.max(720, height)); setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(0, 0)); root.setBackground(BG);
        JPanel header = row(); header.setBorder(new EmptyBorder(18, 28, 18, 28));
        JLabel brand = new JLabel("LAN BRICKS"); brand.setFont(brand.getFont().deriveFont(Font.BOLD, 23)); brand.setForeground(NAVY);
        header.add(brand); header.add(Box.createHorizontalGlue());
        header.add(button("menu", "Menu", false, this::back));
        header.add(button("help", "Help", false, () -> show("HELP")));
        root.add(header, BorderLayout.NORTH); root.add(content, BorderLayout.CENTER);
        status.setBorder(new EmptyBorder(12, 28, 14, 28)); status.setForeground(new Color(67, 85, 107)); root.add(status, BorderLayout.SOUTH);
        setContentPane(root);

        JPanel menu = column(); menu.setBorder(new EmptyBorder(36, 50, 24, 50));
        JLabel titleLabel = new JLabel("Break your record. Challenge a friend."); titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 30)); titleLabel.setForeground(NAVY);
        menu.add(titleLabel); menu.add(Box.createVerticalStrut(12)); menu.add(new JLabel("Chase your high score or challenge a friend with items.")); menu.add(Box.createVerticalStrut(32));
        menu.add(field("Player ID", player)); menu.add(Box.createVerticalStrut(18)); menu.add(field("Difficulty", difficulty)); menu.add(Box.createVerticalStrut(28));
        menu.add(button("single", "Start single player", true, () -> attempt(() -> session.single(player.getText(), selectedDifficulty()))));
        menu.add(Box.createVerticalStrut(12)); menu.add(button("lan", "LAN versus", false, () -> show("LOBBY")));
        menu.add(Box.createVerticalStrut(12)); menu.add(button("ranking", "Rankings & results", false, () -> { show("RANKING"); refreshRanking(); }));
        content.add(center(menu, 600), "MENU");

        JPanel lobby = column(); lobby.setBorder(new EmptyBorder(22, 40, 22, 40));
        lobby.add(heading("LAN versus")); lobby.add(Box.createVerticalStrut(16));
        lobby.add(new JLabel("On the same network, one player hosts and the other joins by address.")); lobby.add(Box.createVerticalStrut(24));
        JPanel hostRow = row(); hostRow.add(new JLabel("Server port  ")); hostRow.add(port); hostRow.add(Box.createHorizontalStrut(12));
        open = button("host", "Open LAN server", true, () -> attempt(() -> session.host(player.getText(), selectedDifficulty(), (Integer) port.getValue())));
        hostRow.add(open); lobby.add(hostRow); lobby.add(Box.createVerticalStrut(16));
        JPanel addressRow = row(); addresses.setPrototypeDisplayValue("192.168.100.100:50000"); addressRow.add(addresses);
        addressRow.add(button("copy-address", "Copy address", false, () -> {
            Object value = addresses.getSelectedItem();
            if (value != null) { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value.toString()), null); status.setText("Address copied. Share it with your friend."); }
        })); lobby.add(addressRow); lobby.add(Box.createVerticalStrut(24));
        lobby.add(field("Server address (IP:port)", endpoint)); lobby.add(Box.createVerticalStrut(12));
        connect = button("connect", "Join LAN game", true, () -> attempt(() -> session.join(player.getText(), endpoint.getText())));
        lobby.add(connect); lobby.add(Box.createVerticalStrut(26)); lobby.add(players); lobby.add(Box.createVerticalStrut(16));
        ready = button("ready", "Ready", true, session::ready); ready.setEnabled(false); lobby.add(ready);
        lobby.add(Box.createVerticalStrut(12)); lobby.add(button("close-server", "Disconnect & menu", false, () -> { session.leave(); show("MENU"); }));
        content.add(center(lobby, 740), "LOBBY");

        JPanel gamePage = new JPanel(new BorderLayout()); gamePage.setBackground(BG);
        JPanel top = row(); top.setBorder(new EmptyBorder(4, 32, 8, 32)); gameTitle.setFont(gameTitle.getFont().deriveFont(Font.BOLD, 20)); top.add(gameTitle);
        top.add(Box.createHorizontalGlue()); top.add(new JLabel("LEFT / RIGHT: move     SPACE: use oldest item")); gamePage.add(top, BorderLayout.NORTH);
        gamePage.add(game, BorderLayout.CENTER);
        JPanel bottom = row(); bottom.setBorder(new EmptyBorder(4, 32, 4, 32));
        bottom.add(new JLabel("Catch falling items with your paddle. Store up to 3.")); bottom.add(Box.createHorizontalGlue());
        retry = button("retry", "Play again", true, session::restart); retry.setEnabled(false); bottom.add(retry);
        gamePage.add(bottom, BorderLayout.SOUTH); content.add(gamePage, "GAME");

        JPanel ranks = new JPanel(new BorderLayout(0, 16)); ranks.setBackground(BG); ranks.setBorder(new EmptyBorder(18, 32, 24, 32));
        JPanel rankTop = row(); rankTop.add(heading("Rankings & results")); rankTop.add(Box.createHorizontalGlue()); rankTop.add(rankDifficulty);
        rankTop.add(button("refresh-rank", "Refresh", false, this::refreshRanking)); ranks.add(rankTop, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane(); tabs.addTab("Single TOP 10", table(singles)); tabs.addTab("LAN standings", table(standings)); ranks.add(tabs, BorderLayout.CENTER); ranks.add(rankStatus, BorderLayout.SOUTH);
        rankDifficulty.addActionListener(event -> { if (current.equals("RANKING")) refreshRanking(); }); content.add(ranks, "RANKING");

        JTextArea help = new JTextArea("CONTROLS\n\nLEFT / RIGHT: move your paddle\nSPACE: use the oldest item (3 inventory slots, 3-second cooldown)\n\n"
                + "ITEMS\n\nHide ball: hide the opponent's ball for 2 seconds\nShrink: reduce the opponent's paddle for 4 seconds\nShield: clear an attack and block the next one\nExtend: enlarge your paddle for 5 seconds\n\n"
                + "LAN PLAY\n\nConnect both PCs to the same Wi-Fi or wired network. The host opens a\nserver and shares the displayed IP:port. The guest pastes it to join.\n"
                + "For two windows on one PC, use 127.0.0.1:port.\nIf connection fails, check the address, port and Windows firewall.\nA server running inside WSL may need additional network configuration.\n\n"
                + "A versus match lasts 180 seconds. Losing all lives ends the match.\nAt the time limit, score wins; remaining lives break a tie.\nInterrupted matches are not recorded.\n\n"
                + "Rankings are local to this PC. LAN results are stored on the host PC.");
        help.setEditable(false); help.setFont(help.getFont().deriveFont(16f)); help.setBackground(BG); help.setBorder(new EmptyBorder(25, 40, 25, 40));
        content.add(new JScrollPane(help), "HELP");

        InputController input = new InputController(getRootPane(), session);
        addWindowFocusListener(new WindowAdapter() { @Override public void windowLostFocus(WindowEvent event) { input.reset(); } });
        addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent event) { session.close(); } });
        session.setListener(view -> SwingUtilities.invokeLater(() -> { if (isDisplayable() || !isVisible()) render(view); }));
    }

    private void render(SessionView view) {
        status.setText(view.status());
        if (view.game() != null) {
            show("GAME"); game.show(view.game(), view.localPlayer()); retry.setEnabled(view.game().finished());
            String timer = view.game().versus() ? String.format(Locale.ROOT, " | %03ds remaining", (int) Math.ceil(Math.max(0, 180 - view.game().elapsed()))) : "";
            gameTitle.setText((view.game().versus() ? "LAN versus" : "Single player") + " · " + Difficulty.valueOf(view.game().difficulty()).label + timer);
        } else if (!view.mode().equals("IDLE")) show("LOBBY");
        else if (current.equals("GAME")) show("MENU");
        boolean idle = view.mode().equals("IDLE"); open.setEnabled(idle); connect.setEnabled(idle);
        ready.setEnabled(view.lobby() != null && !view.lobby().guest().isEmpty());
        if (view.lobby() != null) {
            var lobby = view.lobby();
            players.setText("P1 " + lobby.host() + (lobby.hostReady() ? "  ready" : "  not ready") + "       P2 "
                    + (lobby.guest().isEmpty() ? "Waiting for guest" : lobby.guest() + (lobby.guestReady() ? "  ready" : "  not ready")) + "       " + Difficulty.valueOf(lobby.difficulty()).label);
            ready.setText((view.localPlayer() == 0 ? lobby.hostReady() : lobby.guestReady()) ? "Not ready" : "Ready");
        } else players.setText(view.mode().equals("CONNECTING") ? "Connecting to the server..." : "Open a server or join using your friend's address.");
        if (view.mode().equals("HOST") && addresses.getItemCount() == 0) {
            for (String address : session.addresses()) addresses.addItem(address);
        } else if (!view.mode().equals("HOST")) addresses.removeAllItems();
    }
    private void back() {
        SessionView view = session.view();
        if (view.game() != null && !view.game().finished()
                && JOptionPane.showConfirmDialog(this, "Leave the current match and return to the menu?", "Leave match", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        session.leave(); show("MENU");
    }
    private void show(String name) { current = name; cards.show(content, name); }
    private Difficulty selectedDifficulty() { return (Difficulty) difficulty.getSelectedItem(); }
    private interface Task { void run() throws Exception; }
    private void attempt(Task task) {
        try { task.run(); } catch (Exception e) { status.setText(e.getMessage()); }
    }
    private void refreshRanking() {
        long token = ++rankGeneration; Difficulty selected = (Difficulty) rankDifficulty.getSelectedItem();
        rankStatus.setText("Loading records...");
        CompletableFuture.runAsync(() -> {
            try {
                var first = ranking.single(selected); var second = ranking.versus(selected);
                SwingUtilities.invokeLater(() -> {
                    if (token != rankGeneration) return;
                    singles.setRowCount(0); standings.setRowCount(0); int index = 1;
                    for (var entry : first) singles.addRow(new Object[]{index++, entry.id(), entry.score(), entry.level(), String.format(Locale.ROOT, "%.1fs", entry.elapsed())});
                    index = 1;
                    for (var entry : second) standings.addRow(new Object[]{index++, entry.id(), entry.wins(), entry.draws(), entry.losses()});
                    rankStatus.setText(first.isEmpty() && second.isEmpty() ? "No records yet. Complete a single or LAN match to begin." : "Local records by difficulty | LAN results are stored on the host PC");
                });
            } catch (RuntimeException e) { SwingUtilities.invokeLater(() -> { if (token == rankGeneration) rankStatus.setText(e.getMessage()); }); }
        });
    }
    private static JLabel heading(String text) { JLabel label = new JLabel(text); label.setFont(label.getFont().deriveFont(Font.BOLD, 25)); label.setForeground(NAVY); return label; }
    private static JPanel row() { JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS)); panel.setOpaque(false); return panel; }
    private static JPanel column() { JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setOpaque(false); return panel; }
    private static JPanel center(JPanel panel, int width) {
        JPanel outer = new JPanel(new GridBagLayout()); outer.setBackground(BG);
        panel.setPreferredSize(new Dimension(width, 550)); outer.add(panel); return outer;
    }
    private static JPanel field(String label, JComponent input) {
        JPanel group = column(); JLabel caption = new JLabel(label); caption.setBorder(new EmptyBorder(0, 0, 8, 0));
        caption.setLabelFor(input); group.add(caption); input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); input.setPreferredSize(new Dimension(400, 40)); group.add(input);
        group.setAlignmentX(Component.LEFT_ALIGNMENT); group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); return group;
    }
    private static JButton button(String name, String label, boolean primary, Runnable action) {
        JButton button = new JButton(label); button.setName(name); button.setFocusPainted(true); button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15)); button.setMargin(new Insets(10, 18, 10, 18));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        if (primary) { button.setBackground(BLUE); button.setForeground(Color.WHITE); }
        button.addActionListener(event -> action.run()); return button;
    }
    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }
    private static JScrollPane table(DefaultTableModel model) {
        JTable table = new JTable(model); table.setRowHeight(38); table.setFont(table.getFont().deriveFont(15f)); table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD, 15));
        table.setFillsViewportHeight(true); table.setShowGrid(false); return new JScrollPane(table);
    }
}
