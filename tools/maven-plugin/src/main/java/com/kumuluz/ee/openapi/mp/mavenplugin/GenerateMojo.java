/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
 * Saves OpenAPI schema to file by delegating execution to SmallRye plugin.
 *
 * @author benjamink, Sunesis ltd.
 * @since 1.3.0
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

        String scanClassesValue;
        try {
            scanClassesValue = getScanClassesValue();
        } catch (IOException e) {
            throw new MojoExecutionException("Can't obtain all scan classes.");
        }

        addChild(configuration, "scanClasses", scanClassesValue);
        addChild(configuration, "scanExcludeClasses", "org.glassfish.jersey.server.ResourceConfig", true);
        addChild(configuration, "scanExcludePackages", "org.glassfish.jersey.server.wadl", true);

        if (scanLibraries == null || scanLibraries.isEmpty()) {
            addChild(configuration, "scanDependenciesDisable", "true");
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
        if (scanLibraries != null && !scanLibraries.isEmpty()) {
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

    private void addChild(Xpp3Dom configuration, String name, String value) {
        addChild(configuration, name, value, false);
    }

    private void addChild(Xpp3Dom configuration, String name, String value, boolean appendValue) {
        Xpp3Dom child = configuration.getChild(name);
        if (child != null) {
            if (appendValue) {
                String currentValue = child.getValue();
                child.setValue(value +
                        (currentValue != null && currentValue.isEmpty()
                                ? "," + currentValue
                                : ""
                        )
                );
            } else {
                child.setValue(value);
            }
        } else {
            child = new Xpp3Dom(name);
            child.setValue(value);
            configuration.addChild(child);
        }
    }

    private void removeChild(Xpp3Dom configuration, String name) {
        int i = 0;

        for (Xpp3Dom child : configuration.getChildren()) {
            if (child.getName().equals(name)) {
                configuration.removeChild(i);
                break;
            }
            i++;
        }
    }
}
