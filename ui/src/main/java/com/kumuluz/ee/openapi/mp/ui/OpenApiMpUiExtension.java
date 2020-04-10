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
package com.kumuluz.ee.openapi.mp.ui;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.utils.ResourceUtils;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.openapi.mp.ui.filters.SwaggerUIFilter;
import com.kumuluz.ee.openapi.mp.ui.servlets.UiServlet;
import io.smallrye.openapi.api.OpenApiDocument;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * OpenAPI MP UI extension. Serves UI on a configured mapping with appropriate parameters.
 *
 * @author Zvone Gazvoda
 * @author Urban Malc
 * @since 1.1.0
 */
@EeExtensionDef(name = "OpenAPI-UI", group = "OPEN_API_UI")
public class OpenApiMpUiExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(OpenApiMpUiExtension.class.getName());

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {
            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            LOG.info("Initializing OpenAPI MP UI extension.");

            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            // will map to kumuluzee.openapi-mp.servlet.mapping too
            String specPath = configurationUtil.get("mp.openapi.servlet.mapping").orElse("/openapi");
            String uiPath = configurationUtil.get("kumuluzee.openapi-mp.ui.mapping").orElse("/api-specs/ui");
            if (uiPath.endsWith("*")) {
                uiPath = uiPath.substring(0, uiPath.length() - 1);
            }
            if (uiPath.endsWith("/")) {
                uiPath = uiPath.substring(0, uiPath.length() - 1);
            }

            if (uiPath.isEmpty()) {
                // not supported as of yet, probably could be done by very strict redirects in SwaggerUIFilter
                throw new IllegalArgumentException("UI cannot be served from root. Please change " +
                        "kumuluzee.openapi-mp.ui.mapping configuration value accordingly.");
            }

            // Get base url of the server which serves the OpenAPI spec. There are 4 possible variants:
            //  1. <protocol>://localhost:<port> (lowest priority)
            //  2. servers parameter in @OpenAPIDefinition annotation (on application class)
            //  3. configuration property: kumuluzee.server.base-url
            //  4. configuration property: kumuluzee.openapi-mp.ui.specification-server (highest priority)

            // 1.
            Integer port;
            String serverUrl = "localhost";
            if (eeConfig.getServer().getHttp() != null) {
                port = eeConfig.getServer().getHttp().getPort();
                serverUrl = "http://" + serverUrl;
            } else {
                port = eeConfig.getServer().getHttps().getPort();
                serverUrl = "https://" + serverUrl;
            }
            serverUrl += (port != null ? ":" + port.toString() : "");

            // 2.
            try {
                if (OpenApiDocument.INSTANCE.get().getServers()!=null
                        && !OpenApiDocument.INSTANCE.get().getServers().isEmpty()) {
                    URL url = new URL(OpenApiDocument.INSTANCE.get().getServers().get(0).getUrl());
                    serverUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
                }
            } catch (MalformedURLException e) {
                LOG.warning("Server URL invalid: " + e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                LOG.warning("Servers not provided in annotation OpenAPIDefinition, will use default server url: " +
                        serverUrl);
            }

            // 3.
            String fromConfig1 = EeConfig.getInstance().getServer().getBaseUrl();
            if (fromConfig1 != null) {
                serverUrl = fromConfig1;
            }

            // 4.
            String fromConfig2 = configurationUtil.get("kumuluzee.openapi-mp.ui.specification-server")
                    .orElse(null);
            if (fromConfig2 != null && !fromConfig2.isEmpty()) {
                serverUrl = fromConfig2;
            }

            // remove trailing slashes
            while (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }

            // static files for Swagger UI, created by Maven download plugin and Maven copy plugin
            URL webApp = ResourceUtils.class.getClassLoader().getResource("swagger-ui/api-specs/ui");

            // context path
            String contextPath = configurationUtil.get("kumuluzee.server.context-path").orElse("");
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }

            if (webApp != null) {

                LOG.info("Swagger UI servlet registered on "+uiPath+ " (servlet context is implied)");
                LOG.info("Swagger UI can be accessed at "+serverUrl + contextPath + uiPath);

                // create servlet that will serve static files
                Map<String, String> swaggerUiParams = new HashMap<>();
                swaggerUiParams.put("resourceBase", webApp.toString());
                swaggerUiParams.put("uiPath", uiPath); //context already included in servlet resolution
                server.registerServlet(UiServlet.class, uiPath + "/*", swaggerUiParams, 1);

                String specUrl = serverUrl + contextPath + specPath;
                String oauth2RedirectUrl = serverUrl + contextPath + uiPath;
                String redirUiPath = contextPath+uiPath;

                LOG.info("Swagger UI spec URL resolved to "+specUrl);

                // create filter that will redirect to Swagger UI with appropriate parameters
                Map<String, String> swaggerUiFilterParams = new HashMap<>();
                swaggerUiFilterParams.put("specUrl", specUrl);
                swaggerUiFilterParams.put("uiPath", redirUiPath);
                swaggerUiFilterParams.put("oauth2RedirectUrl", oauth2RedirectUrl + "/oauth2-redirect.html");
                server.registerFilter(SwaggerUIFilter.class, uiPath + "/*", swaggerUiFilterParams);

            } else {
                LOG.severe("Swagger UI not found. Try cleaning and rebuilding project.");
            }
        } else {
            LOG.severe("KumuluzEE not started with " + JettyServletServer.class.getSimpleName() +
                    ". OpenAPI MP UI extension will not be initialized.");
        }
    }

    @Override
    public void load() {
    }

    @Override
    public boolean isEnabled() {
        return ConfigurationUtil.getInstance().getBoolean("kumuluzee.openapi-mp.ui.enabled").orElse(true);
    }
}
