package com.fleet.control.auth.config;

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
            title = "FleetControl Authentication API",
            description =
                """
                REST API for user registration and authentication.
                Issues JWT tokens (HS256, valid for 1 hour)
                with the claims `sub`, `username` and `role`, \
                which every domain microservice validates
                on each protected request.
                Roles: `MANAGER` (assigned on registration) and `
                ADMIN` (created by the demo seeder).
                Registration and login are public; token validation
                is meant for internal use.
                """,
            version = "1.0",
            contact = @Contact(name = "FleetControl Team")),
    servers = {
      @Server(url = "http://localhost:8080", description = "API Gateway (recommended entry point)"),
      @Server(
          url = "http://localhost:8081",
          description = "ms-auth (direct access, development only)")
    },
    security = @SecurityRequirement(name = "BearerAuth"),
    tags = {
      @Tag(name = "Authentication", description = "User registration, login and token validation")
    })
@SecurityScheme(
    name = "BearerAuth",
    description =
        """
                JWT issued by `ms-auth` (HS256, valid for 1 hour)
                and sent in the `Authorization` header \
                using the Bearer scheme: `Authorization:
                Bearer <token>`.
                The token carries the claims `sub` (user id),
                `username` and `role` (`MANAGER` or `ADMIN`).
                Obtain it with `POST /api/auth/login`.
                """,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {}
