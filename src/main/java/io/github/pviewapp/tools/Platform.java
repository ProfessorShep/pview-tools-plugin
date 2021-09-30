package io.github.pviewapp.tools;

import java.util.Locale;

public enum Platform {
    WINDOWS, MAC, UNIX, SOLARIS, UNKNOWN;

    private static final Platform currentPlatform;
    private static final String os;

    static {
        os = System.getProperty("os.name", "generic");

        final var osLower = os.toLowerCase(Locale.ENGLISH);

        if (osLower.contains("mac") || osLower.contains("darwin")) {
            currentPlatform = MAC;
        } else if (osLower.contains("win")) {
            currentPlatform = WINDOWS;
        } else if (osLower.contains("nux") || osLower.contains("nix") || osLower.contains("aix")) {
            currentPlatform = UNIX;
        } else if (osLower.contains("sunos") || osLower.contains("solaris")) {
            currentPlatform = SOLARIS;
        } else {
            currentPlatform = UNKNOWN;
        }
    }

    public static Platform getCurrentPlatform() {
        return currentPlatform;
    }

    public static String getPlatformName() {
        return os;
    }
}
