package game.bricks.ui.swing;

import game.bricks.application.GameSessionService;
import javax.swing.*;
import java.awt.event.ActionEvent;

public final class InputController {
    private boolean left, right, space;
    private final GameSessionService session;
    public InputController(JRootPane root, GameSessionService session) {
        this.session = session;
        bind(root, "pressed LEFT", () -> { left = true; send(); });
        bind(root, "released LEFT", () -> { left = false; send(); });
        bind(root, "pressed RIGHT", () -> { right = true; send(); });
        bind(root, "released RIGHT", () -> { right = false; send(); });
        bind(root, "pressed SPACE", () -> { if (!space) session.useItem(); space = true; });
        bind(root, "released SPACE", () -> space = false);
    }
    private void bind(JRootPane root, String key, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        root.getActionMap().put(key, new AbstractAction() { @Override public void actionPerformed(ActionEvent event) { action.run(); } });
    }
    private void send() { session.keys(left, right); }
    public void reset() { left = false; right = false; space = false; send(); }
}
