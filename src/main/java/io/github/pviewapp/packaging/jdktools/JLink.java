package io.github.pviewapp.packaging.jdktools;

import java.io.IOException;
import java.nio.file.Path;

public class JLink {
    private JLink() {}

    private static final Path jlinkExe = JavaTools.resolve("jlink");

    public static Process runAsync(String... args) throws IOException {
        return new ProcessBuilder(JavaTools.gatherProgramArgs(jlinkExe, args))
                .inheritIO().start();
    }

    public static void run(String... args) throws IOException, InterruptedException {
        runAsync(args).waitFor();
    }
}
