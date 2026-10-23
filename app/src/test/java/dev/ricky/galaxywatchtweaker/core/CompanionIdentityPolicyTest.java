package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;
import java.util.Locale;
import java.util.Map;

import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompanionIdentityPolicyTest {
    private static final SpoofProfile PROFILE = SpoofProfile.defaults();

    @Test
    public void returnsSamsungIdentityForCompanionKeys() {
        assertEquals("SAMSUNG", CompanionIdentityPolicy.spoofedValueFor("key_manufacturer", "HONOR", PROFILE));
        assertEquals("SM-S938U", CompanionIdentityPolicy.spoofedValueFor("key_model_number", "BKQ-AN90", PROFILE));
        assertEquals("XAA", CompanionIdentityPolicy.spoofedValueFor("key_sales_code", "CHC", PROFILE));
    }

    @Test
    public void returnsSamsungIdentityForFeatureExchangeKeys() {
        assertEquals("SAMSUNG", CompanionIdentityPolicy.spoofedValueFor("vender", "honor", PROFILE));
        assertEquals("SM-S938U", CompanionIdentityPolicy.spoofedValueFor("modelNumber", "BKQ-AN90", PROFILE));
        assertEquals("XAA", CompanionIdentityPolicy.spoofedValueFor("csc", "CHC", PROFILE));
    }

    @Test
    public void leavesUnrelatedCapabilityValuesUntouched() {
        assertEquals("original", CompanionIdentityPolicy.spoofedValueFor("key_watch_sdk_version", "original", PROFILE));
        assertFalse(CompanionIdentityPolicy.shouldSpoof("key_watch_sdk_version"));
    }

    @Test
    public void recognizesOnlyCompanionIdentityKeys() {
        assertTrue(CompanionIdentityPolicy.shouldSpoof("key_manufacturer"));
        assertTrue(CompanionIdentityPolicy.shouldSpoof("key_model_number"));
        assertTrue(CompanionIdentityPolicy.shouldSpoof("key_sales_code"));
        assertTrue(CompanionIdentityPolicy.shouldSpoof("vender"));
        assertTrue(CompanionIdentityPolicy.shouldSpoof("modelNumber"));
        assertTrue(CompanionIdentityPolicy.shouldSpoof("csc"));
    }

    @Test
    public void mapsGoogleWearPhoneIdentityPropertiesToSamsungUsModel() {
        assertEquals(
                "samsung",
                CompanionIdentityPolicy.systemPropertyValueFor("ro.product.manufacturer", "HONOR", PROFILE));
        assertEquals(
                "samsung",
                CompanionIdentityPolicy.systemPropertyValueFor("ro.product.brand", "HONOR", PROFILE));
        assertEquals(
                "SM-S938U",
                CompanionIdentityPolicy.systemPropertyValueFor("ro.product.model", "BKQ-AN90", PROFILE));
        assertEquals(
                "XAA",
                CompanionIdentityPolicy.systemPropertyValueFor("ro.csc.sales_code", "CHC", PROFILE));
    }

    @Test
    public void leavesUnrelatedSystemPropertiesUntouched() {
        assertEquals(
                "original",
                CompanionIdentityPolicy.systemPropertyValueFor("ro.boot.verifiedbootstate", "original", PROFILE));
    }

    @Test
    public void mapsExactPresetModelToWatchCompatibleModel() {
        assertEquals("SM-S938U", CompanionIdentityPolicy.toWatchCompatibleModel("SM-S938U1"));
    }

    @Test
    public void passesOtherModelsEndingInOneThroughUnchanged() {
        assertEquals("SM-G9911", CompanionIdentityPolicy.toWatchCompatibleModel("SM-G9911"));
        assertEquals("BKQ-AN91", CompanionIdentityPolicy.toWatchCompatibleModel("BKQ-AN91"));
    }

    @Test
    public void outboundRewritePreservesInputAndUnrelatedFields() {
        Map<String, Object> original = Map.of("vender", "HONOR", "modelNumber", "PHONE",
                "csc", "CHC", "pairing_token", "keep", "watch_id", "keep-too");
        Map<Object, Object> updated = CompanionIdentityPolicy.outboundFeatureData(original, PROFILE);
        assertEquals("SAMSUNG", updated.get("vender"));
        assertEquals("SM-S938U", updated.get("modelNumber"));
        assertEquals("XAA", updated.get("csc"));
        assertEquals("keep", updated.get("pairing_token"));
        assertEquals("keep-too", updated.get("watch_id"));
        assertEquals("HONOR", original.get("vender"));
        assertEquals(original.size(), updated.size());
    }

    @Test
    public void nonIdentityMessagesDoNotAcquirePhoneIdentityFields() {
        Map<String, Object> data = Map.of("watch_id", "abc");
        assertEquals(data, CompanionIdentityPolicy.outboundFeatureData(data, PROFILE));
        assertTrue(CompanionIdentityPolicy.outboundFeatureData(Map.of(), PROFILE).isEmpty());
    }

    @Test
    public void manufacturerDoesNotChangeWithPhoneLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Map<String, Object> prefs = Map.of("spoof_manufacturer", "indigo");
            assertEquals("INDIGO", CompanionIdentityPolicy.spoofedValueFor("vender", "original",
                    SpoofProfile.fromPreferences(prefs)));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void passesBareOneThroughUnchanged() {
        assertEquals("1", CompanionIdentityPolicy.toWatchCompatibleModel("1"));
    }
}
