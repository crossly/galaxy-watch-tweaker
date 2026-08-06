package dev.ricky.galaxywatchtweaker.core;

public final class WatchAliasPolicy {
    private WatchAliasPolicy() {
    }

    public static String aliasFor(String configuredAlias, String originalAlias) {
        if (configuredAlias == null || configuredAlias.trim().isEmpty()) {
            return originalAlias;
        }
        return configuredAlias.trim();
    }
}
