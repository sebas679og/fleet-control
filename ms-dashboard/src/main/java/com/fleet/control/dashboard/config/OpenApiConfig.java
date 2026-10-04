package com.fleet.control.dashboard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
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
            title = "FleetControl Dashboard API",
            description =
                """
                REST API with the executive fleet summary and time series
                ready to be plotted.
                Aggregates data from ms-vehicles, ms-routes, ms-fuel,
                ms-maintenance and ms-alerts, calling \
                them in parallel and caching responses for 60 seconds. I
                f a source service is down, it returns the \
                available data with `partial` set to `true` and the list of
                unavailable services.
                """,
            version = "1.0",
            contact = @Contact(name = "FleetControl Team")),
    servers = {
      @Server(url = "http://localhost:8080", description = "API Gateway (recommended entry point)"),
      @Server(
          url = "http://localhost:8089",
          description = "ms-dashboard (direct access, development only)")
    },
    security = @SecurityRequirement(name = "BearerAuth"),
    tags = {
      @Tag(name = "Dashboard", description = "Fleet summary, time series and vehicle rankings")
    })
@SecurityScheme(
    name = "BearerAuth",
    description =
        """
                JWT issued by `ms-auth` (HS256, valid for 1 hour) and
                sent in the `Authorization` header \
                using the Bearer scheme: `Authorization: Bearer <token>`.
                The token carries the claims `sub` (user id),
                `username` and `role` (`MANAGER` or `ADMIN`).
                Obtain it with `POST /api/auth/login`.
                """,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {}
