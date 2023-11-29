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

import com.kumuluz.ee.openapi.mp.util.MediaTypeUtil;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

            Format format = null;

            // format query parameter can override format
            String queryParameterFormat = req.getParameter("format");
            if (queryParameterFormat != null) {
                if (queryParameterFormat.equalsIgnoreCase("json")) {
                    format = Format.JSON;
                } else if (queryParameterFormat.equalsIgnoreCase("yml") ||
                        queryParameterFormat.equalsIgnoreCase("yaml")) {
                    format = Format.YAML;
                }
            }

            if (format == null) {
                // respect Accept header
                format = MediaTypeUtil.parseMediaType(req.getHeader("Accept"));
            }

            if (format == null) {
                // by default use yaml
                format = Format.YAML;
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
