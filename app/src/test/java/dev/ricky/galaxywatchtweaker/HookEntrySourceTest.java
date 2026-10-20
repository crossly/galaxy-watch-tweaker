package dev.ricky.galaxywatchtweaker;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class HookEntrySourceTest {
    @Test
    public void xposedEntryClassUsesDeclaredPackage() throws Exception {
        String source = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");

        assertTrue(source.contains("package dev.ricky.galaxywatchtweaker;"));
    }

    @Test
    public void frameworkScopeIsDeclaredForCompanionDeviceRecovery() throws Exception {
        String strings = read("app/src/main/res/values/strings.xml");

        assertTrue(strings.contains("<item>android</item>"));
    }

    @Test
    public void frameworkHookTargetsCompanionDevicePresenceProcessor() throws Exception {
        String source = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/CompanionPresenceRecoveryHook.java");

        assertTrue(source.contains("com.android.server.companion.CompanionDeviceManagerService"));
        assertTrue(source.contains("onBluetoothCompanionDeviceConnected"));
    }

    @Test
    public void frameworkRecoveryReactivatesWhenWatchBleReappears() throws Exception {
        String source = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/CompanionPresenceRecoveryHook.java");

        assertTrue(source.contains(
                "com.android.server.companion.devicepresence.DevicePresenceProcessor"));
        assertTrue(source.contains("onBleCompanionDeviceFound"));
        assertTrue(source.contains("requestBluetoothConnection"));
        assertTrue(source.contains("\"connect\""));
        assertTrue(source.contains("watch reappeared"));
    }

    @Test
    public void frameworkRecoveryOnlyInstallsInSystemServerProcess() throws Exception {
        String hookEntry = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");
        String modernEntry = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/ModernHookEntry.java");

        assertTrue(hookEntry.contains("SYSTEM_SERVER_PROCESS"));
        assertTrue(hookEntry.contains("SYSTEM_SERVER_PROCESS.equals(lpparam.processName)"));
        assertTrue(modernEntry.contains("legacyParam.processName = \"system\""));
    }

    @Test
    public void frameworkRecoveryMarksAssociationObservedBeforeConnectedEvent() throws Exception {
        String source = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/CompanionPresenceRecoveryHook.java");

        assertTrue(source.contains("android.companion.AssociationInfo$Builder"));
        assertTrue(source.contains("setNotifyOnDeviceNearby"));
        assertTrue(source.contains("updateAssociation"));
    }

    @Test
    public void watch7PluginHookTargetsOnlyCompanionIdentityPreferences() throws Exception {
        String source = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");

        assertTrue(source.contains("com.samsung.android.basicdata.capability.CapabilityDataSetter"));
        assertTrue(source.contains("setPreference"));
        assertTrue(source.contains("CompanionIdentityPolicy.shouldSpoof"));
    }

    @Test
    public void scopeIncludesSamsungHealthMonitorForMergedHealthPatch() throws Exception {
        String strings = read("app/src/main/res/values/strings.xml");
        String scopeList = read("app/src/main/resources/META-INF/xposed/scope.list");
        String manifest = read("app/src/main/AndroidManifest.xml");

        assertTrue(strings.contains("com.google.android.wearable.app.cn"));
        assertTrue(scopeList.contains("com.google.android.wearable.app.cn"));
        assertTrue(manifest.contains("com.google.android.wearable.app.cn"));
        assertTrue(strings.contains("com.samsung.android.shealthmonitor"));
        assertTrue(scopeList.contains("com.samsung.android.shealthmonitor"));
        assertTrue(manifest.contains("com.samsung.android.shealthmonitor"));
    }

    @Test
    public void googleWearHookSpoofsPhoneIdentityWithoutWatchManagerBuildSpoof() throws Exception {
        String source = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");

        assertTrue(source.contains("com.google.android.wearable.app.cn"));
        assertTrue(source.contains("setStaticObjectField(android.os.Build.class"));
        assertTrue(source.contains("android.os.SystemProperties"));
        assertTrue(source.contains("Samsung process hook is passive for"));
    }

    @Test
    public void watch7PluginHookSpoofsWearCompanionIdentityModelOnly() throws Exception {
        String entrySource = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");
        String hookSource = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/Watch7CompanionIdentityHook.java");

        assertTrue(entrySource.contains("Watch7CompanionIdentityHook.install"));
        assertTrue(hookSource.contains("com.google.android.gms.internal.wear_companion.wt"));
        assertTrue(hookSource.contains("\"e\""));
        assertTrue(hookSource.contains("findSingleArgumentMethod"));
        assertTrue(hookSource.contains("XposedBridge.hookMethod"));
        assertTrue(hookSource.contains("\"q\""));
        assertTrue(hookSource.contains("profile.manufacturer().toUpperCase()"));
    }

    @Test
    public void watch7PluginHookSpoofsFeatureExchangeDataForWcsProvider() throws Exception {
        String entrySource = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");
        String hookSource = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/Watch7CapabilityExchangeHook.java");
        String policySource = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/core/CompanionIdentityPolicy.java");

        assertTrue(entrySource.contains("Watch7CapabilityExchangeHook.install"));
        assertTrue(hookSource.contains(
                "com.samsung.android.companionservice.capability.CapabilityExchangeMessage"));
        assertTrue(hookSource.contains(
                "com.samsung.android.companionservice.capability.CapabilityExchangeSender"));
        assertTrue(hookSource.contains("\"data\""));
        assertTrue(hookSource.contains("CompanionIdentityPolicy.FEATURE_VENDOR"));
        assertTrue(hookSource.contains("CompanionIdentityPolicy.FEATURE_MODEL_NUMBER"));
        assertTrue(hookSource.contains("CompanionIdentityPolicy.FEATURE_CSC"));
        assertTrue(policySource.contains("FEATURE_VENDOR = \"vender\""));
        assertTrue(policySource.contains("FEATURE_MODEL_NUMBER = \"modelNumber\""));
        assertTrue(policySource.contains("FEATURE_CSC = \"csc\""));
        assertTrue(hookSource.contains("setCapabilityMessage"));
        assertTrue(hookSource.contains("profile.salesCode()"));
    }

    @Test
    public void watch7PluginRefreshesCapabilityExchangeAfterConnectedState() throws Exception {
        String entrySource = read("app/src/main/java/dev/ricky/galaxywatchtweaker/HookEntry.java");
        String hookSource = read(
                "app/src/main/java/dev/ricky/galaxywatchtweaker/Watch7CapabilityRefreshHook.java");

        assertTrue(entrySource.contains("Watch7CapabilityRefreshHook.install"));
        assertTrue(hookSource.contains(
                "com.samsung.android.companionservice.service.CapabilityCompanion"));
        assertTrue(hookSource.contains("onConnectionStateChanged"));
        assertTrue(hookSource.contains("mLastestConnectedBtAddr"));
        assertTrue(hookSource.contains("requestCapabilityExchange"));
        assertTrue(hookSource.contains("GalaxyWatchTweaker-WCS"));
    }

    private static String read(String path) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).getParent();
        return new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
    }
}
