package com.fleet.control.auth.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleet.control.auth.common.exceptions.DuplicateEmailException;
import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.common.exceptions.ErrorMessages;
import com.fleet.control.auth.common.exceptions.GlobalExceptionHandler;
import com.fleet.control.auth.common.exceptions.InvalidCredentialsException;
import com.fleet.control.auth.common.exceptions.TokenInvalidException;
import com.fleet.control.auth.common.path.ApiPaths;
import com.fleet.control.auth.controller.impl.AuthApiController;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Edge case tests for the authentication endpoints through the error contract. */
@ExtendWith(MockitoExtension.class)
class AuthApiControllerTest {

  @Mock private AuthService authService;

  @InjectMocks private AuthApiController controller;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
            .build();
  }

  @Test
  void registerWithSpacedUsernameReturns400() throws Exception {
    var request =
        RegisterRequest.builder()
            .username("john doe")
            .email("john.doe@example.com")
            .password("StrongPass123!")
            .build();

    mockMvc
        .perform(
            post(ApiPaths.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCodes.VALIDATION_ERROR))
        .andExpect(jsonPath("$.details[0].field").value("username"));
  }

  @Test
  void registerWithWeakPasswordReturns400() throws Exception {
    var request =
        RegisterRequest.builder()
            .username("john_doe")
            .email("john.doe@example.com")
            .password("weakpass1")
            .build();

    mockMvc
        .perform(
            post(ApiPaths.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCodes.VALIDATION_ERROR))
        .andExpect(jsonPath("$.details[0].field").value("password"));
  }

  @Test
  void registerWithDuplicateEmailReturns409() throws Exception {
    var request =
        RegisterRequest.builder()
            .username("john_doe")
            .email("john.doe@example.com")
            .password("StrongPass123!")
            .build();
    given(authService.createUser(any(RegisterRequest.class)))
        .willThrow(new DuplicateEmailException(ErrorMessages.USER_ALREADY_EXISTS));

    mockMvc
        .perform(
            post(ApiPaths.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCodes.USER_ALREADY_EXISTS));
  }

  @Test
  void loginWithEmptyBodyReturns400() throws Exception {
    var request = LoginRequest.builder().email("").password("").build();

    mockMvc
        .perform(
            post(ApiPaths.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCodes.VALIDATION_ERROR));
  }

  @Test
  void loginWithInvalidCredentialsReturns401() throws Exception {
    var request =
        LoginRequest.builder().email("john.doe@example.com").password("WrongPass123!").build();
    given(authService.login(any(LoginRequest.class)))
        .willThrow(new InvalidCredentialsException(ErrorMessages.INVALID_CREDENTIALS));

    mockMvc
        .perform(
            post(ApiPaths.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value(ErrorCodes.INVALID_CREDENTIALS));
  }

  @Test
  void validateWithoutHeaderReturns401() throws Exception {
    mockMvc
        .perform(post(ApiPaths.VALIDATE_ENDPOINT))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value(ErrorCodes.UNAUTHORIZED));
  }

  @Test
  void validateWithInvalidTokenReturns401Payload() throws Exception {
    given(authService.validateToken("Bearer bad-token"))
        .willThrow(
            new TokenInvalidException(ErrorCodes.INVALID_TOKEN, ErrorMessages.INVALID_TOKEN));

    mockMvc
        .perform(
            post(ApiPaths.VALIDATE_ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.error").value(ErrorCodes.INVALID_TOKEN));
  }
}
