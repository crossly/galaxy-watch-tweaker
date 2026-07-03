package dev.ricky.galaxywatchtweaker;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public final class WatchDisplayNameSourceTest {
    @Test
    public void preferencesExposeIndependentWatchAliasOverride() throws Exception {
        String preferences = read("app/src/main/java/dev/ricky/galaxywatchtweaker/settings/TweakerPreferences.java");

        assertTrue(preferences.contains("KEY_WATCH_ALIAS_OVERRIDE_ENABLED"));
        assertTrue(preferences.contains("KEY_WATCH_ALIAS_NAME"));
    }

    @Test
    public void hookEntryInstallsWatchDisplayNameHookOnlyForWatch7Plugin() throws Exception {
        String entry = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");

        assertTrue(entry.contains("config.isWatchAliasOverrideEnabled()"));
        assertTrue(entry.contains("WatchDisplayNameHook.install"));
        assertTrue(entry.contains("config.watchAliasName()"));
    }

    @Test
    public void watchDisplayNameHookTargetsSamsungAliasGetter() throws Exception {
        String hook = read("app/src/main/java/dev/ricky/galaxywatchtweaker/WatchDisplayNameHook.java");

        assertTrue(hook.contains("com.samsung.android.plugin.sharedlib.utils.SharedCommonUtils"));
        assertTrue(hook.contains("getAliasName"));
        assertTrue(hook.contains("Context.class"));
        assertTrue(hook.contains("WatchAliasPolicy.aliasFor"));
    }

    @Test
    public void uiExposesWatchAliasOverrideControls() throws Exception {
        String profilePage = read("app/src/main/java/dev/ricky/galaxywatchtweaker/ui/ProfilePage.kt");
        String strings = read("app/src/main/res/values/strings.xml");

        assertTrue(profilePage.contains("R.string.section_watch_display_name"));
        assertTrue(profilePage.contains("KEY_WATCH_ALIAS_NAME"));
        assertTrue(strings.contains("Watch display name"));
    }

    private static String read(String path) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).getParent();
        return new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
    }
}
