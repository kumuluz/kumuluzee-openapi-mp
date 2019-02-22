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
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import java.io.IOException;
import java.io.InputStream;
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
        staticFile.setFormat(OpenApiSerializer.Format.YAML);

        InputStream stream = classLoader.getResourceAsStream("META-INF/openapi.yaml");

        if(stream == null) {
            stream = classLoader.getResourceAsStream("META-INF/openapi.yml");
        }

        if(stream == null) {
            stream = classLoader.getResourceAsStream("META-INF/openapi.json");
            staticFile.setFormat(OpenApiSerializer.Format.JSON);
        }

        if (stream == null) {
            return null;
        } else {
            staticFile.setContent(stream);
            return staticFile;
        }
    }

    private Index getIndex(OpenApiConfig config, ClassLoader classLoader) {
        Indexer indexer = new Indexer();
        ClassGraph classGraph = new ClassGraph().enableClassInfo();
        classGraph.blacklistPackages("org.glassfish.jersey.server.wadl");
        for(String c: config.scanClasses()) {
            LOG.info("Including class: " + c);
            classGraph.whitelistClasses(c);
        }
        for(String p: config.scanPackages()) {
            LOG.info("Including package: " + p);
            classGraph.whitelistPackages(p);
        }
        for(String c: config.scanExcludeClasses()) {
            LOG.info("Excluding class: " + c);
            classGraph.blacklistClasses(c);
        }
        for(String p: config.scanExcludePackages()) {
            LOG.info("Excluding package: " + p);
            classGraph.blacklistPackages(p);
        }
        ScanResult scanResult = classGraph.scan();
        ClassInfoList classInfoList = scanResult.getAllClasses();
        for(ClassInfo classInfo: classInfoList) {
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
        if(!config.scanDisable()) {
            openApiDocument.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, getIndex(config, classLoader)));
        }
        openApiDocument.filter(OpenApiProcessor.getFilter(config, classLoader));
        openApiDocument.initialize();

        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {
            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            // will get mapped to kumuluzee.openapi.servlet.mapping as well
            String mapping = ConfigurationUtil.getInstance().get("mp.openapi.servlet.mapping").orElse("/openapi");

            server.registerServlet(OpenApiMPServlet.class, mapping);
        }
    }

    @Override
    public boolean isEnabled() {
        ConfigurationUtil config = ConfigurationUtil.getInstance();
        return config.getBoolean("mp.openapi.enabled")
                .orElse(config.getBoolean("kumuluzee.openapi.enabled")
                        .orElse(true));
    }
}
