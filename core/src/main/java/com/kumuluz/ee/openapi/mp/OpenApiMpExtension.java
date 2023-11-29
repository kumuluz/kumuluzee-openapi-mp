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
package com.kumuluz.ee.openapi.mp;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.exceptions.KumuluzServerException;
import com.kumuluz.ee.common.utils.ResourceUtils;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.openapi.mp.spi.OASFilterProvider;
import com.kumuluz.ee.openapi.mp.utils.JarUtils;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;

/**
 * MicroProfile OpenAPI extension.
 *
 * @author Domen Kajdič
 * @since 1.0.0
 */
@EeExtensionDef(name = "OpenApiMp", group = "OPEN_API")
@EeComponentDependency(EeComponentType.JAX_RS)
public class OpenApiMpExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(OpenApiMpExtension.class.getName());

    @Override
    public void load() {
        try {
            Class.forName("com.kumuluz.ee.config.microprofile.MicroprofileConfigExtension");
        } catch (ClassNotFoundException e) {
            throw new KumuluzServerException("KumuluzEE Config MP Extension is required by the OpenAPI MP Extension. " +
                    "Please include it in the dependencies.");
        }
    }

    private OpenApiStaticFile getStaticFiles() {
        OpenApiStaticFile staticFile = new OpenApiStaticFile();
        ClassLoader classLoader = getClass().getClassLoader();
        staticFile.setFormat(Format.YAML);

        InputStream stream = classLoader.getResourceAsStream("META-INF/openapi.yaml");

        if (stream == null) {
            stream = classLoader.getResourceAsStream("META-INF/openapi.yml");
        }

        if (stream == null) {
            stream = classLoader.getResourceAsStream("META-INF/openapi.json");
            staticFile.setFormat(Format.JSON);
        }

        if (stream == null) {
            return null;
        } else {
            staticFile.setContent(stream);
            return staticFile;
        }
    }

    private Index getIndex(OpenApiConfig config) {

        ClassGraph classGraph = new ClassGraph().enableClassInfo();

        if (ConfigurationUtil.getInstance().getBoolean("kumuluzee.openapi-mp.scanning.debug").orElse(false)) {
            classGraph = classGraph.verbose();
        }

        // disable Jersey wadl
        classGraph.rejectPackages("org.glassfish.jersey.server.wadl");
        // disable Jersey ResourceConfig
        classGraph.rejectClasses(ResourceConfig.class.getName());

        if (ConfigurationUtil.getInstance().getBoolean("kumuluzee.openapi-mp.scanning.optimize").orElse(true)) {
            List<String> scanJars = new LinkedList<>(); // which jars should ClassGraph scan

            // if in jar add main jar name
            if (ResourceUtils.isRunningInJar()) {
                try {

                    Class.forName("com.kumuluz.ee.loader.EeClassLoader");
                    scanJars.add(JarUtils.getMainJarName());

                } catch (ClassNotFoundException e) {
                    // this should not fail since we check if we are running in jar beforehand
                    // if you get this warning you are probably doing something weird with packaging
                    LOG.warning("Could not load EeClassLoader, OpenAPI specification may not work as expected. " +
                            "Are you running in UberJAR created by KumuluzEE Maven plugin?");
                }
            }

            // add jars from kumuluzee.dev.scan-libraries configuration
            List<String> scanLibraries = EeConfig.getInstance().getDev().getScanLibraries();
            if (scanLibraries != null) {
                scanJars.addAll(scanLibraries);
            }

            if (scanJars.isEmpty()) {
                // running exploded with no scan-libraries defined in config
                classGraph.disableJarScanning();
            } else {
                // running in jar or scan-libraries defined in config
                for (String scanJar : scanJars) {
                    // scan-libraries allows two formats:
                    // - artifact-1.0.0-SNAPSHOT.jar
                    // - artifact
                    if (scanJar.endsWith(".jar")) {
                        classGraph.acceptJars(scanJar);
                    } else {
                        classGraph.acceptJars(scanJar + "-*.jar");
                    }
                }
            }
        }

        // include/exclude according to configuration defined in MP spec
        for (String c : patternToStringArray(config.scanClasses())) {
            LOG.info("Including class: " + c);
            classGraph.acceptClasses(c);
        }

        for (String p : patternToStringArray(config.scanPackages())) {
            LOG.info("Including package: " + p);
            classGraph.acceptPackages(p);
        }

        for (String c : patternToStringArray(config.scanExcludeClasses())) {
            LOG.info("Excluding class: " + c);
            classGraph.rejectClasses(c);
        }

        for (String p : patternToStringArray(config.scanExcludePackages())) {
            LOG.info("Excluding package: " + p);
            classGraph.rejectPackages(p);
        }

        ScanResult scanResult = classGraph.scan();

        ClassInfoList classInfoList = scanResult.getAllClasses();
        Indexer indexer = new Indexer();
        ClassLoader classLoader = getClass().getClassLoader();

        for (ClassInfo classInfo : classInfoList) {
            try {
                indexer.index(classLoader.getResourceAsStream(classInfo.getName().replaceAll("\\.", "/") + ".class"));
            } catch (IOException e) {
                LOG.warning("Skipped scanning class: " + classInfo.getName());
            }
        }
        scanResult.close();

        return indexer.complete();
    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        OpenApiConfig config = new OpenApiConfigImpl(ConfigProvider.getConfig());
        ClassLoader classLoader = getClass().getClassLoader();

        OpenApiDocument openApiDocument = OpenApiDocument.INSTANCE;
        openApiDocument.config(config);
        openApiDocument.modelFromReader(OpenApiProcessor.modelFromReader(config, classLoader));
        openApiDocument.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(getStaticFiles()));
        if (!config.scanDisable()) {
            openApiDocument.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, getIndex(config)));
        }
        openApiDocument.filter(OpenApiProcessor.getFilter(config, classLoader));
        for (OASFilterProvider filterProvider : ServiceLoader.load(OASFilterProvider.class)) {
            openApiDocument.filter(filterProvider.registerOasFilter());
        }
        openApiDocument.initialize();

        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {
            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            // will get mapped to kumuluzee.openapi-mp.servlet.mapping as well
            String mapping = ConfigurationUtil.getInstance().get("mp.openapi.servlet.mapping").orElse("/openapi");

            server.registerServlet(OpenApiMPServlet.class, mapping);
        }
    }

    @Override
    public boolean isEnabled() {
        ConfigurationUtil config = ConfigurationUtil.getInstance();
        return config.getBoolean("mp.openapi.enabled")
                .orElse(config.getBoolean("kumuluzee.openapi-mp.enabled")
                        .orElse(true));
    }

    private String[] patternToStringArray(Set<String> pattern) {
        if (pattern == null) {
            return new String[]{};
        }

        return pattern.stream()
                .map(s -> s.replaceAll("\\(", ""))
                .map(s -> s.replaceAll("\\)", ""))
                .map(s -> s.replaceAll("\\\\Q", ""))
                .map(s -> s.replaceAll("\\\\E", ""))
                .filter(s -> !s.isEmpty())
                .toList()
                .toArray(new String[]{});
    }
}
