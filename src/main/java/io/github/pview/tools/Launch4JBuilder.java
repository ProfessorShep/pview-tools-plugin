package io.github.pview.tools;

import org.apache.commons.io.IOUtils;

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

    public Path build(Path tmpDir) throws IOException {
        @SuppressWarnings("ConstantConditions") final var template = IOUtils.toString(
                getClass().getResourceAsStream("/io/github/pview/tools/l4j=template.xml"),
                StandardCharsets.UTF_8);

        final var config = String.format(
                template,
                Icons.get(iconDirectory),
                outFile,
                mainClass,
                multiValue("opt", jvmArgs)
        );

        final var configPath = tmpDir.resolve("l4jConfig.xml");

        Files.createDirectories(tmpDir);

        try (final var out = Files.newOutputStream(configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            IOUtils.write(config, out, StandardCharsets.UTF_8);
        }

        net.sf.launch4j.Main.main(new String[]{configPath.toString()});

        return outFile;
    }

    private static String multiValue(String name, Collection<String> values) {
        final StringBuilder s = new StringBuilder((name.length() + 3) * values.size() + (values.size() * 5));

        for (var value : values) {
            s.append("<").append(name).append(">").append(value).append("</").append(name).append(">\n");
        }

        return s.toString();
    }
}
