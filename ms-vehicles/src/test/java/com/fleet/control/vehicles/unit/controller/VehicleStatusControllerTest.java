package com.fleet.control.vehicles.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleet.control.vehicles.common.constants.ApiPathConstants;
import com.fleet.control.vehicles.common.exceptions.ApiVehicleException;
import com.fleet.control.vehicles.common.exceptions.GlobalExceptionHandler;
import com.fleet.control.vehicles.controller.impl.VehicleApiController;
import com.fleet.control.vehicles.models.dtos.request.UpdateStatusRequest;
import com.fleet.control.vehicles.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.models.enums.FuelType;
import com.fleet.control.vehicles.models.enums.VehicleStatus;
import com.fleet.control.vehicles.models.enums.VehicleType;
import com.fleet.control.vehicles.service.VehicleService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * HTTP contract tests for the vehicle status user story: 200 update, 400 validation, 403 forbidden,
 * 404 not found, 409 transition and 409 odometer.
 */
@ExtendWith(MockitoExtension.class)
class VehicleStatusControllerTest {

  @Mock private VehicleService vehicleService;

  @InjectMocks private VehicleApiController controller;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final int VALID_YEAR = LocalDate.now().getYear();

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

  private VehicleResponse vehicleResponse(UUID id, VehicleStatus status, int odometerKm) {
    return new VehicleResponse(
        id,
        "4821-KDF",
        "Ford",
        "Transit",
        VALID_YEAR,
        VehicleType.VAN,
        FuelType.DIESEL,
        80,
        odometerKm,
        status,
        null,
        null);
  }

  private String statusUrl(UUID id) {
    return ApiPathConstants.VEHICLE_BASE_PATH + "/" + id + "/status";
  }

  @Test
  void updateStatusReturns200() throws Exception {
    UUID id = UUID.randomUUID();
    var request = new UpdateStatusRequest(VehicleStatus.IN_USE, 45310);
    given(vehicleService.updateStatus(eq(id), any(UpdateStatusRequest.class)))
        .willReturn(vehicleResponse(id, VehicleStatus.IN_USE, 45310));

    mockMvc
        .perform(
            patch(statusUrl(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_USE"))
        .andExpect(jsonPath("$.odometerKm").value(45310));

    verify(vehicleService).updateStatus(eq(id), any(UpdateStatusRequest.class));
  }

  @Test
  void nullStatusReturns400WithDetails() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(
            patch(statusUrl(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"odometerKm\":45310}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("status"));
  }

  @Test
  void negativeOdometerReturns400WithDetails() throws Exception {
    UUID id = UUID.randomUUID();
    var request = new UpdateStatusRequest(VehicleStatus.IN_USE, -5);

    mockMvc
        .perform(
            patch(statusUrl(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("odometerKm"));
  }

  @Test
  void unknownVehicleReturns404() throws Exception {
    UUID id = UUID.randomUUID();
    given(vehicleService.updateStatus(eq(id), any(UpdateStatusRequest.class)))
        .willThrow(new ApiVehicleException.VehicleNotFoundException(id));

    mockMvc
        .perform(
            patch(statusUrl(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateStatusRequest(VehicleStatus.IN_USE, null))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("VEHICLE_NOT_FOUND"));
  }

  @Test
  void disallowedTransitionReturns409() throws Exception {
    UUID id = UUID.randomUUID();
    given(vehicleService.updateStatus(eq(id), any(UpdateStatusRequest.class)))
        .willThrow(
            new ApiVehicleException.InvalidStatusTransitionException("IN_USE", "IN_MAINTENANCE"));

    mockMvc
        .perform(
            patch(statusUrl(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateStatusRequest(VehicleStatus.IN_MAINTENANCE, null))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
  }

  @Test
  void lowerOdometerReturns409() throws Exception {
    UUID id = UUID.randomUUID();
    given(vehicleService.updateStatus(eq(id), any(UpdateStatusRequest.class)))
        .willThrow(new ApiVehicleException.InvalidOdometerException(45209, 45210));

    mockMvc
        .perform(
            patch(statusUrl(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateStatusRequest(VehicleStatus.IN_USE, 45209))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("INVALID_ODOMETER"));
  }

  @Test
  void managerReactivationReturns403() throws Exception {
    UUID id = UUID.randomUUID();
    given(vehicleService.updateStatus(eq(id), any(UpdateStatusRequest.class)))
        .willThrow(new AccessDeniedException("Only ADMIN can reactivate a vehicle"));

    mockMvc
        .perform(
            patch(statusUrl(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateStatusRequest(VehicleStatus.AVAILABLE, null))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }
}
