/*
 *  Copyright (c) 2014-2020 Kumuluz and/or its affiliates
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

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author cen1
 * @since 1.1.0
 */
public class RapiDocServlet extends HttpServlet {

    private String specUrl;

    private String index;

    @Override
    public void init() throws ServletException {
        specUrl = getInitParameter("specUrl");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter out = resp.getWriter();

        //Read the index file which contains rapidoc js and empty rapidoc html tag
        if (index == null) {
            InputStream in = getClass().getClassLoader().getResourceAsStream("webapp/rapidoc/index.html");
            index = IOUtils.toString(in, "UTF-8");

            //Configuration options at https://mrin9.github.io/RapiDoc/api.html
            //Defaults
            Map<String, String> fullConfig = new HashMap<>();
            fullConfig.put("heading-text", "KumuluzEE RapiDoc");
            fullConfig.put("show-header", "false");
            fullConfig.put("spec-url", specUrl);

            //Custom
            Optional<List<String>> keys = ConfigurationUtil.getInstance().getMapKeys("kumuluzee.openapi-mp.ui.extensions.rapidoc");
            if (keys.isPresent()) {
                Map<String, String> customConfig = keys.get().stream().collect(Collectors.toMap(i -> i,
                    j -> ConfigurationUtil.getInstance()
                        .get("kumuluzee.openapi-mp.ui.extensions.rapidoc."+j).get(), (oldValue, newValue) -> newValue));
                fullConfig.putAll(customConfig);
            }

            //Build the tag
            StringBuilder builder = new StringBuilder();
            builder.append("<rapi-doc ");

            fullConfig.entrySet().stream().forEach( i -> {
                builder.append(i.getKey());
                builder.append("=");
                builder.append("\"");
                builder.append(i.getValue());
                builder.append("\" ");
            });

            builder.append(">");

            index = index.replace("<rapi-doc>", builder.toString());
        }

        out.print(index);
    }
}
