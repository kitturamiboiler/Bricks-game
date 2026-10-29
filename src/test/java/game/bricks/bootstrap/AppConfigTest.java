package game.bricks.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void loadsPackagedSettingsAndUsesUserHomeForData() throws Exception {
        AppConfig config = AppConfig.load(new Properties());

        assertAll(
                () -> assertEquals("LAN Bricks", config.title()),
                () -> assertTrue(config.width() > 0 && config.height() > 0),
                () -> assertEquals(5000, config.port()),
                () -> assertEquals(Path.of(System.getProperty("user.home"), ".lan-bricks")
                        .toAbsolutePath().normalize(), config.dataDirectory())
        );
    }

    @Test
    void runtimeOptionsOverrideDefaultsAndPreserveKoreanText() throws Exception {
        Properties overrides = new Properties();
        overrides.setProperty("game.title", " 벽돌깨기 ");
        overrides.setProperty("network.port", "5001");
        overrides.setProperty("app.data-directory", "target/test-data/../runtime");

        AppConfig config = AppConfig.load(overrides);

        assertAll(
                () -> assertEquals("벽돌깨기", config.title()),
                () -> assertEquals(5001, config.port()),
                () -> assertEquals(1100, config.width()),
                () -> assertEquals(Path.of("target/runtime").toAbsolutePath(), config.dataDirectory())
        );
    }

    @ParameterizedTest
    @CsvSource({"game.width,0", "game.height,-1", "network.port,0", "network.port,65536",
            "game.width,not-a-number", "network.port,2147483648"})
    void rejectsInvalidNumbersWithTheSettingName(String key, String value) {
        Properties properties = validProperties();
        properties.setProperty(key, value);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.from(properties));

        assertTrue(error.getMessage().contains(key));
    }

    @ParameterizedTest
    @CsvSource({"game.title", "app.data-directory"})
    void rejectsBlankText(String key) {
        Properties properties = validProperties();
        properties.setProperty(key, "  ");

        assertThrows(IllegalArgumentException.class, () -> AppConfig.from(properties));
    }

    @Test
    void rejectsMissingRequiredSetting() {
        Properties properties = validProperties();
        properties.remove("game.width");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AppConfig.from(properties));

        assertTrue(error.getMessage().contains("game.width"));
    }

    private Properties validProperties() {
        Properties properties = new Properties();
        properties.setProperty("game.title", "LAN Bricks");
        properties.setProperty("game.width", "1100");
        properties.setProperty("game.height", "720");
        properties.setProperty("network.port", "5000");
        return properties;
    }
}
