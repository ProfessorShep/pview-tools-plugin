package io.github.pview.tools.mojo;

import io.github.pview.tools.Packager;
import io.github.pview.tools.PackagerBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractMojo {
    public static final String PVIEW_RUNTIME_PATH_PROPERTY = "pview.tools.runtimePath";
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(name = "baseDir", defaultValue = "${project.basedir}")
    private File baseDir;

    @Parameter(name = "appName", defaultValue = "${project.name}")
    private String appName;

    @Parameter(name = "appVersion", defaultValue = "${project.version}")
    private String appVersion;

    @Parameter(name = "modulePath", defaultValue = "${project.basedir}/classes")
    private String modulePath;

    /**
     * The main executable class.
     *
     * Format: {@code <moduleName>/<className>}.
     */
    @Parameter(name = "mainClass")
    private String mainClass;

    @Parameter(name = "jvmArguments")
    private Set<String> jvmArguments = Set.of();

    @Parameter(name = "modules")
    private Set<String> modules = Set.of();

    @Parameter(name = "runtimePath")
    private File runtimePath = null;

    @Parameter(name = "outputPath", defaultValue = "${build.directory}")
    private File outputPath = null;

    @Parameter(name = "runtimeName", defaultValue = "runtime")
    private String runtimeFileName = null;

    /**
     * The name of the native package.
     *
     * Use {@code %s} to get the platform-specific file extension. For example,
     * {@code %s} might be {@code msi} on Windows.
     */
    @Parameter
    private String nativePackageName = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        System.setProperty("pview.tools.jpackage.output", outputPath.toString());

        final var packager = createPackager();

        final Path runtimePath;
        if (this.runtimePath == null) {
            try {
                project.getProperties().setProperty(PVIEW_RUNTIME_PATH_PROPERTY,
                        (runtimePath = packager.generateRuntime(outputPath.toPath().resolve(runtimeFileName)))
                                .toString());
            } catch (IOException | InterruptedException e) {
                throw new MojoExecutionException("Execution during package runtime generation: " + e);
            }
        } else {
            runtimePath = this.runtimePath.toPath();
        }

        Path nativePackagePath;

        try {
            nativePackagePath = packager.generateNativePackage(runtimePath);
        } catch (InterruptedException | IOException e) {
            throw new MojoExecutionException("Execution during package runtime generation: " + e);
        }

        if (nativePackageName != null) {
            final var nativePackageFileName = nativePackagePath.getFileName().toString();
            try {
                Files.move(nativePackagePath, nativePackagePath.getParent().resolve(
                                String.format(nativePackageName, nativePackageFileName.substring(nativePackageFileName.lastIndexOf(".")))
                ), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to rename file using format " + nativePackageName, e);
            }
        }
    }

    private Packager createPackager() {
        return new PackagerBuilder()
                .appName(appName)
                .appVersion(appVersion)
                .jvmArguments(jvmArguments)
                .mainClass(mainClass)
                .modules(modules)
                .baseDirectory(baseDir.toPath())
                .modulePath(modulePath)
                .createPackager();
    }
}
