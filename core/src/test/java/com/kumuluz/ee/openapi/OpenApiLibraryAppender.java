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
package com.kumuluz.ee.openapi;

import com.kumuluz.ee.openapi.mp.OpenApiMpExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Builds the KumuluzEE OpenAPI extension.
 *
 * @author Domen Kajdič
 * @since 1.0.0
 */
public class OpenApiLibraryAppender extends CachedAuxilliaryArchiveAppender {
    @Override
    protected Archive<?> buildArchive() {
        return ShrinkWrap.create(JavaArchive.class, "kumuluzee-openapi.jar")
                .addPackages(true, OpenApiMpExtension.class.getPackage())
                .addAsServiceProvider(com.kumuluz.ee.common.Extension.class, OpenApiMpExtension.class)
                .addAsServiceProvider(org.eclipse.microprofile.openapi.spi.OASFactoryResolver.class,
                        io.smallrye.openapi.spi.OASFactoryResolverImpl.class);
    }
}
