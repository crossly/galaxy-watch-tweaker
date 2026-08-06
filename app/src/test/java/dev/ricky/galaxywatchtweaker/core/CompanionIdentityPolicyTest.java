package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;

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
    public void passesBareOneThroughUnchanged() {
        assertEquals("1", CompanionIdentityPolicy.toWatchCompatibleModel("1"));
    }
}
