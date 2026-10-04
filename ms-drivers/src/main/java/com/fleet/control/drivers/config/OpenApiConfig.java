package com.fleet.control.drivers.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(
        info =
        @Info(
                title = "FleetControl Drivers API",
                description =
                        """
                REST API for fleet drivers and their driving licenses.
                Tracks license number, category (`A`, `B`, `C`), expiration date and driver status \
                (`ACTIVE`, `ON_LEAVE`, `SUSPENDED`). ms-routes uses it to check driver eligibility and ms-alerts \
                to detect licenses that are about to expire.
                """,
                version = "1.0",
                contact = @Contact(name = "FleetControl Team")),
        servers = {
                @Server(url = "http://localhost:8080", description = "API Gateway (recommended entry point)"),
                @Server(
                        url = "http://localhost:8083",
                        description = "ms-drivers (direct access, development only)")
        },
        security = @SecurityRequirement(name = "BearerAuth"),
        tags = {
                @Tag(name = "Drivers", description = "Driver registration, license data and status")
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
@SecurityScheme(
        name = "InternalApiKey",
        description =
                """
                Shared key for service-to-service calls and scheduled tasks, sent in the `X-Internal-Key` \
                header (value of `INTERNAL_API_KEY`). A call with a valid key is handled with `MANAGER` \
                permissions. It only applies to endpoints marked as internal calls. `ms-gateway` removes \
                this header from every external request.
                """,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Internal-Key")
public class OpenApiConfig {}