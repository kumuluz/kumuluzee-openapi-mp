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
package com.kumuluz.ee.openapi.mp.ui.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Configures default parameters for Swagger UI when coming from root URL.
 *
 * @author Zvone Gazvoda
 * @author Urban Malc
 * @since 1.1.0
 */
public class SwaggerUIFilter implements Filter {

    private String specUrl;
    private String uiPath;
    private String oauth2RedirectUrl;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.specUrl = filterConfig.getInitParameter("specUrl");
        this.uiPath = filterConfig.getInitParameter("uiPath");
        this.oauth2RedirectUrl = filterConfig.getInitParameter("oauth2RedirectUrl");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        String path = httpServletRequest.getServletPath();

        // check if request is for UI
        if (path.contains(uiPath)) {
            // match static files and urls with existing parameter url=... set
            Pattern staticFiles = Pattern.compile("(\\.css|\\.js|\\.html|url=)");
            String requestQueryString = httpServletRequest.getRequestURI();
            if (httpServletRequest.getQueryString() != null) {
                requestQueryString += httpServletRequest.getQueryString();
            }
            if (!staticFiles.matcher(requestQueryString).find()) {
                // not a static file, redirect to appropriate url
                httpServletResponse.sendRedirect(uiPath +
                        "/?url=" + specUrl +
                        "&oauth2RedirectUrl=" + oauth2RedirectUrl);
            } else {
                // static file, leave as is
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            }

        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }
}