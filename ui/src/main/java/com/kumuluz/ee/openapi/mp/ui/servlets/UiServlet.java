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
package com.kumuluz.ee.openapi.mp.ui.servlets;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

import javax.servlet.UnavailableException;

/**
 * Static file server for Swagger UI, modified to remove relative UI path when looking for resources.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class UiServlet extends DefaultServlet {

    private static final Logger LOG = Log.getLogger(UiServlet.class);

    private String uiPath;

    @Override
    public void init() throws UnavailableException {
        super.init();
        uiPath = getInitParameter("uiPath");
    }

    @Override
    public Resource getResource(String pathInContext) {

        if (pathInContext.startsWith(this.uiPath)) {
            // request starts with path (as it should), remove path when looking for static files
            pathInContext = pathInContext.substring(uiPath.length());
        } else {
            // this should be unreachable, since this servlet should be mapped to uiPath/*
            LOG.warn("Could not remove uiPath (" + uiPath + ") from path: " + pathInContext);
            return null;
        }

        return super.getResource(pathInContext);
    }
}
