package com.fleet.control.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(
    info =
        @Info(
            title = "FleetControl API Gateway",
            description =
                """
        Single entry point to the FleetControl platform.
        Routes every `/api/**` request to the right microservice, applies global rate limiting \
        (60 requests per minute per IP), generates the `X-Request-Id` header when it is missing and \
        removes `X-Internal-Key` from external requests.
        The gateway does not validate JWT tokens: each downstream service validates them on its own.
        Routing: `/api/auth` → ms-auth, `/api/vehicles` → ms-vehicles, `/api/drivers` → ms-drivers, \
        `/api/routes` → ms-routes, `/api/maintenance` → ms-maintenance, `/api/fuel` → ms-fuel, \
        `/api/alerts` → ms-alerts, `/api/dashboard` → ms-dashboard.
        """,
            version = "1.0",
            contact = @Contact(name = "FleetControl Team")),
    servers = {
        @Server(url = "http://localhost:8080", description = "API Gateway (local)")
    },
    tags = {
        @Tag(name = "Health", description = "Gateway status and health of the downstream services")
    })
@SecurityScheme(
    name = "BearerAuth",
    description =
        """
        JWT issued by `ms-auth` (HS256, valid for 1 hour) and sent in the `Authorization` header \
        using the Bearer scheme: `Authorization: Bearer <token>`.
        The token carries the claims `sub` (user id), `username` and `role` (`MANAGER` or `ADMIN`).
        Obtain it with `POST /api/auth/login`.
        """,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {}
