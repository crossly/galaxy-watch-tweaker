package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TargetPackagesTest {
    private static final String PLUGIN = "com.samsung.wearable.watch7plugin";
    private static final String MANAGER = "com.samsung.android.app.watchmanager";

    @Test
    public void recognizesOnlySamsungWearableTargets() {
        assertTrue(TargetPackages.isTargetPackage(PLUGIN));
        assertTrue(TargetPackages.isTargetPackage(MANAGER));
        assertFalse(TargetPackages.isTargetPackage("com.example.other"));
    }
}
