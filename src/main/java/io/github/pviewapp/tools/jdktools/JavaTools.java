package io.github.pviewapp.tools.jdktools;

import java.nio.file.Path;

public class JavaTools {
    private JavaTools() {}

    public static Path resolve(String toolName) {
        return Path.of(System.getProperty("java.home"), "bin", toolName).toAbsolutePath();
    }

    public static String[] gatherProgramArgs(Path toolExeLocation, String... args) {
        final String[] command = new String[args.length + 1];

        command[0] = toolExeLocation.toString();

        System.arraycopy(args, 0, command, 1, args.length);

        return command;
    }
}
