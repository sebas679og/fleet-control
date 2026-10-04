package com.fleet.control.maintenance.config;

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
            title = "FleetControl Maintenance API",
            description =
                """
                REST API for scheduled maintenance.
                Manages recurring plans per vehicle (by kilometers,
                by days or both) and the work orders \
                derived from them. A daily task (06:00 UTC) creates
                `PENDING` orders when a due date is 7 days \
                away or less, or the odometer is within 500 km of the next due mileage.
                Starting an order puts the vehicle `IN_MAINTENANCE`;
                completing it records the cost and \
                recalculates the plan.
                """,
            version = "1.0",
            contact = @Contact(name = "FleetControl Team")),
    servers = {
      @Server(url = "http://localhost:8080", description = "API Gateway (recommended entry point)"),
      @Server(
          url = "http://localhost:8086",
          description = "ms-maintenance (direct access, development only)")
    },
    security = @SecurityRequirement(name = "BearerAuth"),
    tags = {
      @Tag(name = "Maintenance plans", description = "Recurring maintenance plans per vehicle"),
      @Tag(name = "Maintenance orders", description = "Work orders: listing, start and completion"),
      @Tag(
          name = "Maintenance statistics",
          description = "Time series of completed orders and costs")
    })
@SecurityScheme(
    name = "BearerAuth",
    description =
        """
                JWT issued by `ms-auth` (HS256, valid for 1 hour) and sent in
                the `Authorization` header \
                using the Bearer scheme: `Authorization: Bearer <token>`.
                The token carries the claims `sub` (user id), `username`
                and `role` (`MANAGER` or `ADMIN`).
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
                permissions. It only applies to endpoints marked as internal calls.
                `ms-gateway` removes \
                this header from every external request.
                """,
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-Internal-Key")
public class OpenApiConfig {}
