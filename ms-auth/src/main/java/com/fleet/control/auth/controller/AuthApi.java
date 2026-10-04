package com.fleet.control.auth.controller;

import com.fleet.control.auth.common.constants.HeaderConstants;
import com.fleet.control.auth.common.path.ApiPaths;
import com.fleet.control.auth.models.dto.exceptions.response.ErrorResponse;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(ApiPaths.AUTH_BASE)
@Tag(name = "Auth", description = "Operaciones de autenticación y gestión de usuarios")
public interface AuthApi {

  @Operation(
      summary = "Registro de usuario",
      description =
          "Crea un usuario nuevo en el sistema. Valida los datos, "
              + "verifica que el email no esté registrado, encripta la contraseña con BCrypt, "
              + "guarda en la base de datos y devuelve los datos del usuario creado.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Usuario creado exitosamente",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o la contraseña no cumple los requisitos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "El correo electrónico ya está registrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping(ApiPaths.REGISTER)
  ResponseEntity<RegisterResponse> createUser(@Valid @RequestBody RegisterRequest registerRequest);

  @Operation(
      summary = "Inicio de sesión",
      description =
          "Autentica un usuario existente usando email y contraseña. "
              + "Spring Security verifica las credenciales contra la base de datos "
              + "y si son correctas, genera un token JWT para usar en adelante.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Inicio de sesión exitoso",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o faltantes",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas (email o contraseña incorrectos)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping(ApiPaths.LOGIN)
  ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest);

  @Operation(
      summary = "Validar token JWT",
      description =
          "Verifica si un token JWT es válido y no ha expirado.\n\n"
              + "¿Cómo usar este endpoint?\n"
              + "1. Obtén un token haciendo login en POST /api/auth/login\n"
              + "2. Haz clic en el botón Authorize (arriba a la derecha) y pega el token en el formato: Bearer <token>\n"
              + "3. Ahora el header Authorization se enviará automáticamente en este endpoint")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token válido — devuelve los datos del usuario (userId, username, role)",
            content = @Content(schema = @Schema(implementation = TokenPayload.class))),
        @ApiResponse(
            responseCode = "401",
            description =
                "Token inválido, expirado o con firma incorrecta — devuelve el error (TOKEN_EXPIRED / INVALID_TOKEN)",
            content = @Content(schema = @Schema(implementation = TokenPayload.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Header Authorization faltante (es requerido)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  @PostMapping(ApiPaths.VALIDATE)
  ResponseEntity<TokenPayload> validateToken(
      @RequestHeader(HeaderConstants.AUTHORIZATION) String authHeader);
}
