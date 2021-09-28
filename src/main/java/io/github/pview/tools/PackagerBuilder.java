package io.github.pview.tools;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public class PackagerBuilder {
    private Path baseDir;
    private String appVersion;
    private Set<String> jvmArgs = Set.of();
    private Set<String> modules = Set.of();
    private String modulePath;
    private String appName;
    private String mainClass;

    public PackagerBuilder baseDirectory(Path baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public PackagerBuilder appVersion(String appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    public PackagerBuilder jvmArguments(Set<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public PackagerBuilder appName(String appName) {
        this.appName = appName;
        return this;
    }

    public PackagerBuilder mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public PackagerBuilder modules(Set<String> modules) {
        this.modules = modules;
        return this;
    }

    public PackagerBuilder modulePath(String modulePath) {
        this.modulePath = modulePath;
        return this;
    }

    public Packager createPackager() {
        Objects.requireNonNull(baseDir);
        Objects.requireNonNull(appVersion);
        Objects.requireNonNull(jvmArgs);
        Objects.requireNonNull(appName);
        Objects.requireNonNull(mainClass);

        Objects.requireNonNull(modulePath);
        Objects.requireNonNull(modules);

        return new Packager(baseDir, appVersion, Set.copyOf(jvmArgs), appName, mainClass, modulePath, Set.copyOf(modules));
    }
}