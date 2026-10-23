package dev.ricky.galaxywatchtweaker.settings;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class CompanionProfilePolicyTest {
    @Test public void upgradePinsCurrentCustomProfileRatherThanDefaults() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put(TweakerPreferences.KEY_SPOOF_MODEL, "SM-S928B");
        prefs.put(TweakerPreferences.KEY_SPOOF_SALES_CODE, "EUX");
        Map<String, String> pinned = CompanionProfilePolicy.pinnedValues(prefs);
        assertEquals("SM-S928B", pinned.get("companion_spoof_model"));
        assertEquals("EUX", pinned.get("companion_spoof_sales_code"));
    }
    @Test public void editsAndRestoringDefaultsDoNotChangeCompanionIdentityAcrossRestarts() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put(TweakerPreferences.KEY_SPOOF_MODEL, "SM-S928B");
        prefs.putAll(CompanionProfilePolicy.pinnedValues(prefs));
        prefs.put(TweakerPreferences.KEY_SPOOF_MODEL, "OTHER");
        assertEquals("SM-S928B", CompanionProfilePolicy.pinnedValues(prefs).get("companion_spoof_model"));
        prefs.putAll(SpoofProfile.defaults().preferenceValues());
        assertEquals("SM-S928B", CompanionProfilePolicy.pinnedValues(new HashMap<>(prefs))
                .get("companion_spoof_model"));
    }
    @Test public void allProfileFieldsArePinnedAndRoundTripWithoutDroppingCustomValues() {
        Map<String, Object> prefs = new HashMap<>();
        SpoofProfile.defaults().preferenceValues().forEach((key, value) -> prefs.put(key, "custom_" + value));
        Map<String, String> pinned = CompanionProfilePolicy.pinnedValues(prefs);
        assertEquals(12, pinned.size());
        Map<String, String> restored = new HashMap<>();
        pinned.forEach((key, value) -> restored.put(key.substring(CompanionProfilePolicy.PREFIX.length()), value));
        assertEquals(prefs, SpoofProfile.fromPreferences(restored).preferenceValues());
    }
}
