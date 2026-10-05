package com.fleet.control.routes.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Configuration class for OpenAPI (Swagger) documentation. */
@OpenAPIDefinition(
    info =
        @Info(
            title = "FleetControl Routes API",
            description =
                """
                REST API for route planning, execution and history.
                Validates vehicle availability, driver eligibility (status,
                license validity and category) \
                and schedule overlaps by calling ms-vehicles and ms-drivers. Starting and
                completing a route updates \
                the vehicle status and odometer.
                It is the origin of the distance traveled metric used by the dashboard.
                """,
            version = "1.0",
            contact = @Contact(name = "FleetControl Team")),
    servers = {
      @Server(url = "http://localhost:8080", description = "API Gateway (recommended entry point)"),
      @Server(
          url = "http://localhost:8085",
          description = "ms-routes (direct access, development only)")
    },
    security = @SecurityRequirement(name = "BearerAuth"),
    tags = {
      @Tag(name = "Routes", description = "Route planning, start, completion and history"),
      @Tag(name = "Route statistics", description = "Time series of completed routes")
    })
@SecurityScheme(
    name = "BearerAuth",
    description =
        """
                JWT issued by `ms-auth` (HS256, valid for 1 hour) and sent
                in the `Authorization` header \
                using the Bearer scheme: `Authorization: Bearer <token>`.
                The token carries the claims `sub` (user id), `username` and
                `role` (`MANAGER` or `ADMIN`).
                Obtain it with `POST /api/auth/login`.
                """,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@SecurityScheme(
    name = "InternalApiKey",
    description =
        """
                Shared key for service-to-service calls and scheduled tasks,
                sent in the `X-Internal-Key` \
                header (value of `INTERNAL_API_KEY`). A call with a valid key is
                handled with `MANAGER` \
                permissions. It only applies to endpoints marked as internal
                 calls. `ms-gateway` removes \
                this header from every external request.
                """,
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-Internal-Key")
public class OpenApiConfig {}
