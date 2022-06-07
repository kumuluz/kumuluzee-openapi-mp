package com.kumuluz.ee.openapi.mp.ui.filters;

import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;

import java.util.Collections;
import java.util.Map;

/**
 * Server implementation used to insert generated server into Swagger UI server list.
 *
 * @author Michal Vrsansky
 */
public class CustomServer implements Server {

    private final String url;

    public CustomServer(String url) {
        this.url = url;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
    }

    @Override
    public String getDescription() {
        return "(added by auto-configuration)";
    }

    @Override
    public void setDescription(String description) {
    }

    @Override
    public Map<String, ServerVariable> getVariables() {
        return Collections.emptyMap();
    }

    @Override
    public Server addVariable(String s, ServerVariable serverVariable) {
        return this;
    }

    @Override
    public void removeVariable(String s) {
    }

    @Override
    public void setVariables(Map<String, ServerVariable> variables) {
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Collections.emptyMap();
    }

    @Override
    public Server addExtension(String name, Object value) {
        return this;
    }

    @Override
    public void removeExtension(String name) {
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
    }
}