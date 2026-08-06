package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WatchAliasPolicyTest {
    @Test
    public void returnsConfiguredAliasWhenOverrideIsSet() {
        assertEquals(
                "Ricky Watch",
                WatchAliasPolicy.aliasFor("Ricky Watch", "Galaxy Watch Ultra"));
    }

    @Test
    public void preservesOriginalAliasWhenConfiguredAliasIsBlank() {
        assertEquals(
                "Galaxy Watch Ultra",
                WatchAliasPolicy.aliasFor("  ", "Galaxy Watch Ultra"));
    }

    @Test
    public void preservesOriginalAliasWhenConfiguredAliasIsNull() {
        assertEquals(
                "Galaxy Watch Ultra",
                WatchAliasPolicy.aliasFor(null, "Galaxy Watch Ultra"));
    }

    @Test
    public void trimsConfiguredAliasBeforeReturningIt() {
        assertEquals(
                "Ricky Watch",
                WatchAliasPolicy.aliasFor("  Ricky Watch  ", "Galaxy Watch Ultra"));
    }
}
