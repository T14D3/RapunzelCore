package de.t14d3.rapunzelcore.util;

public final class Closeables {
    private Closeables() {
    }

    public static AutoCloseable chain(AutoCloseable first, AutoCloseable second) {
        if (first == null) return second;
        if (second == null) return first;
        return () -> {
            try {
                second.close();
            } catch (Exception ignored) {
            }
            try {
                first.close();
            } catch (Exception ignored) {
            }
        };
    }
}
