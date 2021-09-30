package io.github.pviewapp.tools;

import io.github.pviewapp.tools.jdktools.JLink;
import io.github.pviewapp.tools.jdktools.JPackage;

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
    private final List<String> jvmArgs;

    private final String appName;
    private final String mainClass;

    private final String modulePath;
    private final Set<String> modules;

    private final List<String> jlinkArgs;
    private final List<String> jpackageArgs;

    Packager(Path baseDir,
             String appVersion,
             List<String> jvmArgs,
             String appName,
             String mainClass,
             String modulePath,
             Set<String> modules,
             List<String> jlinkArgs,
             List<String> jpackageArgs) {
        this.baseDir = baseDir;
        this.appVersion = appVersion;
        this.jvmArgs = jvmArgs;
        this.appName = appName;
        this.mainClass = mainClass;
        this.modulePath = modulePath;
        this.modules = modules;

        this.jlinkArgs = jlinkArgs;
        this.jpackageArgs = jpackageArgs;
    }


    public Path generateRuntime(Path output) throws IOException, InterruptedException {
        if (output.getFileName().toString().equalsIgnoreCase("app")) {
            throw new IllegalArgumentException("Name 'app' is reserved");
        }

        output = output.isAbsolute() ? output : baseDir.resolve(output);

        final var filesReverse = new ArrayDeque<Path>();

        if (Files.exists(output))
            Files.walk(output).forEach(filesReverse::add);


        final Iterable<Path> files = filesReverse::descendingIterator;

        for (var file : files) {
            Files.delete(file);
        }

        final ArrayList<String> args = new ArrayList<>(jlinkArgs);

        args.add("--output");
        args.add(output.toString());

        args.add("-p");
        args.add(modulePath);

        if (!modules.isEmpty()) {
            args.add("--add-modules");
            args.add(String.join(", ", modules));
        }

        JLink.run(args.toArray(new String[0]));

        return output;
    }

    public Path generateNativePackage(Path outputDir, Path runtimePath) throws IOException, InterruptedException {
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

        final List<String> args = new ArrayList<>(jpackageArgs);

        if (!installerType.isEmpty()) {
            args.add("-t");
            args.add(installerType);
        }

        args.addAll(List.of(
                "-n", appName,
                "-m", mainClass,
                "--runtime-image", runtimePath.toAbsolutePath().toString(),
                "--app-version", appVersion,
                "-d", outputDir.toAbsolutePath().toString()
        ));


        for (var jvmArg : jvmArgs) {
            args.add("--java-options");
            args.add(jvmArg.replace("$PV_RESOURCE_DIR", "$APPDIR/../" + runtimePath.getFileName().toString()));
        }

        JPackage.run(args.toArray(new String[0]));

        final BiPredicate<Path, BasicFileAttributes> jpackageOutputFinder = (p, bfa) -> p.getFileName().toString().contains(appName)
                && p.getFileName().toString().contains(appVersion);

        return Files.find(
                        outputDir,
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
