package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompanionIdentityModelTest {
    static class Supported {
        String m = "originalModel", n = "originalManufacturer", q = "originalVendor";
        void e(Object value) {}
        void e() {}
    }
    static class Ambiguous {
        void e(Object value) {}
        void e(String value) {}
    }
    static class WrongLayout { String m, n; int q; }
    static class FinalLayout { String m, n; final String q = "vendor"; }
    @Test public void findsOnlyUniqueMatchingMethod() throws Exception {
        assertEquals(Object.class, CompanionIdentityModel.findSingleArgumentMethod(Supported.class, "e")
                .getParameterTypes()[0]);
    }
    @Test public void ambiguousOverloadsAreRejected() {
        assertThrows(NoSuchMethodException.class,
                () -> CompanionIdentityModel.findSingleArgumentMethod(Ambiguous.class, "e"));
    }
    @Test public void unsupportedFieldsAreRejectedBeforeAnyWrite() {
        assertThrows(NoSuchFieldException.class, () -> new CompanionIdentityModel(WrongLayout.class));
        assertThrows(NoSuchFieldException.class, () -> new CompanionIdentityModel(FinalLayout.class));
    }
    @Test public void updatesValidatedIdentityTogether() throws Exception {
        Supported target = new Supported();
        new CompanionIdentityModel(Supported.class).apply(target, "SM-S938U", "SAMSUNG");
        assertEquals("SM-S938U", target.m);
        assertEquals("SAMSUNG", target.n);
        assertEquals("SAMSUNG", target.q);
    }
}
