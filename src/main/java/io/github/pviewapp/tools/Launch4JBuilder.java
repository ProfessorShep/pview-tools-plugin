package io.github.pviewapp.tools;

import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Launch4JBuilder {
    private Path outFile;
    private Path iconDirectory;
    private String mainClass;
    private List<String> jvmArgs = new ArrayList<>();

    public Launch4JBuilder outputFile(Path outFile) {
        this.outFile = outFile.toAbsolutePath();
        return this;
    }

    public Launch4JBuilder iconDirectory(Path iconDirectory) {
        this.iconDirectory = iconDirectory.toAbsolutePath();
        return this;
    }

    public Launch4JBuilder mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public Launch4JBuilder jvmArg(String arg) {
        this.jvmArgs.add(arg);
        return this;
    }

    public Launch4JBuilder jvmArgs(Collection<String> args) {
        this.jvmArgs.addAll(args);
        return this;
    }

    public Path build(Path workDir) throws IOException {
        @SuppressWarnings("ConstantConditions") final var template = IOUtils.toString(
                getClass().getResourceAsStream("/io/github/pviewapp/tools/l4j-template.xml"),
                StandardCharsets.UTF_8);

        final var config = String.format(
                template,
                Icons.get(iconDirectory),
                outFile,
                mainClass,
                processVmArgs()
        );

        final var configPath = workDir.resolve("l4jConfig.xml");

        Files.createDirectories(workDir);

        try (final var out = Files.newOutputStream(configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            IOUtils.write(config, out, StandardCharsets.UTF_8);
        }

        final var launch4jExe = workDir.resolve("launch4j.exe").toAbsolutePath();
        if (!Files.exists(launch4jExe)) {
            ZipUtil.unpack(getClass().getResourceAsStream("/io/github/pviewapp/tools/launch4j.zip"), workDir.toFile(), StandardCharsets.UTF_8);
        }

        try {
            Threads.pause();
            new ProcessBuilder(launch4jExe.toString(), "l4jConfig.xml")
                    .inheritIO()
                    .directory(workDir.toFile())
                    .start()
                    .waitFor();
            Threads.pause();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        return outFile;
    }

    private String processVmArgs() {
        final StringBuilder s = new StringBuilder(("opt".length() + 3) * jvmArgs.size() + (jvmArgs.size() * 5));

        for (var value : jvmArgs) {
            s.append("<").append("opt").append(">").append(value).append("</opt>\n");
        }

        return s.toString();
    }
}
