package io.github.pview.tools;

public enum Platform {
    WINDOWS, MAC, UNIX, SOLARIS, UNKNOWN;

    private static final Platform currentPlatform;
    private static final String os;

    static {
        os = System.getProperty("os.name", "generic");

        if (os.contains("mac") || os.contains("Mac") || os.contains("darwin")) {
            currentPlatform = Platform.MAC;
        } else if (os.contains("win") || os.contains("Win")) {
            currentPlatform = Platform.WINDOWS;
        } else if (os.contains("nux")) {
            currentPlatform = Platform.UNIX;
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
