package io.github.pview.tools;

import io.github.pview.tools.jdktools.JLink;
import io.github.pview.tools.jdktools.JPackage;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;

import static java.lang.System.getProperty;

public class Packager {
    private final Path baseDir;

    private final String appVersion;
    private final Set<String> jvmArgs;

    private final String appName;
    private final String mainClass;

    private final String modulePath;
    private final Set<String> modules;

    Packager(Path baseDir, String appVersion, Set<String> jvmArgs, String appName, String mainClass, String modulePath, Set<String> modules) {
        this.baseDir = baseDir;
        this.appVersion = appVersion;
        this.jvmArgs = jvmArgs;
        this.appName = appName;
        this.mainClass = mainClass;
        this.modulePath = modulePath;
        this.modules = modules;
    }


    public Path generateRuntime(Path output) throws IOException, InterruptedException {

        output = output.isAbsolute() ? output : baseDir.resolve(output);

        final var filesReverse = new ArrayDeque<Path>();

        if (Files.exists(output))
            Files.walk(output).forEach(filesReverse::add);


        final Iterable<Path> files = filesReverse::descendingIterator;

        for (var file : files) {
            Files.delete(file);
        }

        final ArrayList<String> args = new ArrayList<>(List.of(
                "--compress=" + getProperty("pview.tools.jlink.compress", "2"),
                "--strip-debug",
                "--no-header-files",
                "--no-man-pages",
                "--strip-java-debug-attributes",
                "--output", output.toString(),
                "-p", modulePath
        ));

        if (!modules.isEmpty()) {
            args.add("--add-modules");
            args.add(String.join(", ", modules));
        }

        JLink.run(args.toArray(new String[0]));

        return output;
    }

    public Path generateNativePackage(Path runtimePath) throws IOException, InterruptedException {
        final String installerType;

        if (getProperty("pview.tools.jpackage.outputType") != null) {
            installerType = getProperty("pview.tools.jpackage.outputType");
        } else {
            switch (Platform.getCurrentPlatform()) {
                case WINDOWS:
                    installerType = "msi";
                    break;
                case MAC:
                    installerType = "pkg";
                    break;
                default:
                    installerType = "";
            }
        }

        final List<String> args = new ArrayList<>();

        if (!installerType.isEmpty()) {
            args.add("-t");
            args.add(installerType);
        }

        args.addAll(List.of(
                "-n", appName,
                "-m", mainClass,
                "--runtime-image", runtimePath.toAbsolutePath().toString(),
                "--app-version", appVersion
        ));

        put(args, "--file-associations", getProperty(getPropertyKey("jpackage.fileAssociations")));

        put(args, "--vendor", getProperty(getPropertyKey("jpackage.vendor")));

        put(args, "--copyright", getProperty(getPropertyKey("jpackage.copyright")));

        put(args, "--description", getProperty(getPropertyKey("jpackage.description")));

        put(args, "--icon", System.getProperty(getPropertyKey("jpackage.icon")));

        put(args, "--license-file", System.getProperty(getPropertyKey("jpackage.license")));

        put(args, "-i", System.getProperty(getPropertyKey("jpackage.include")));

        put(args, "-d", System.getProperty(getPropertyKey("jpackage.output")));


        for (var jvmArg : jvmArgs) {
            args.add("--java-options");
            args.add(jvmArg);
        }

        //noinspection SwitchStatementWithTooFewBranches  might add other platforms in future
        switch (Platform.getCurrentPlatform()) {
            case WINDOWS:
                args.addAll(List.of(
                        "--win-menu",
                        "--win-menu-group", appName
                ));
                putIfTrue(args, "--win-per-user-install", Boolean.parseBoolean(getProperty(getPropertyKey("jpackage.userInstall"), "true")));
                break;
        }

        JPackage.run(args.toArray(new String[0]));

        final BiPredicate<Path, BasicFileAttributes> jpackageOutputFinder = (p, bfa) -> p.getFileName().toString().contains(appName) && p.getFileName().toString().contains(appVersion);
        final var jpackageOutputFolder = getProperty(getPropertyKey("jpackage.output"));

        return Files.find(
                        jpackageOutputFolder != null ? Path.of(jpackageOutputFolder) : Path.of(System.getProperty("user.dir")),
                        1,
                        jpackageOutputFinder
                ).findAny()
                .orElse(null);
    }

    private static <E> void put(Collection<E> c, E key, E value) {
        if (value != null) {
            c.add(key);
            c.add(value);
        }
    }

    private static <E> void putIfTrue(Collection<E> c, E value, boolean flag) {
        if (flag) {
            c.add(value);
        }
    }

    private static String getPropertyKey(String key) {
        return "pview.tools." + key;
    }
}
