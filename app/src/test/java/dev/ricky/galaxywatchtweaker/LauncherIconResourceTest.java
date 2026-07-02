package dev.ricky.galaxywatchtweaker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public final class LauncherIconResourceTest {
    @Test
    public void launcherIconUsesSmartwatchVisualLanguage() throws Exception {
        String foreground = read("app/src/main/res/drawable/ic_launcher_foreground.xml");
        String background = read("app/src/main/res/drawable/ic_launcher_background.xml");
        String adaptiveIcon = read("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml");

        assertTrue(foreground.contains("smartwatch body"));
        assertTrue(foreground.contains("watch crown"));
        assertTrue(foreground.contains("connection nodes"));
        assertFalse(foreground.contains("#F7C948"));
        assertTrue(background.contains("#111827"));
        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_foreground"));
    }

    private static String read(String path) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).getParent();
        return new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
    }
}
