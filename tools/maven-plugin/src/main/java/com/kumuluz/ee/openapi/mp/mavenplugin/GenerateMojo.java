package com.kumuluz.ee.openapi.mp.mavenplugin;

import io.smallrye.openapi.mavenplugin.GenerateSchemaMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.JarIndexer;
import org.jboss.jandex.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @author benjamink, Sunesis ltd.
 * @since 1.0.0
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class GenerateMojo extends GenerateSchemaMojo {

    private static final ResourceBundle versionsBundle = ResourceBundle.getBundle("META-INF/kumuluzee/openapi/versions");

    @Parameter(property = "scanLibraries")
    private List<String> scanLibraries;

    @Parameter(property = "scanClasses")
    private String scanClasses;

    /**
     * Compiled classes of the project.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDir")
    private File classesDir;

    @Parameter(defaultValue = "${project}")
    private MavenProject mavenProject;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("io.smallrye"),
                        artifactId("smallrye-open-api-maven-plugin"),
                        version(versionsBundle.getString("smallrye-open-api.version"))
                ),
                goal("generate-schema"),
                getModifiedConfiguration(),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

    private Xpp3Dom getModifiedConfiguration() throws MojoExecutionException {
        Xpp3Dom configuration = mavenProject.getBuildPlugins().stream()
                .filter(p -> p.getKey().equals("com.kumuluz.ee.openapi:kumuluzee-openapi-mp-maven-plugin"))
                .findFirst()
                .map(p -> (Xpp3Dom) p.getConfiguration())
                .orElse(new Xpp3Dom("configuration"));

        Xpp3Dom scanClassesChild = configuration.getChild("scanClasses");
        String scanClassesValue;
        try {
            scanClassesValue = getScanClassesValue();
        } catch (IOException e) {
            throw new MojoExecutionException("Can't obtain all scan classes.");
        }

        if (scanClassesChild != null) {
            scanClassesChild.setValue(scanClassesValue);
        } else {
            scanClassesChild = new Xpp3Dom("scanClasses");
            scanClassesChild.setValue(scanClassesValue);
            configuration.addChild(scanClassesChild);
        }

        Xpp3Dom excludeClassesChild = configuration.getChild("scanExcludeClasses");
        if (excludeClassesChild != null) {
            String excludeClassesValue = excludeClassesChild.getValue();
            excludeClassesValue = "org.glassfish.jersey.server.ResourceConfig" +
                    (excludeClassesValue != null && excludeClassesValue.isEmpty()
                            ? "," + excludeClassesValue
                            : ""
                    );
            excludeClassesChild.setValue(excludeClassesValue);
        } else {
            excludeClassesChild = new Xpp3Dom("scanExcludeClasses");
            excludeClassesChild.setValue("org.glassfish.jersey.server.ResourceConfig");
            configuration.addChild(excludeClassesChild);
        }

        Xpp3Dom excludePackagesChild = configuration.getChild("scanExcludePackages");
        if (excludePackagesChild != null) {
            String excludeClassesValue = excludePackagesChild.getValue();
            excludeClassesValue = "org.glassfish.jersey.server.wadl" +
                    (excludeClassesValue != null && excludeClassesValue.isEmpty()
                            ? "," + excludeClassesValue
                            : ""
                    );
            excludePackagesChild.setValue(excludeClassesValue);
        } else {
            excludePackagesChild = new Xpp3Dom("scanExcludePackages");
            excludePackagesChild.setValue("org.glassfish.jersey.server.wadl");
            configuration.addChild(excludePackagesChild);
        }

        removeChild(configuration, "scanLibraries");

        return configuration;
    }

    private String getScanClassesValue() throws IOException {
        List<String> scanClassesList = new ArrayList<>();
        List<String> scanJars = new LinkedList<>();

        // classes of this module
        Indexer indexer = new Indexer();
        List<Path> classFiles = Files.walk(classesDir.toPath())
                .filter(path -> path.toString().endsWith(".class"))
                .collect(Collectors.toList());
        for (Path path : classFiles) {
            indexer.index(Files.newInputStream(path));
        }
        indexer.complete().getKnownClasses()
                .forEach(c -> scanClassesList.add(c.name().toString()));

        // add jars from scanLibraries configuration
        if (scanLibraries != null) {
            scanJars.addAll(scanLibraries);
        }

        if (!scanJars.isEmpty()) {
            for (Artifact artifact : mavenProject.getArtifacts()) {
                String artifactId = artifact.getArtifactId();
                String version = artifact.getVersion();

                if (scanJars.contains(artifactId) || scanJars.contains(artifactId + "-" + version + ".jar")) {
                    try {
                        Result result = JarIndexer.createJarIndex(artifact.getFile(), new Indexer(), false, false, false);
                        result.getIndex().getKnownClasses()
                                .forEach(c -> scanClassesList.add(c.name().toString()));
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
        }

        return String.join(",", scanClassesList) +
                (scanClasses != null && !scanClasses.isEmpty()
                        ? "," + scanClasses
                        : ""
                );
    }

    private String[] stringToStringArray(String value) {
        if (value == null) {
            return new String[] {};
        }

        return Arrays.stream(value.split(","))
                .map(s -> s.replaceAll("\\s+", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList())
                .toArray(new String[] {});
    }

    private void removeChild(Xpp3Dom configuration, String childName) {
        int i = 0;

        for (Xpp3Dom child : configuration.getChildren()) {
            if (child.getName().equals(childName)) {
                configuration.removeChild(i);
                break;
            }
            i++;
        }
    }

    private boolean isRunningInJar() {
        return mavenProject.getBuildPlugins().stream()
                .filter(p -> p.getKey().equals("com.kumuluz.ee:kumuluzee-maven-plugin"))
                .flatMap(p -> p.getExecutions().stream())
                .flatMap(e -> e.getGoals().stream())
                .anyMatch(g -> g.equals("repackage"));
    }
}
