package game.bricks.domain;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainBoundaryTest {

    @Test
    void domainDoesNotImportUiNetworkingPersistenceOrOuterLayers() throws Exception {
        Pattern outerDependency = Pattern.compile(
                "(?m)^import\\s+(?:static\\s+)?(?:javafx|javax\\.swing|java\\.awt|java\\.net|java\\.io|"
                        + "java\\.nio\\.file|game\\.bricks\\.(?:ui|infrastructure|application|bootstrap))\\.");

        assertTrue(outerDependency.matcher("import javax.swing.JPanel;").find());
        assertTrue(outerDependency.matcher("import javafx.scene.canvas.GraphicsContext;").find());
        assertFalse(outerDependency.matcher("import java.util.List;").find());

        try (var files = Files.walk(Path.of("src/main/java/game/bricks/domain"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(outerDependency.matcher(Files.readString(file)).find(), file.toString());
            }
        }
    }
}
