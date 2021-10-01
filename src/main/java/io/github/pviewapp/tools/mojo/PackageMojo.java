package io.github.pviewapp.tools.mojo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.pviewapp.tools.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldMayBeFinal"})
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute()
public class PackageMojo extends AbstractMojo {
    public static final String RUNTIME_PATH_PROPERTY = "pview.tools.runtimePath";
    public static final String NATIVE_PACKAGE_PATH_PROPERTY = "pview.tools.nativePackagePath";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File baseDir;

    @Parameter(defaultValue = "${project.name}")
    private String appName;

    @Parameter(defaultValue = "${project.version}")
    private String appVersion;

    /**
     * The main executable class.
     */
    @Parameter(required = true)
    private String mainClass;

    @Parameter(required = true)
    private String mainModule;

    @Parameter
    private List<String> jvmArguments = List.of();

    @Parameter
    private Set<String> additionalModules = Set.of();

    @Parameter
    private File runtimePath = null;

    @Parameter( defaultValue = "${build.directory}")
    private File outputPath;

    @Parameter(defaultValue = "runtime")
    private String runtimeName;

    @Parameter(defaultValue = "${project.name}-${project.version}.%s")
    private String compressedPackageName;

    @Parameter
    private List<String> jlinkArguments = List.of();

    @Parameter
    private List<String> jpackageArguments = List.of();

    @Parameter
    private List<String> jpackageArgumentsWin = List.of();

    @Parameter
    private List<String> jpackageArgumentsMac = List.of();

    @Parameter
    private List<String> jpackageArgumentsLinux = List.of();

    @Parameter
    private File resourceDirectory = null;

    @Parameter(required = true)
    private File iconDirectory;

    private Path effectiveRuntimePath;

    private Packager packager;

    /**
     * The name of the native package.
     *
     * Use {@code %s} to get the platform-specific file extension. For example,
     * {@code %s} might be {@code msi} on Windows.
     */
    @Parameter
    private String nativePackageName = null;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            packager = createPackager();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to begin packaging", e);
        }

        final Path runtimePath = getRuntime();

        Path nativePackagePath;

        try {
            nativePackagePath = packager.generateNativePackage(outputPath.toPath(), runtimePath);
        } catch (InterruptedException | IOException e) {
            throw new MojoExecutionException("Execution during package runtime generation: " + e);
        }

        if (nativePackageName != null) {
            final var nativePackageFileName = nativePackagePath.getFileName().toString();
            try {
                nativePackagePath = Files.move(nativePackagePath, nativePackagePath.getParent().resolve(
                                String.format(nativePackageName, nativePackageFileName.substring(nativePackageFileName.lastIndexOf(".") + 1))
                ), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to rename file using format " + nativePackageName, e);
            }
        }

        try {
            createCompressedPackage();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create zip package", e);
        }

        project.getProperties().setProperty(NATIVE_PACKAGE_PATH_PROPERTY, nativePackagePath.toAbsolutePath().toString());
    }

    private Packager createPackager() throws IOException {
        final List<String> jpackageArgs = new ArrayList<>(jpackageArguments);

        switch (Platform.getCurrentPlatform()) {
            case WINDOWS:
                jpackageArgs.addAll(jpackageArgumentsWin);
                break;
            case MAC:
                jpackageArgs.addAll(jpackageArgumentsMac);
                break;
            case UNIX:
                jpackageArgs.addAll(jpackageArgumentsLinux);
                break;
        }

        jpackageArgs.add("--icon");
        jpackageArgs.add(Icons.get(iconDirectory.toPath()).toString());

        if (resourceDirectory != null) {
            jpackageArgs.add("-i");
            jpackageArgs.add(resourceDirectory.getAbsolutePath());
        }

        try {
            final var modulePath = String.join(File.pathSeparator, project.getRuntimeClasspathElements());
            getLog().debug("Using module path: " + modulePath);

            final var modules = new HashSet<String>(additionalModules.size() + 1);

            modules.addAll(additionalModules);
            modules.add(mainModule);

            return new PackagerBuilder()
                    .appName(appName)
                    .appVersion(appVersion)
                    .jvmArguments(jvmArguments)
                    .mainClass(mainModule + "/" + mainClass)
                    .modules(modules)
                    .baseDirectory(baseDir.toPath())
                    .modulePath(modulePath)
                    .jlinkArgs(jlinkArguments)
                    .jpackageArgs(jpackageArgs)
                    .createPackager();
        } catch (DependencyResolutionRequiredException e) {
            throw new AssertionError(e);
        }
    }

    private Path getRuntime() throws MojoExecutionException {
        if (this.runtimePath == null) {
            try {
                project.getProperties().setProperty(RUNTIME_PATH_PROPERTY,
                        (effectiveRuntimePath = packager.generateRuntime(outputPath.toPath().resolve(runtimeName)))
                                .toString());
            } catch (IOException | InterruptedException e) {
                throw new MojoExecutionException("Execution during package runtime generation: " + e);
            }
        } else {
            effectiveRuntimePath = this.runtimePath.toPath();
        }

        return effectiveRuntimePath;
    }

    // todo add other platform support
    @SuppressWarnings({"UnusedReturnValue", "SwitchStatementWithTooFewBranches"})
    private Path createCompressedPackage() throws IOException {
        switch (Platform.getCurrentPlatform()) {
            case WINDOWS:
                return createWindowsCompressedPackage();
            default:
                return null;
        }
    }

    private Path createWindowsCompressedPackage() throws IOException {
        if (Platform.getCurrentPlatform() != Platform.WINDOWS) {
            throw new UnsupportedOperationException("Platform needs to be windows for zip package");
        }

        final File resourceDest = effectiveRuntimePath.resolve("resources").toFile();

        FileUtils.copyDirectory(resourceDirectory, resourceDest);

        final var winWrapper = new Launch4JBuilder()
                .outputFile(effectiveRuntimePath.resolve(appName + ".exe"))
                .jvmArgs(jvmArguments.stream().map(s -> s.replace("$RESOURCE_DIR", "./resources"))
                        .collect(Collectors.toUnmodifiableList()))
                .mainClass(mainClass)
                .iconDirectory(iconDirectory.toPath())
                .build(outputPath.toPath().resolve("launch4j-temp"));

        final var zipLocation =
                new File(outputPath, String.format(compressedPackageName, "zip"));

        ZipUtil.pack(effectiveRuntimePath.toFile(), zipLocation);

        Threads.pause();

        Files.delete(winWrapper);
        FileUtils.deleteDirectory(resourceDest);

        return zipLocation.toPath();
    }
}
