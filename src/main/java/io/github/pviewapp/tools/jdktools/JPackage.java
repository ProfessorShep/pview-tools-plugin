package io.github.pviewapp.tools.jdktools;

import java.io.IOException;
import java.nio.file.Path;

public class JPackage {
    private JPackage() {}

    private static final Path jpackageExe = JavaTools.resolve("jpackage");

    public static Process runAsync(String... args) throws IOException {
        return new ProcessBuilder(JavaTools.gatherProgramArgs(jpackageExe, args))
                .inheritIO()
                .start();
    }

    public static void run(String... args) throws IOException, InterruptedException {
        runAsync(args).waitFor();
    }
}
