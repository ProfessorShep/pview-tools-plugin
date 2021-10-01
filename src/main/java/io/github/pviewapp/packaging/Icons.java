package io.github.pviewapp.packaging;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Icons {
    private Icons() {}

    private static final Map<Platform, String> fileExtensions = Map.of(
            Platform.WINDOWS, ".ico",
            Platform.MAC, ".icns",
            Platform.UNIX, ".png"
    );

    public static Path get(Path dir) throws IOException {
        if (Platform.getCurrentPlatform() == Platform.UNKNOWN)
            throw new UnsupportedOperationException("Unknown platform: " + Platform.getPlatformName());

        return
                Files.find(dir, 1, (p, a) -> p.getFileName().toString().endsWith(fileExtensions.get(Platform.getCurrentPlatform()))).findAny()
                        .orElseThrow(() -> new FileNotFoundException("No icon file found in directory " + dir)).toAbsolutePath();
    }
}
