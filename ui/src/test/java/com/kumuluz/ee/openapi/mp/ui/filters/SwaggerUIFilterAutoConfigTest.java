package com.kumuluz.ee.openapi.mp.ui.filters;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.jetty.server.Request;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SwaggerUIFilterAutoConfigTest {

    private static final Optional<Boolean> TRUE_OPTIONAL = Optional.of(true);
    private static final Optional<Boolean> FALSE_OPTIONAL = Optional.of(false);
    private static final Optional<Boolean> DEFAULT_VALUE_OPTIONAL = Optional.empty();

    private static final String URL_QUERY = "/?url=";
    private static final String URL_OA_QUERY = "&oauth2RedirectUrl=";

    private static final String AUTO_CONFIG_SWITCH = "kumuluzee.openapi-mp.ui.server-auto-config.enabled";
    private static final String UPDATE_SERVERS_SWITCH = "kumuluzee.openapi-mp.ui.server-auto-config.update-servers";
    private static final String ORIGINAL_URI_CHECK_SWITCH = "kumuluzee.openapi-mp.ui.server-auto-config.original-uri-check";

    private static final String SPEC_URL_PARAMETER = "specUrl";
    private static final String UI_PATH_PARAMETER = "uiPath";
    private static final String OAUTH_REDIRECT_URL_PARAMETER = "oauth2RedirectUrl";
    private static final String SPEC_PATH_PARAMETER = "specPath";

    private static final String SERVER = "http://server.org";
    private static final String UI_PATH = "/default/ui";
    private static final String SPEC_URL = SERVER + "/oa-specification";
    private static final String SPEC_PATH = "/oa-specification";
    private static final String OA_HTML = "/oauth2-redirect.html";
    private static final String OA_REDIRECT_URL = SERVER + UI_PATH + OA_HTML;

    private static final String API_SERVER_A = "http://localhost:8080";
    private static final String API_SERVER_B = "http://kube-server:1234";

    private static final String HTTP_SERVER = "http://external-server.org:8090";
    private static final String XPATH = "/ingress-path";
    private static final String X_ORIGINAL_URI = "X-Original-URI";

    private static final String REQUEST_URI = HTTP_SERVER + UI_PATH;

    @Spy
    private SwaggerUIFilter swaggerUIFilterSpy;
    @Mock
    private ConfigurationUtil configurationUtilMock;
    @Mock
    private OpenAPI openAPIMock;
    @Mock
    private FilterConfig filterConfigMock;
    @Mock
    private FilterChain filterChainMock;
    @Mock
    private HttpServletResponse servletResponseMock;
    @Mock
    private Request servletRequestMock;

    private List<Server> serverList;
    private String redirectedUrl;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(configurationUtilMock).when(swaggerUIFilterSpy).getConfigUtil();
        doReturn(openAPIMock).when(swaggerUIFilterSpy).getOpenAPI();
        setupConfigMocks();
        setupServerList();
        setupServletRequestMock();
        setupServletResponseMock();
    }

    @Test
    public void redirectingWhileAutoConfigIsDisabled() throws Exception {
        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        verify(swaggerUIFilterSpy).getConfigUtil();
        assertThat(redirectedUrl)
                .isEqualTo(UI_PATH + URL_QUERY + SPEC_URL + URL_OA_QUERY + OA_REDIRECT_URL);

        verify(servletRequestMock, never()).getRootURL();
        verify(swaggerUIFilterSpy, never()).getOpenAPI();
        verify(filterChainMock, never()).doFilter(servletRequestMock, servletResponseMock);
    }

    @Test
    public void redirectWithStaticConfigUponDynamicResolutionFailure() throws Exception {
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        doThrow(new RuntimeException()).when(servletRequestMock).getRootURL();

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        verify(servletRequestMock).getRootURL();
        assertThat(redirectedUrl).isEqualTo(UI_PATH + URL_QUERY + SPEC_URL + URL_OA_QUERY + OA_REDIRECT_URL);
    }

    @Test
    public void redirectingWhileAutoConfigIsEnabled() throws Exception {
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        verify(swaggerUIFilterSpy).getConfigUtil();
        assertThat(redirectedUrl)
                .isEqualTo(UI_PATH + URL_QUERY + HTTP_SERVER + SPEC_PATH + URL_OA_QUERY + HTTP_SERVER + UI_PATH + OA_HTML);

        verify(servletRequestMock).getRootURL();
        verify(swaggerUIFilterSpy).getOpenAPI();
        verify(filterChainMock, never()).doFilter(servletRequestMock, servletResponseMock);
        assertThat(getStringServerList()).containsExactly(HTTP_SERVER, API_SERVER_A, API_SERVER_B);
    }

    @DataProvider(name = "localHost")
    public Object[][] localHostDataProvider() {
        return new Object[][]{{"http://localhost:8080"}, {"http://127.0.0.1:8080"}, {"http://[::1]:8080"}};
    }

    @Test(dataProvider = "localHost")
    public void redirectingWhenDynamicServerIsLoopbackServerAndIsAlreadyInServerList(
            String localHost) throws Exception {
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        doReturn(new StringBuilder(localHost)).when(servletRequestMock).getRootURL();

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        assertThat(redirectedUrl)
                .isEqualTo(UI_PATH + URL_QUERY + localHost + SPEC_PATH + URL_OA_QUERY + localHost + UI_PATH + OA_HTML);

        assertThat(getStringServerList()).containsExactly(API_SERVER_A, API_SERVER_B);
    }

    @Test
    public void redirectingWhenServerListContainsLoopbackServerWithDifferentPort() throws Exception {
        String testLocalhost = "http://localhost:8081";

        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        doReturn(new StringBuilder(testLocalhost)).when(servletRequestMock).getRootURL();

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        assertThat(redirectedUrl).isEqualTo(UI_PATH + URL_QUERY + testLocalhost + SPEC_PATH + URL_OA_QUERY +
                testLocalhost + UI_PATH + OA_HTML);
        assertThat(getStringServerList()).containsExactly(testLocalhost, API_SERVER_A, API_SERVER_B);
    }

    @Test
    public void redirectingWhenDynamicServerIsLoopbackServerAndIsNotInServerList() throws Exception {
        String localHost = "http://[::1]:8080";
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        doReturn(new StringBuilder(localHost)).when(servletRequestMock).getRootURL();
        serverList.remove(0);

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        assertThat(redirectedUrl)
                .isEqualTo(UI_PATH + URL_QUERY + localHost + SPEC_PATH + URL_OA_QUERY + localHost + UI_PATH + OA_HTML);

        assertThat(getStringServerList()).containsExactly(localHost, API_SERVER_B);
    }

    @DataProvider(name = "xPath")
    public Object[][] xPathProvider() {
        return new Object[][]{{XPATH + UI_PATH + "/random-path"}, {XPATH + UI_PATH + "//"}, {XPATH + UI_PATH}};
    }

    @Test(dataProvider = "xPath")
    public void redirectingWithPresentXPath(String xPath) throws Exception {
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(ORIGINAL_URI_CHECK_SWITCH);
        doReturn(xPath).when(servletRequestMock).getHeader(X_ORIGINAL_URI);

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        assertThat(redirectedUrl)
                .isEqualTo(XPATH + UI_PATH + URL_QUERY + HTTP_SERVER + XPATH + SPEC_PATH + URL_OA_QUERY + HTTP_SERVER + XPATH + UI_PATH + OA_HTML);
        assertThat(getStringServerList()).containsExactly(HTTP_SERVER + XPATH, API_SERVER_A, API_SERVER_B);
    }

    @Test
    public void redirectingWithPresentXPathWhichIsDisabledInConfig() throws Exception {
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        doReturn(FALSE_OPTIONAL).when(configurationUtilMock).getBoolean(ORIGINAL_URI_CHECK_SWITCH);
        doReturn(XPATH).when(servletRequestMock).getHeader(X_ORIGINAL_URI);

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        assertThat(redirectedUrl)
                .isEqualTo(UI_PATH + URL_QUERY + HTTP_SERVER + SPEC_PATH + URL_OA_QUERY + HTTP_SERVER + UI_PATH + OA_HTML);
        assertThat(getStringServerList()).containsExactly(HTTP_SERVER, API_SERVER_A, API_SERVER_B);
    }

    @Test
    public void dynamicServerIsAlreadyInListOnBottom() throws Exception {
        doReturn(TRUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        serverList.add(new CustomServer(HTTP_SERVER));

        swaggerUIFilterSpy.init(filterConfigMock);
        swaggerUIFilterSpy.doFilter(servletRequestMock, servletResponseMock, filterChainMock);

        assertThat(getStringServerList()).containsExactly(HTTP_SERVER, API_SERVER_A, API_SERVER_B);
    }

    // mocks setup
    private void setupServletRequestMock() {
        doReturn(REQUEST_URI).when(servletRequestMock).getRequestURI();
        doReturn(UI_PATH).when(servletRequestMock).getServletPath();
        doReturn(new StringBuilder(HTTP_SERVER)).when(servletRequestMock).getRootURL();
        doReturn("").when(servletRequestMock).getContextPath();
    }

    private void setupServletResponseMock() throws Exception {
        doAnswer(invocation -> {
            redirectedUrl = invocation.getArgument(0);
            return null;
        }).when(servletResponseMock).sendRedirect(anyString());
    }

    private void setupConfigMocks() {
        doReturn(DEFAULT_VALUE_OPTIONAL).when(configurationUtilMock).getBoolean(AUTO_CONFIG_SWITCH);
        doReturn(DEFAULT_VALUE_OPTIONAL).when(configurationUtilMock).getBoolean(ORIGINAL_URI_CHECK_SWITCH);

        doReturn(UI_PATH).when(filterConfigMock).getInitParameter(UI_PATH_PARAMETER);
        doReturn(OA_REDIRECT_URL).when(filterConfigMock).getInitParameter(OAUTH_REDIRECT_URL_PARAMETER);
        doReturn(SPEC_PATH).when(filterConfigMock).getInitParameter(SPEC_PATH_PARAMETER);
        doReturn(SPEC_URL).when(filterConfigMock).getInitParameter(SPEC_URL_PARAMETER);
    }

    private void setupServerList() {
        serverList = new ArrayList<>();
        serverList.add(new CustomServer(API_SERVER_A));
        serverList.add(new CustomServer(API_SERVER_B));
        doReturn(serverList).when(openAPIMock).getServers();
        doAnswer(invocationOnMock -> {
            serverList = invocationOnMock.getArgument(0);
            return null;
        }).when(openAPIMock).setServers(anyList());
    }

    private List<String> getStringServerList() {
        return serverList.stream().map(Server::getUrl).collect(Collectors.toList());
    }
}