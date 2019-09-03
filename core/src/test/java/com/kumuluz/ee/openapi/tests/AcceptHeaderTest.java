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
package com.kumuluz.ee.openapi.tests;

import io.restassured.RestAssured;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

/**
 * Test Accept header on /openapi endpoint.
 *
 * @author Urban Malc
 * @since 1.2.0
 */
public class AcceptHeaderTest extends Arquillian {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addAsManifestResource("test-openapi.yml", "openapi.yml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeClass
    public void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost:9080";
    }

    private Matcher<String> isYaml() {
        return matchesPattern(Pattern.compile("\\p{all}*openapi: 3\\.\\d+\\.\\d+\\p{all}*"));
    }

    @Test
    @RunAsClient
    public void acceptNoHeaderTest() {
        given()
                .noFilters()
        .when()
                .get("/openapi")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(isYaml());
    }

    @Test
    @RunAsClient
    public void acceptYamlTest() {
        given()
                .noFilters()
                .header("Accept", "text/yaml")
        .when()
                .get("/openapi")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(isYaml());
    }

    @Test
    @RunAsClient
    public void acceptJsonTest() {
        given()
                .noFilters()
                .header("Accept", MediaType.APPLICATION_JSON)
        .when()
                .get("/openapi")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("openapi", matchesPattern("3\\.\\d+\\.\\d+"));
    }

    @Test
    @RunAsClient
    public void acceptJsonFormatOverrideYamlTest() {
        given()
                .noFilters()
                .header("Accept", MediaType.APPLICATION_JSON)
        .when()
                .get("/openapi?format=yaml")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(isYaml());
    }

    @Test
    @RunAsClient
    public void acceptYamlFormatOverrideJsonTest() {
        given()
                .noFilters()
                .header("Accept", "text/yaml")
        .when()
                .get("/openapi?format=json")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("openapi", matchesPattern("3\\.\\d+\\.\\d+"));
    }
}
