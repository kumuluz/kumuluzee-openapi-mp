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
@OpenAPIDefinition(info = @Info(title = "CustomerApi", version = "v2.0.0", contact = @Contact(), license = @License(name="something")), servers = @Server(url = "http://localhost:8080/v2"), security
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

**http://<-hostname-:<-port->/openapi**

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
    servers: https://example-api.com/v1,https://my-proxy.com/v1
```

Some interesting configuration properties are:

- `kumuluzee.openapi-mp.enabled` - If set to `false` disables the extension (and OpenAPI servlet). Default value: `true`
- `kumuluzee.openapi-mp.servlet.mapping` - The endpoint at which the OpenAPI specification is available. Default value: `/openapi`
- `kumuluzee.openapi-mp.scan.packages` - Comma separated list of packages which are scanned for the OpenAPI annotations.
  By default, all packages are scanned.

Full list of configuration properties can be found in
[MicroProfile OpenAPI specification](https://github.com/eclipse/microprofile-open-api/releases).

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
