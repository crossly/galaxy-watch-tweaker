package dev.ricky.galaxywatchtweaker;

final class HookUtils {
    private HookUtils() {
    }

    static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
