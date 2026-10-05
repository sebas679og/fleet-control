package com.fleet.control.alerts.config;

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
            title = "FleetControl Alerts API",
            description =
                """
                REST API for fleet alerts and their evaluation rules.
                A scheduled evaluator (every 15 minutes) raises alerts for upcoming or
                overdue maintenance, \
                licenses about to expire or expired, and abnormal fuel consumption,
                deduplicating them with a \
                unique key. Alerts move from `OPEN` to `ACKNOWLEDGED` or `RESOLVED`.
                Editing rules and triggering manual evaluations is reserved for `ADMIN`.
                """,
            version = "1.0",
            contact = @Contact(name = "FleetControl Team")),
    servers = {
      @Server(url = "http://localhost:8080", description = "API Gateway (recommended entry point)"),
      @Server(
          url = "http://localhost:8088",
          description = "ms-alerts (direct access, development only)")
    },
    security = @SecurityRequirement(name = "BearerAuth"),
    tags = {
      @Tag(name = "Alerts", description = "Alert listing and acknowledgement"),
      @Tag(name = "Alert rules", description = "Evaluation rules configuration (ADMIN)"),
      @Tag(name = "Alert evaluation", description = "Manual evaluation of the rules (ADMIN)"),
      @Tag(name = "Alert statistics", description = "Time series of created alerts")
    })
@SecurityScheme(
    name = "BearerAuth",
    description =
        """
                JWT issued by `ms-auth` (HS256, valid for 1 hour)
                and sent in the `Authorization` header \
                using the Bearer scheme: `Authorization: Bearer <token>`.
                The token carries the claims `sub` (user id),
                `username` and `role` (`MANAGER` or `ADMIN`).
                Obtain it with `POST /api/auth/login`.
                """,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@SecurityScheme(
    name = "InternalApiKey",
    description =
        """
                Shared key for service-to-service calls and
                scheduled tasks, sent in the `X-Internal-Key` \
                header (value of `INTERNAL_API_KEY`). A call with a
                valid key is handled with `MANAGER` \
                permissions. It only applies to endpoints marked as
                internal calls. `ms-gateway` removes \
                this header from every external request.
                """,
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-Internal-Key")
public class OpenApiConfig {}
