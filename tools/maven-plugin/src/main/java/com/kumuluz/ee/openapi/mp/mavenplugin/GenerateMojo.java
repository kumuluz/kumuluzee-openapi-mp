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

import com.kumuluz.ee.openapi.mp.spi.ConfigurableOASFilter;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.constants.OpenApiConstants;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;
import org.apache.maven.artifact.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates OpenAPI schema
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
public class GenerateMojo extends AbstractMojo {

    private static final String META_INF_OPENAPI_YAML = "META-INF/openapi.yaml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YAML = "WEB-INF/classes/META-INF/openapi.yaml";
    private static final String META_INF_OPENAPI_YML = "META-INF/openapi.yml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YML = "WEB-INF/classes/META-INF/openapi.yml";
    private static final String META_INF_OPENAPI_JSON = "META-INF/openapi.json";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_JSON = "WEB-INF/classes/META-INF/openapi.json";

    @Parameter(defaultValue = "false", property = "debug")
    private Boolean debug;

    @Parameter(defaultValue = "true", property = "scanningOptimize")
    private Boolean scanningOptimize;

    /**
     * When scanOptimize is true, specify which jars to scan
     */
    @Parameter(property = "scanLibraries")
    private List<String> scanLibraries;

    @Parameter(property = "filters")
    private List<String> filters;

    @Parameter(property = "filterConfig")
    private Map<String, String> filterConfig;

    /**
     * Directory where to output the schemas.
     * If no path is specified, the schema will be printed to the getLog().
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/", property = "outputDirectory")
    private File outputDirectory;

    /**
     * Filename of the schema
     * Default to openapi. So the files created will be openapi.yaml and openapi.json.
     */
    @Parameter(defaultValue = "openapi", property = "schemaFilename")
    private String schemaFilename;

    /**
     * When you include dependencies, we only look at compile and system scopes (by default)
     * You can change that here.
     * Valid options are: compile, provided, runtime, system, test, import
     */
    @Parameter(defaultValue = "compile,system", property = "includeDependenciesScopes")
    private List<String> includeDependenciesScopes;

    /**
     * When you include dependencies, we only look at jars (by default)
     * You can change that here.
     */
    @Parameter(defaultValue = "jar", property = "includeDependenciesTypes")
    private List<String> includeDependenciesTypes;

    @Parameter(defaultValue = "${project}")
    private MavenProject mavenProject;

    /**
     * Compiled classes of the project.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "classesDir")
    private File classesDir;

    @Parameter(property = "configProperties")
    private File configProperties;

    // Properies as per OpenAPI Config.

    @Parameter(property = "modelReader")
    private String modelReader;

//    @Parameter(property = "filter")
//    private String filter;

    @Parameter(property = "scanDisabled")
    private Boolean scanDisabled;

    @Parameter(property = "scanPackages")
    private List<String> scanPackages;

    @Parameter(property = "scanClasses")
    private List<String> scanClasses;

    @Parameter(property = "scanExcludePackages")
    private List<String> scanExcludePackages;

    @Parameter(property = "scanExcludeClasses")
    private List<String> scanExcludeClasses;

    @Parameter(property = "servers")
    private List<String> servers;

    @Parameter(property = "pathServers")
    private List<String> pathServers;

    @Parameter(property = "operationServers")
    private List<String> operationServers;

    @Parameter(property = "scanDependenciesDisable")
    private Boolean scanDependenciesDisable;

    @Parameter(property = "scanDependenciesJars")
    private List<String> scanDependenciesJars;

    @Parameter(property = "schemaReferencesEnable")
    private Boolean schemaReferencesEnable;

    @Parameter(property = "customSchemaRegistryClass")
    private String customSchemaRegistryClass;

    @Parameter(property = "applicationPathDisable")
    private Boolean applicationPathDisable;

    @Parameter(property = "openApiVersion")
    private String openApiVersion;

    @Parameter(property = "infoTitle")
    private String infoTitle;

    @Parameter(property = "infoVersion")
    private String infoVersion;

    @Parameter(property = "infoDescription")
    private String infoDescription;

    @Parameter(property = "infoTermsOfService")
    private String infoTermsOfService;

    @Parameter(property = "infoContactEmail")
    private String infoContactEmail;

    @Parameter(property = "infoContactName")
    private String infoContactName;

    @Parameter(property = "infoContactUrl")
    private String infoContactUrl;

    @Parameter(property = "infoLicenseName")
    private String infoLicenseName;

    @Parameter(property = "infoLicenseUrl")
    private String infoLicenseUrl;

    @Parameter(property = "operationIdStrategy")
    private String operationIdStrategy;

    @Override
    public void execute() throws MojoExecutionException {
        Thread.currentThread().setContextClassLoader(getClassLoader());

        try {
            IndexView index = createIndex();
            OpenApiDocument schema = generateSchema(index);
            if (schema != null) {
                write(schema);
            } else {
                getLog().warn("No Schema generated. Check that your code contains the MicroProfile OpenAPI Annotations");
            }
        } catch (IOException ex) {
            getLog().error(ex);
            throw new MojoExecutionException("Could not generate OpenAPI Schema", ex); // TODO allow failOnError = false ?
        }
    }

    private ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = (List<String>) mavenProject.getCompileClasspathElements();
            classpathElements.add(mavenProject.getBuild().getOutputDirectory() );
            classpathElements.add(mavenProject.getBuild().getTestOutputDirectory() );
            URL[] urls = new URL[classpathElements.size()];

            for ( int i = 0; i < classpathElements.size(); ++i ) {
                urls[i] = new File(classpathElements.get(i)).toURI().toURL();
            }

            return new URLClassLoader(urls, getClass().getClassLoader() );
        } catch (Exception e) {
            throw new MojoExecutionException("Couldn't create a classloader.", e);
        }
    }

    private IndexView createIndex() throws MojoExecutionException {
        List<IndexView> indexList = new ArrayList<>();

        ClassGraph classGraph = new ClassGraph().enableClassInfo();

        if (debug) {
            classGraph = classGraph.verbose();
        }

        // disable Jersey wadl
        classGraph.rejectPackages("org.glassfish.jersey.server.wadl");
        // disable Jersey ResourceConfig
        classGraph.rejectClasses("org.glassfish.jersey.server.ResourceConfig");

        IndexView moduleIndex;
        try {
            moduleIndex = indexModuleClasses();
            indexList.add(moduleIndex);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't compute index", e);
        }

        if (scanningOptimize) {
            List<String> scanJars = new LinkedList<>(); // which jars should ClassGraph scan

            // add jars from scanLibraries configuration
            if (scanLibraries != null) {
                scanJars.addAll(scanLibraries);
            }

            if (scanJars.isEmpty()) {
                // running exploded with no scan-libraries defined in config
                classGraph.disableJarScanning();
            } else {
                for (Artifact artifact : (Set<Artifact>) mavenProject.getArtifacts()) {
                    String artifactId = artifact.getArtifactId();
                    String version = artifact.getVersion();

                    if (scanJars.contains(artifactId) || scanJars.contains(artifactId + "-" + version + ".jar")) {
                        try {
                            Result result = JarIndexer.createJarIndex(artifact.getFile(), new Indexer(), false, false, false);
                            indexList.add(result.getIndex());
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                }
            }
        } else {
            if (!scanDependenciesDisable) {
                for (Object a : mavenProject.getArtifacts()) {
                    Artifact artifact = (Artifact) a;
                    if (includeDependenciesScopes.contains(artifact.getScope()) && includeDependenciesTypes.contains(artifact.getType())) {
                        try {
                            Result result = JarIndexer.createJarIndex(artifact.getFile(), new Indexer(), false, false, false);
                            indexList.add(result.getIndex());
                        } catch (Exception e) {
                            getLog().error("Can't compute index of " + artifact.getFile().getAbsolutePath() + ", skipping", e);
                        }
                    }
                }
            }
        }

        // include/exclude according to configuration defined in MP spec
        if (scanClasses != null) {
            for (String c : scanClasses) {
                if (debug) {
                    getLog().info("Including class: " + c);
                }
                classGraph.acceptClasses(processString(c));
            }
        }

        if (scanPackages != null) {
            for (String p : scanPackages) {
                if (debug) {
                    getLog().info("Including package: " + p);
                }
                classGraph.acceptPackages(processString(p));
            }
        }

        if (scanExcludePackages != null) {
            for (String c : scanExcludeClasses) {
                if (debug) {
                    getLog().info("Excluding class: " + c);
                }
                classGraph.rejectClasses(processString(c));
            }
        }

        if (scanExcludePackages != null) {
            for (String p : scanExcludePackages) {
                if (debug) {
                    getLog().info("Excluding package: " + p);
                }
                classGraph.rejectPackages(processString(p));
            }
        }

        ScanResult scanResult = classGraph.scan();

        ClassInfoList classInfoList = scanResult.getAllClasses();
        Indexer indexer = new Indexer();
        ClassLoader classLoader = getClass().getClassLoader();

        for (ClassInfo classInfo : classInfoList) {
            try {
                indexer.index(classLoader.getResourceAsStream(classInfo.getName().replaceAll("\\.", "/") + ".class"));
            } catch (IOException e) {
                if (debug) {
                    getLog().warn("Skipped scanning class: " + classInfo.getName());
                }
            }
        }
        scanResult.close();

        Index dependenciesIndex = indexer.complete();
        indexList.add(dependenciesIndex);

        return CompositeIndex.create(indexList);
    }

    // index the classes of this Maven module
    private Index indexModuleClasses() throws IOException {
        Indexer indexer = new Indexer();
        List<Path> classFiles = Files.walk(classesDir.toPath())
                .filter(path -> path.toString().endsWith(".class"))
                .collect(Collectors.toList());
        for (Path path : classFiles) {
            indexer.index(Files.newInputStream(path));
        }
        return indexer.complete();
    }

    private OpenApiDocument generateSchema(IndexView index) throws IOException {
        OpenApiConfig openApiConfig = new MavenConfig(getProperties());

        OpenAPI staticModel = generateStaticModel();
        OpenAPI annotationModel = generateAnnotationModel(index, openApiConfig);

        OpenAPI readerModel = OpenApiProcessor.modelFromReader(openApiConfig, Thread.currentThread().getContextClassLoader());

        OpenApiDocument document = OpenApiDocument.INSTANCE;
        document.reset();
        document.config(openApiConfig);

        if (annotationModel != null) {
            document.modelFromAnnotations(annotationModel);
        }
        if (readerModel != null) {
            document.modelFromReader(readerModel);
        }
        if (staticModel != null) {
            document.modelFromStaticFile(staticModel);
        }
        if (filters != null) {
            filters.forEach(filter -> document.filter(getFilter(filter)));
        }
        document.initialize();

        return document;
    }

    private OpenAPI generateAnnotationModel(IndexView indexView, OpenApiConfig openApiConfig) {
        OpenApiAnnotationScanner openApiAnnotationScanner = new OpenApiAnnotationScanner(openApiConfig, indexView);
        return openApiAnnotationScanner.scan();
    }

    private OpenAPI generateStaticModel() throws IOException {
        Path staticFile = getStaticFile();
        if (staticFile != null) {
            try (InputStream is = Files.newInputStream(staticFile);
                 OpenApiStaticFile openApiStaticFile = new OpenApiStaticFile(is, getFormat(staticFile))) {
                return OpenApiProcessor.modelFromStaticFile(openApiStaticFile);
            }
        }
        return null;
    }

    private Path getStaticFile() {
        Path classesPath = classesDir.toPath();

        if (Files.exists(classesPath)) {
            Path resourcePath = Paths.get(classesPath.toString(), META_INF_OPENAPI_YAML);
            if (Files.exists(resourcePath)) {
                return resourcePath;
            }
            resourcePath = Paths.get(classesPath.toString(), WEB_INF_CLASSES_META_INF_OPENAPI_YAML);
            if (Files.exists(resourcePath)) {
                return resourcePath;
            }
            resourcePath = Paths.get(classesPath.toString(), META_INF_OPENAPI_YML);
            if (Files.exists(resourcePath)) {
                return resourcePath;
            }
            resourcePath = Paths.get(classesPath.toString(), WEB_INF_CLASSES_META_INF_OPENAPI_YML);
            if (Files.exists(resourcePath)) {
                return resourcePath;
            }
            resourcePath = Paths.get(classesPath.toString(), META_INF_OPENAPI_JSON);
            if (Files.exists(resourcePath)) {
                return resourcePath;
            }
            resourcePath = Paths.get(classesPath.toString(), WEB_INF_CLASSES_META_INF_OPENAPI_JSON);
            if (Files.exists(resourcePath)) {
                return resourcePath;
            }
        }
        return null;
    }

    private Format getFormat(Path path) {
        if (path.endsWith(".json")) {
            return Format.JSON;
        }
        return Format.YAML;
    }

    private Map<String, String> getProperties() throws IOException {
        // First check if the configProperties is set, if so, load that.
        Map<String, String> cp = new HashMap<>();
        if (configProperties != null && configProperties.exists()) {
            Properties p = new Properties();
            try (InputStream is = Files.newInputStream(configProperties.toPath())) {
                p.load(is);
                cp.putAll((Map) p);
            }
        }

        // Now add properties set in the maven plugin.

        addToPropertyMap(cp, OASConfig.MODEL_READER, modelReader);
//        addToPropertyMap(cp, OASConfig.FILTER, filter);
        addToPropertyMap(cp, OASConfig.SCAN_DISABLE, scanDisabled);
        addToPropertyMap(cp, OASConfig.SCAN_PACKAGES, String.join(",", Optional.ofNullable(scanPackages).orElse(new ArrayList<>())));
        addToPropertyMap(cp, OASConfig.SCAN_CLASSES, String.join(",", Optional.ofNullable(scanClasses).orElse(new ArrayList<>())));
        addToPropertyMap(cp, OASConfig.SCAN_EXCLUDE_PACKAGES, String.join(",", Optional.ofNullable(scanExcludePackages).orElse(new ArrayList<>())));
        addToPropertyMap(cp, OASConfig.SCAN_EXCLUDE_CLASSES, String.join(",", Optional.ofNullable(scanExcludeClasses).orElse(new ArrayList<>())));
        addToPropertyMap(cp, OASConfig.SERVERS, servers);
        addToPropertyMap(cp, OASConfig.SERVERS_PATH_PREFIX, pathServers);
        addToPropertyMap(cp, OASConfig.SERVERS_OPERATION_PREFIX, operationServers);
        addToPropertyMap(cp, OpenApiConstants.SMALLRYE_SCAN_DEPENDENCIES_DISABLE, scanDependenciesDisable);
        addToPropertyMap(cp, OpenApiConstants.SMALLRYE_SCAN_DEPENDENCIES_JARS, scanDependenciesJars);
        addToPropertyMap(cp, OpenApiConstants.SMALLRYE_SCHEMA_REFERENCES_ENABLE, schemaReferencesEnable);
        addToPropertyMap(cp, OpenApiConstants.SMALLRYE_CUSTOM_SCHEMA_REGISTRY_CLASS, customSchemaRegistryClass);
        addToPropertyMap(cp, OpenApiConstants.SMALLRYE_APP_PATH_DISABLE, applicationPathDisable);
        addToPropertyMap(cp, OpenApiConstants.VERSION, openApiVersion);
        addToPropertyMap(cp, OpenApiConstants.INFO_TITLE, infoTitle);
        addToPropertyMap(cp, OpenApiConstants.INFO_VERSION, infoVersion);
        addToPropertyMap(cp, OpenApiConstants.INFO_DESCRIPTION, infoDescription);
        addToPropertyMap(cp, OpenApiConstants.INFO_TERMS, infoTermsOfService);
        addToPropertyMap(cp, OpenApiConstants.INFO_CONTACT_EMAIL, infoContactEmail);
        addToPropertyMap(cp, OpenApiConstants.INFO_CONTACT_NAME, infoContactName);
        addToPropertyMap(cp, OpenApiConstants.INFO_CONTACT_URL, infoContactUrl);
        addToPropertyMap(cp, OpenApiConstants.INFO_LICENSE_NAME, infoLicenseName);
        addToPropertyMap(cp, OpenApiConstants.INFO_LICENSE_URL, infoLicenseUrl);
        addToPropertyMap(cp, OpenApiConstants.OPERATION_ID_STRAGEGY, operationIdStrategy);

        return cp;
    }

    private void addToPropertyMap(Map<String, String> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }

    private void addToPropertyMap(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void addToPropertyMap(Map<String, String> map, String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            map.put(key, String.join(",", values));
        }
    }

    private void write(OpenApiDocument schema) throws MojoExecutionException {
        try {
            String yaml = OpenApiSerializer.serialize(schema.get(), Format.YAML);
            String json = OpenApiSerializer.serialize(schema.get(), Format.JSON);
            if (outputDirectory == null) {
                // no destination file specified => print to stdout
                getLog().info(yaml);
            } else {
                Path directory = outputDirectory.toPath();
                if (!Files.exists(directory)) {
                    Files.createDirectories(directory);
                }

                writeSchemaFile(directory, schemaFilename + ".yaml", yaml.getBytes());
                writeSchemaFile(directory, schemaFilename + ".json", json.getBytes());

                getLog().info("Wrote the schema files to " + outputDirectory.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write the result", e);
        }
    }

    private void writeSchemaFile(Path directory, String filename, byte[] contents) throws IOException {
        Path file = Paths.get(directory.toString(), filename);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }

        Files.write(file, contents, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private OASFilter getFilter(String filterClassName) {
        if (filterClassName == null) {
            return null;
        } else {
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> c = loader.loadClass(filterClassName);

                OASFilter oasFilter = (OASFilter) c.getDeclaredConstructor().newInstance();
                if (oasFilter instanceof ConfigurableOASFilter) {
                    ConfigurableOASFilter configurableOasFilter = (ConfigurableOASFilter) oasFilter;

                    if (filterConfig != null) {
                        String keyPrefix = filterClassName + ".";

                        filterConfig.entrySet().stream()
                                .filter(e -> e.getKey().startsWith(keyPrefix))
                                .forEach(e -> {
                                    String key = e.getKey().substring(keyPrefix.length());
                                    configurableOasFilter.configure(key, e.getValue());
                                });
                    }
                }

                return oasFilter;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException var4) {
                throw new RuntimeException(var4);
            }
        }
    }

    private String processString(String string) {
        return string.replaceAll("\\(", "")
                .replaceAll("\\)", "")
                .replaceAll("\\\\Q", "")
                .replaceAll("\\\\E", "");
    }
}
