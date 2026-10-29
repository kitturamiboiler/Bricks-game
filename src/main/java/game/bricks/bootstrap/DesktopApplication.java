package game.bricks.bootstrap;

import game.bricks.ui.swing.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import java.util.Objects;

public final class DesktopApplication {

    private static final Logger log = LoggerFactory.getLogger(DesktopApplication.class);

    private final AppConfig config;
    private final MainFrame frame;

    public DesktopApplication(AppConfig config, MainFrame frame) {
        this.config = Objects.requireNonNull(config);
        this.frame = Objects.requireNonNull(frame);
    }

    public void start(boolean smokeTest) {
        frame.setVisible(true);

        log.info("Swing window opened; data directory: {}", config.dataDirectory());
        log.info("Default LAN port: {} (opened only by user action)", config.port());

        if (smokeTest) {
            Timer closeTimer = new Timer(1500, event -> {
                frame.dispose();
                log.info("Swing smoke test completed");
            });

            closeTimer.setRepeats(false);
            closeTimer.start();
        }
    }
}
