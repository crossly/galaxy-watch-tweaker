package dev.ricky.galaxywatchtweaker;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public final class GalaxyWatchTweakerMergeSourceTest {
    @Test
    public void appBrandingUsesGalaxyWatchTweaker() throws Exception {
        String strings = read("app/src/main/res/values/strings.xml");
        String manifest = read("app/src/main/AndroidManifest.xml");

        assertTrue(strings.contains("Galaxy Watch Tweaker"));
        assertTrue(strings.contains("Samsung Health Monitor"));
        assertTrue(manifest.contains("@string/app_name"));
    }

    @Test
    public void scopeIncludesSamsungHealthMonitorForMergedShmPatch() throws Exception {
        String scope = read("app/src/main/resources/META-INF/xposed/scope.list");
        String manifest = read("app/src/main/AndroidManifest.xml");
        String strings = read("app/src/main/res/values/strings.xml");

        assertTrue(scope.contains("com.samsung.android.shealthmonitor"));
        assertTrue(manifest.contains("com.samsung.android.shealthmonitor"));
        assertTrue(strings.contains("com.samsung.android.shealthmonitor"));
    }

    @Test
    public void preferencesDeclareIndependentFeatureTogglesAndSpoofProfile() throws Exception {
        String preferences = read("app/src/main/java/dev/ricky/galaxywatchtweaker/settings/TweakerPreferences.java");

        assertTrue(preferences.contains("KEY_SHM_PATCH_ENABLED"));
        assertTrue(preferences.contains("KEY_COMPANION_IDENTITY_ENABLED"));
        assertTrue(preferences.contains("KEY_CAPABILITY_EXCHANGE_ENABLED"));
        assertTrue(preferences.contains("KEY_CONNECTION_RECOVERY_ENABLED"));
        assertTrue(preferences.contains("KEY_SPOOF_MODEL"));
        assertTrue(preferences.contains("KEY_SPOOF_SALES_CODE"));
        assertTrue(preferences.contains("DEFAULT_SPOOF_MODEL = \"SM-S938U1\""));
        assertTrue(preferences.contains("DEFAULT_SPOOF_SALES_CODE = \"XAA\""));
    }

    @Test
    public void hookEntryDispatchesEachRiskDomainThroughConfig() throws Exception {
        String entry = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");

        assertTrue(entry.contains("HookConfig config"));
        assertTrue(entry.contains("config.isConnectionRecoveryEnabled()"));
        assertTrue(entry.contains("config.isCompanionIdentityEnabled()"));
        assertTrue(entry.contains("config.isCapabilityExchangeEnabled()"));
        assertTrue(entry.contains("SamsungHealthMonitorHook.handleLoadPackage(lpparam, config)"));
        assertTrue(entry.contains("SHM_PACKAGE.equals(lpparam.packageName)"));
    }

    @Test
    public void uiExposesFeatureSwitchesSpoofProfileAndVersionMatrix() throws Exception {
        String hooksPage = read("app/src/main/java/dev/ricky/galaxywatchtweaker/ui/HooksPage.kt");
        String profilePage = read("app/src/main/java/dev/ricky/galaxywatchtweaker/ui/ProfilePage.kt");
        String versionsPage = read("app/src/main/java/dev/ricky/galaxywatchtweaker/ui/VersionsPage.kt");
        String components = read("app/src/main/java/dev/ricky/galaxywatchtweaker/ui/SettingsComponents.kt");
        String strings = read("app/src/main/res/values/strings.xml");

        assertTrue(hooksPage.contains("R.string.section_samsung_health_monitor"));
        assertTrue(hooksPage.contains("R.string.section_connection_recovery"));
        assertTrue(hooksPage.contains("R.string.companion_identity_title"));
        assertTrue(profilePage.contains("R.string.section_spoof_profile"));
        assertTrue(strings.contains("Phone SHM"));
        assertTrue(strings.contains("Watch SHM"));
        assertTrue(versionsPage.contains("R.string.version_phone_shm"));
        assertTrue(versionsPage.contains("R.string.version_watch_shm"));
        assertTrue(versionsPage.contains("R.string.version_watch_side_only"));
        assertTrue(components.contains("OutlinedTextField"));
        assertTrue(strings.contains("Restart target apps after changing hook switches"));
        assertTrue(strings.contains("Watch-side only"));
    }

    private static String read(String path) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).getParent();
        return new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
    }
}
