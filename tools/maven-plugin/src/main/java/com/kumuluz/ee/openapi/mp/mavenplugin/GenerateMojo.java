package com.kumuluz.ee.openapi.mp.mavenplugin;

import io.smallrye.openapi.mavenplugin.GenerateSchemaMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

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

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();
    }
}
