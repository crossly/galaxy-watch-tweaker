package dev.ricky.galaxywatchtweaker.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/** The companion identity survives profile edits and module/target process restarts. */
public final class CompanionProfilePolicy {
    public static final String PREFIX = "companion_";

    private CompanionProfilePolicy() {
    }

    public static Map<String, String> pinnedValues(Map<String, ?> preferences) {
        Map<String, String> pinned = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : SpoofProfile.defaults().preferenceValues().entrySet()) {
            String key = entry.getKey();
            Object existing = preferences.get(PREFIX + key);
            Object configured = preferences.get(key);
            String fallback = configured instanceof String
                    ? TweakerPreferences.nonBlankOrDefault((String) configured, entry.getValue())
                    : entry.getValue();
            pinned.put(PREFIX + key, existing instanceof String
                    ? TweakerPreferences.nonBlankOrDefault((String) existing, fallback) : fallback);
        }
        return pinned;
    }
}
