package com.fleet.control.fuel.config;

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
                title = "FleetControl Fuel API",
                description =
                        """
                REST API for refuels and fuel consumption.
                Records refuels, takes the fuel type and tank capacity from the vehicle and computes \
                consumption in L/100 km between consecutive full-tank refuels, adding the partial refuels in between.
                It is the origin of the liters, cost and consumption metrics used by the dashboard and \
                the alert evaluator.
                """,
                version = "1.0",
                contact = @Contact(name = "FleetControl Team")),
        servers = {
                @Server(url = "http://localhost:8080", description = "API Gateway (recommended entry point)"),
                @Server(
                        url = "http://localhost:8087",
                        description = "ms-fuel (direct access, development only)")
        },
        security = @SecurityRequirement(name = "BearerAuth"),
        tags = {
                @Tag(name = "Refuels", description = "Refuel registration and history"),
                @Tag(name = "Fuel consumption", description = "Aggregated consumption per vehicle"),
                @Tag(name = "Fuel statistics", description = "Time series of liters, cost and consumption")
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