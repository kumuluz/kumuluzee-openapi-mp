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

import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Servlet for serving the OpenAPI file.
 *
 * @author Domen Kajdič
 * @since 1.0.0
 */
public class OpenApiMPServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        PrintWriter writer = resp.getWriter();
        if (OpenApiDocument.INSTANCE.isSet()) {

            // by default use yaml
            Format format = Format.YAML;

            // respect Accept header
            if (req.getHeader("Accept").equals(MediaType.APPLICATION_JSON)) {
                format = Format.JSON;
            }

            // format query parameter can override format
            if (req.getParameter("format") != null) {
                if (req.getParameter("format").equalsIgnoreCase("json")) {
                    format = Format.JSON;
                } else {
                    format = Format.YAML;
                }
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(format.getMimeType());
            String output = OpenApiSerializer.serialize(OpenApiDocument.INSTANCE.get(), format);
            writer.println(output);
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.println("Something went wrong generating the OpenAPI document. Check application logs for more information.");
        }
        writer.close();
    }
}
