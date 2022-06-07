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

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.microprofile.openapi.OASConfig;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.constants.OpenApiConstants;

/**
 * @author benjamink, Sunesis ltd.
 * @since 1.3.1
 */
public class MavenConfig implements OpenApiConfig {

    private final Map<String, String> properties;

    public MavenConfig(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public String modelReader() {
        return properties.getOrDefault(OASConfig.MODEL_READER, null);
    }

    @Override
    public String filter() {
        return properties.getOrDefault(OASConfig.FILTER, null);
    }

    @Override
    public boolean scanDisable() {
        return Boolean.parseBoolean(properties.getOrDefault(OASConfig.SCAN_DISABLE, "false"));
    }

    @Override
    public Pattern scanPackages() {
        return patternOf(properties.getOrDefault(OASConfig.SCAN_PACKAGES, null));
    }

    @Override
    public Pattern scanClasses() {
        return patternOf(properties.getOrDefault(OASConfig.SCAN_CLASSES, null));
    }

    @Override
    public Pattern scanExcludePackages() {
        return patternOf(properties.getOrDefault(OASConfig.SCAN_EXCLUDE_PACKAGES, null), OpenApiConstants.NEVER_SCAN_PACKAGES);
    }

    @Override
    public Pattern scanExcludeClasses() {
        return patternOf(properties.getOrDefault(OASConfig.SCAN_EXCLUDE_CLASSES, null), OpenApiConstants.NEVER_SCAN_CLASSES);
    }

    @Override
    public Set<String> servers() {
        return asCsvSet(properties.getOrDefault(OASConfig.SERVERS, null));
    }

    @Override
    public Set<String> pathServers(String path) {
        return asCsvSet(properties.getOrDefault(OASConfig.SERVERS_PATH_PREFIX + path, null));
    }

    @Override
    public Set<String> operationServers(String operationId) {
        return asCsvSet(properties.getOrDefault(OASConfig.SERVERS_OPERATION_PREFIX + operationId, null));
    }

    @Override
    public boolean scanDependenciesDisable() {
        return Boolean.parseBoolean(properties.getOrDefault(OpenApiConstants.SMALLRYE_SCAN_DEPENDENCIES_DISABLE, "false"));
    }

    @Override
    public Set<String> scanDependenciesJars() {
        return asCsvSet(properties.getOrDefault(OpenApiConstants.SMALLRYE_SCAN_DEPENDENCIES_JARS, null));
    }

    @Override
    public String customSchemaRegistryClass() {
        return properties.getOrDefault(OpenApiConstants.SMALLRYE_CUSTOM_SCHEMA_REGISTRY_CLASS, null);
    }

    @Override
    public boolean applicationPathDisable() {
        return Boolean.parseBoolean(properties.getOrDefault(OpenApiConstants.SMALLRYE_APP_PATH_DISABLE, "false"));
    }

    @Override
    public String getOpenApiVersion() {
        return properties.getOrDefault(OpenApiConstants.VERSION, null);
    }

    @Override
    public String getInfoTitle() {
        return properties.getOrDefault(OpenApiConstants.INFO_TITLE, null);
    }

    @Override
    public String getInfoVersion() {
        return properties.getOrDefault(OpenApiConstants.INFO_VERSION, null);
    }

    @Override
    public String getInfoDescription() {
        return properties.getOrDefault(OpenApiConstants.INFO_DESCRIPTION, null);
    }

    @Override
    public String getInfoTermsOfService() {
        return properties.getOrDefault(OpenApiConstants.INFO_TERMS, null);
    }

    @Override
    public String getInfoContactEmail() {
        return properties.getOrDefault(OpenApiConstants.INFO_CONTACT_EMAIL, null);
    }

    @Override
    public String getInfoContactName() {
        return properties.getOrDefault(OpenApiConstants.INFO_CONTACT_NAME, null);
    }

    @Override
    public String getInfoContactUrl() {
        return properties.getOrDefault(OpenApiConstants.INFO_CONTACT_URL, null);
    }

    @Override
    public String getInfoLicenseName() {
        return properties.getOrDefault(OpenApiConstants.INFO_LICENSE_NAME, null);
    }

    @Override
    public String getInfoLicenseUrl() {
        return properties.getOrDefault(OpenApiConstants.INFO_LICENSE_URL, null);
    }

    @Override
    public OperationIdStrategy getOperationIdStrategy() {
        String strategy = properties.getOrDefault(OpenApiConstants.OPERATION_ID_STRAGEGY, null);
        if (strategy != null) {
            return OperationIdStrategy.valueOf(strategy);
        }
        return null;
    }

    @Override
    public Set<String> getScanProfiles() {
        return asCsvSet(properties.getOrDefault(OpenApiConstants.SCAN_PROFILES, null));
    }

    @Override
    public Set<String> getScanExcludeProfiles() {
        return asCsvSet(properties.getOrDefault(OpenApiConstants.SCAN_EXCLUDE_PROFILES, null));
    }
}
