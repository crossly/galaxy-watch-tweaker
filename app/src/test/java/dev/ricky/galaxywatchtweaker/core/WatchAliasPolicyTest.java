package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WatchAliasPolicyTest {
    @Test
    public void returnsConfiguredAliasWhenOverrideIsEnabled() {
        assertEquals(
                "Ricky Watch",
                WatchAliasPolicy.aliasFor(true, "Ricky Watch", "Galaxy Watch Ultra"));
    }

    @Test
    public void preservesOriginalAliasWhenOverrideIsDisabled() {
        assertEquals(
                "Galaxy Watch Ultra",
                WatchAliasPolicy.aliasFor(false, "Ricky Watch", "Galaxy Watch Ultra"));
    }

    @Test
    public void preservesOriginalAliasWhenConfiguredAliasIsBlank() {
        assertEquals(
                "Galaxy Watch Ultra",
                WatchAliasPolicy.aliasFor(true, "  ", "Galaxy Watch Ultra"));
    }

    @Test
    public void trimsConfiguredAliasBeforeReturningIt() {
        assertEquals(
                "Ricky Watch",
                WatchAliasPolicy.aliasFor(true, "  Ricky Watch  ", "Galaxy Watch Ultra"));
    }
}
