package game.bricks.bootstrap;

import com.formdev.flatlaf.FlatLightLaf;
import game.bricks.ui.swing.MainFrame;
import game.bricks.application.GameSessionService;
import game.bricks.application.RankingService;
import game.bricks.infrastructure.persistence.FileRankingRepository;
import game.bricks.infrastructure.network.TcpConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.Arrays;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        final AppConfig config;

        try {
            config = AppConfig.load();
        } catch (IOException | IllegalArgumentException e) {
            log.error("Invalid application configuration: {}", e.getMessage());
            System.exit(1);
            return;
        }

        boolean smokeTest = Arrays.asList(args).contains("--smoke-test");
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            RankingService ranking = new RankingService(new FileRankingRepository(config.dataDirectory()));
            GameSessionService session = new GameSessionService(ranking, new TcpConnectionFactory());
            MainFrame frame = new MainFrame(config.title(), config.width(), config.height(), config.port(), session, ranking);
            DesktopApplication application = new DesktopApplication(config, frame);

            application.start(smokeTest);
        });
    }
}
