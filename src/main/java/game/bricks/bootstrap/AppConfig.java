package game.bricks.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public record AppConfig(String title, int width, int height, int port, Path dataDirectory) {

    public AppConfig {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("game.title must not be blank");
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("game.width and game.height must be positive");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("network.port must be between 1 and 65535");
        }

        if (dataDirectory == null) {
            throw new IllegalArgumentException("app.data-directory must not be null");
        }

        title = title.trim();
        dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    public static AppConfig load() throws IOException {
        return load(System.getProperties());
    }

    static AppConfig load(Properties overrides) throws IOException {
        Properties properties = new Properties();

        try (InputStream stream = AppConfig.class.getResourceAsStream("/game.properties")) {
            if (stream == null) {
                throw new IOException("game.properties is missing");
            }

            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        for (String key : List.of("game.title", "game.width", "game.height",
                "network.port", "app.data-directory")) {
            if (overrides.getProperty(key) != null) {
                properties.setProperty(key, overrides.getProperty(key));
            }
        }

        return from(properties);
    }

    static AppConfig from(Properties properties) {
        String directory = properties.getProperty("app.data-directory");

        if (directory != null && directory.isBlank()) {
            throw new IllegalArgumentException("app.data-directory must not be blank");
        }

        Path dataDirectory = directory == null
                ? Path.of(System.getProperty("user.home"), ".lan-bricks")
                : Path.of(directory);

        return new AppConfig(
                properties.getProperty("game.title"),
                integer(properties, "game.width"),
                integer(properties, "game.height"),
                integer(properties, "network.port"),
                dataDirectory
        );
    }

    private static int integer(Properties properties, String key) {
        String value = properties.getProperty(key);

        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer: " + value, e);
        }
    }
}
