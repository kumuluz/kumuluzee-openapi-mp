# KumuluzEE OpenAPI MicroProfile
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-openapi-mp/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-openapi-mp)

> KumuluzEE OpenAPI MicroProfile project provides powerful tools to incorporate the OpenAPI 3 specification to your
microservices in a standardized way.

KumuluzEE OpenAPI MicroProfile project allows you to document microservice APIs using OpenAPI v3 compliant annotations.
Project will automatically hook-up servlet that will serve your API specifications on endpoint `/openapi`.
The project implements the MicroProfile OpenAPI specification.
 
More details: 

- [OpenAPI v3 Specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md)
- [MicroProfile OpenAPI specification](https://github.com/eclipse/microprofile-open-api/releases)

## Usage

You can enable KumuluzEE OpenAPI MicroProfile support by adding the following dependency:
```xml
<dependency>
    <groupId>com.kumuluz.ee.openapi</groupId>
    <artifactId>kumuluzee-openapi-mp</artifactId>
    <version>${kumuluzee-openapi-mp.version}</version>
</dependency>
```

## OpenAPI configuration

When kumuluzee-openapi-mp dependency is included in the project, you can start documenting your REST API using
MicroProfile OpenAPI annotations.

### Documenting application class

```java
@SecurityScheme(securitySchemeName = "openid-connect", type = SecuritySchemeType.OPENIDCONNECT,
        openIdConnectUrl = "http://auth-server-url/.well-known/openid-configuration")
@ApplicationPath("v2")
@OpenAPIDefinition(info = @Info(title = "CustomerApi", version = "v2.0.0", contact = @Contact(), license = @License(name="something")), servers = @Server(url = "http://localhost:8080"), security
        = @SecurityRequirement(name = "openid-connect"))
public class CustomerApplication extends Application {
}
```

### Documenting resource class and operations

```java
@Path("customers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @GET
    @Operation(summary = "Get customers details", description = "Returns customer details.")
    @APIResponses({
            @APIResponse(description = "Customer details", responseCode = "200", content = @Content(schema = @Schema(implementation =
                    Customer.class)))
    })
    @Path("{customerId}")
    public Response getCustomer(@PathParam("customerId") String customerId) {
        // ...
    }

}
```

## Accessing API specification

Build and run project using:

```bash
mvn clean package
java -jar target/${project.build.finalName}.jar
```

After startup API specification will be available at:

**http://<-hostname->:<-port->/<-optional-context-path->/openapi**

Example:

http://localhost:8080/openapi

Serving OpenAPI specification can be disabled by setting property **kumuluzee.openapi-mp.enabled** to false. By default
serving API spec is enabled.

## Configuration

The KumuluzEE OpenAPI MicroProfile extension can be configured with the standard KumuluzEE configuration mechanism. For
example with the _config.yml_ file:

```yaml
kumuluzee:
  openapi-mp:
    enabled: true
    servlet:
      mapping: /openapi-custom-mapping
    scan:
      packages: com.kumuluz.ee.samples.openapi
    servers: https://example-api.com,https://my-proxy.com
```

Some interesting configuration properties are:

- `kumuluzee.openapi-mp.enabled` - If set to `false` disables the extension (and OpenAPI servlet). Default value: `true`
- `kumuluzee.openapi-mp.servlet.mapping` - The endpoint at which the OpenAPI specification is available. Appended to optional server context path. Default value: `/openapi`
- `kumuluzee.openapi-mp.scan.packages` - Comma separated list of packages which are scanned for the OpenAPI annotations.
  By default, all packages are scanned.

Full list of configuration properties can be found in
[MicroProfile OpenAPI specification](https://github.com/eclipse/microprofile-open-api/releases).

## Scanning

By default KumuluzEE OpenAPI MP uses optimized scanning in order to reduce startup times. This means that only the main
application JAR will be scanned (main artifact). In order to scan additional artifacts you need to specify them using
the [scan-libraries mechanism](https://github.com/kumuluz/kumuluzee/pull/123). You need to include all dependencies
which contain JAX-RS application and resources as well as dependencies containing models returned from JAX-RS resources.
If all your models and resources are in the main artifact you don't need to include anything. For example to include
_my-models_ artifact use the following configuration:

```yaml
kumuluzee:
  dev:
    scan-libraries:
      - my-models
```

If you are unsure if your configuration is correct you can try to disable optimized scanning by using the following
configuration:

```yaml
kumuluzee:
  openapi-mp:
    scanning:
      optimize: false
```

You can also enable scan debugging by setting the following key to `true`: `kumuluzee.openapi-mp.scanning.debug`. This
will output a verbose log of scanning configuration and progress.

## Adding Swagger UI

To serve API specification in visual form and to allow API consumers to interact with API resources you can add
Swagger UI by including the __kumuluzee-swagger-ui__ dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.openapi</groupId>
    <artifactId>kumuluzee-openapi-mp-ui</artifactId>
    <version>${kumuluzee-openapi-mp.version}</version>
</dependency>
```

Swagger UI is automatically enabled and is available at __/api-specs/ui__. In order to disable Swagger UI you can set
the configuration key `kumuluzee.openapi-mp.ui.enabled` to `false`. You can also remap the Swagger UI to another
location by setting the `kumuluzee.openapi-mp.ui.mapping` key (default value: `/api-specs/ui`). Path is appended to optional server context path.

Swagger UI needs to know where the OpenAPI specification is served from. It tries to define it from the following
sources:

1. Static: `<protocol>://localhost:<port>` (useful when nothing is defined, lowest priority)
1. From the `servers` parameter in `@OpenAPIDefinition` annotation (useful when OpenAPI specification is available from
   the same hostname as Swagger UI)
1. Configuration property: `kumuluzee.server.base-url` (useful for overriding above values)
1. Configuration property: `kumuluzee.openapi-mp.ui.specification-server` (same as above but in a namespace specific to
   this extension)
   
## Swagger UI server auto configuration

Server auto configuration allows for dynamic resolution of parameters like `kumuluzee.openapi-mp.ui.specification-server` 
instead of need to set it. This is done by retrieving necessary information from request during runtime and based on that 
setting correct redirect. It is especially useful when service is being deployed in multiple environments.
This feature also updates list of servers in Swagger UI and reorder them, so it will be first one.
It is also possible to resolve correct redirect when service is behind for example Ingres (Ingress must have enabled feature which 
forwards original URL).

By default, this feature is disabled for backward compatibility. To enable it set `kumuluzee.openapi-mp.ui.auto-config.enabled`
to `true`. To enable (Ingress) URI check set `kumuluzee.openapi-mp.ui.auto-config.original-uri-check` to `true`.

Default settings for server auto configuration:
```yaml
kumuluzee:
  openapi-mp:
    ui:
      server-auto-config:
        enabled: false
        original-uri-check: false
```

> **note:** localhost server with same port is added only once (if not already specified in @OpenAPIDefinition) and in case in which 
> is used localhost address in different format (for example http://127.0.0.1:8080 instead of http://localhost:8080), 
>no new server will be added to list and because of this Swagger UI will be unable to do API calls (this is due to issue with cors) 

## OpenAPI Maven Plugin

Maven plugin can be used to generate OpenAPI schema when building the application. To use it add the following to
pom.xml:

```
<plugin>
    <artifactId>kumuluzee-openapi-mp-maven-plugin</artifactId>
    <groupId>com.kumuluz.ee.openapi</groupId>
    <version>${kumuluzee-openapi-mp.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

By default, the schema is generated in `target/generated/` directory in JSON and YAML formats.

If the schema is defined across multiple artefacts you must define the `<scanLibraries></scanLibraries>` configuration
property with comma-separated list of dependencies in the same format as described in the [Scanning](#scanning) section
above.

Maven plugin uses SmallRye implementation in order to generate the schema. For more configuration options see the
following document: https://github.com/smallrye/smallrye-open-api/tree/master/tools/maven-plugin#configuration-options

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-openapi-mp/releases)


## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-openapi-mp/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-openapi-mp/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
