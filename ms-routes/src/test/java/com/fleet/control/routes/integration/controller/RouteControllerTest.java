package com.fleet.control.routes.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleet.control.routes.commons.constants.ApiPathConstants;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import com.fleet.control.routes.commons.exception.exception.DriverNotEligibleException;
import com.fleet.control.routes.commons.exception.exception.InvalidRouteStateException;
import com.fleet.control.routes.commons.exception.exception.RouteNotFoundException;
import com.fleet.control.routes.commons.exception.exception.RouteOverlapException;
import com.fleet.control.routes.commons.exception.exception.ServiceUnavailableException;
import com.fleet.control.routes.commons.exception.exception.VehicleNotAvailableException;
import com.fleet.control.routes.config.SecurityErrorHandler;
import com.fleet.control.routes.controller.impl.RouteController;
import com.fleet.control.routes.models.dtos.request.CompleteRouteRequest;
import com.fleet.control.routes.models.dtos.request.CreateRouteRequest;
import com.fleet.control.routes.models.dtos.response.RouteResponse;
import com.fleet.control.routes.models.enums.RouteStatus;
import com.fleet.control.routes.service.RouteService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP edge cases for route planning: every failure maps to its status and machine-readable error
 * code (409 unavailable/ineligible/overlap, 503 downstream down, 404 missing route, 400 invalid
 * input with details).
 */
@WebMvcTest(RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityErrorHandler.class)
class RouteControllerTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @MockitoBean private RouteService routeService;

  private Map<String, Object> validBody(UUID vehicleId, UUID driverId) {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicleId.toString());
    body.put("driverId", driverId.toString());
    body.put("origin", "Madrid");
    body.put("destination", "Barcelona");
    body.put("plannedStart", Instant.now().plus(2, ChronoUnit.DAYS).toString());
    body.put("estimatedDurationMin", 240);
    body.put("plannedDistanceKm", new BigDecimal("620.50"));
    return body;
  }

  @Test
  void postUnavailableVehicleReturns409() throws Exception {
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    given(routeService.create(any(CreateRouteRequest.class), any()))
        .willThrow(new VehicleNotAvailableException(vehicleId));

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.VEHICLE_NOT_AVAILABLE.name()));
  }

  @Test
  void postIneligibleDriverReturns409() throws Exception {
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    given(routeService.create(any(CreateRouteRequest.class), any()))
        .willThrow(new DriverNotEligibleException(driverId));

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.DRIVER_NOT_ELIGIBLE.name()));
  }

  @Test
  void postOverlapReturns409() throws Exception {
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    given(routeService.create(any(CreateRouteRequest.class), any()))
        .willThrow(new RouteOverlapException());

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.ROUTE_OVERLAP.name()));
  }

  @Test
  void postDownstreamDownReturns503WithService() throws Exception {
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    given(routeService.create(any(CreateRouteRequest.class), any()))
        .willThrow(new ServiceUnavailableException("vehicles"));

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value(ErrorCode.SERVICE_UNAVAILABLE.name()))
        .andExpect(jsonPath("$.service").value("vehicles"));
  }

  @Test
  void getMissingRouteReturns404() throws Exception {
    UUID id = UUID.randomUUID();
    given(routeService.getById(id)).willThrow(new RouteNotFoundException(id));

    mockMvc
        .perform(get(ApiPathConstants.ROUTE_BASE_PATH + "/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value(ErrorCode.ROUTE_NOT_FOUND.name()));
  }

  @Test
  void postMissingVehicleReturns400WithDetails() throws Exception {
    Map<String, Object> body = validBody(UUID.randomUUID(), UUID.randomUUID());
    body.remove("vehicleId");

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCode.VALIDATION_ERROR.name()))
        .andExpect(jsonPath("$.details[0].field").value("vehicleId"));
  }

  @Test
  void postStartReturnsInProgressPayload() throws Exception {
    UUID id = UUID.randomUUID();
    given(routeService.start(id)).willReturn(startedResponse(id));

    mockMvc
        .perform(post(ApiPathConstants.ROUTE_BASE_PATH + "/" + id + "/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value(RouteStatus.IN_PROGRESS.name()))
        .andExpect(jsonPath("$.startOdometerKm").value(10000));
  }

  @Test
  void postStartWithNonPlannedRouteReturns409() throws Exception {
    UUID id = UUID.randomUUID();
    given(routeService.start(id))
        .willThrow(
            new InvalidRouteStateException(
                RouteStatus.IN_PROGRESS.name(), RouteStatus.IN_PROGRESS.name()));

    mockMvc
        .perform(post(ApiPathConstants.ROUTE_BASE_PATH + "/" + id + "/start"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.INVALID_ROUTE_STATE.name()));
  }

  @Test
  void postStartWithBusyVehicleReturns409() throws Exception {
    UUID id = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    given(routeService.start(id)).willThrow(new VehicleNotAvailableException(vehicleId));

    mockMvc
        .perform(post(ApiPathConstants.ROUTE_BASE_PATH + "/" + id + "/start"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.VEHICLE_NOT_AVAILABLE.name()));
  }

  @Test
  void postCompleteReturnsCompletedPayload() throws Exception {
    UUID id = UUID.randomUUID();
    var request = new CompleteRouteRequest(new BigDecimal("632.10"), "Traffic delay");
    given(routeService.complete(any(), any())).willReturn(completedResponse(id));

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH + "/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value(RouteStatus.COMPLETED.name()))
        .andExpect(jsonPath("$.endOdometerKm").value(12332));
  }

  @Test
  void postCompleteWithNonInProgressRouteReturns409() throws Exception {
    UUID id = UUID.randomUUID();
    var request = new CompleteRouteRequest(new BigDecimal("10.00"), null);
    given(routeService.complete(any(), any()))
        .willThrow(
            new InvalidRouteStateException(
                RouteStatus.PLANNED.name(), RouteStatus.COMPLETED.name()));

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH + "/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.INVALID_ROUTE_STATE.name()));
  }

  @Test
  void postCompleteWhenVehiclesIsDownReturns503() throws Exception {
    UUID id = UUID.randomUUID();
    var request = new CompleteRouteRequest(new BigDecimal("10.00"), null);
    given(routeService.complete(any(), any()))
        .willThrow(new ServiceUnavailableException("vehicles"));

    mockMvc
        .perform(
            post(ApiPathConstants.ROUTE_BASE_PATH + "/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value(ErrorCode.SERVICE_UNAVAILABLE.name()))
        .andExpect(jsonPath("$.service").value("vehicles"));
  }

  private RouteResponse startedResponse(UUID id) {
    return new RouteResponse(
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Madrid",
        "Barcelona",
        Instant.now().plus(2, ChronoUnit.DAYS),
        240,
        new BigDecimal("620.50"),
        RouteStatus.IN_PROGRESS,
        Instant.now(),
        null,
        10000,
        null,
        null,
        null,
        null,
        Instant.now(),
        null);
  }

  private RouteResponse completedResponse(UUID id) {
    return new RouteResponse(
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Madrid",
        "Barcelona",
        Instant.now().minus(2, ChronoUnit.DAYS),
        300,
        new BigDecimal("350.00"),
        RouteStatus.COMPLETED,
        Instant.now().minus(2, ChronoUnit.DAYS),
        Instant.now().minus(2, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS),
        11980,
        12332,
        new BigDecimal("352.00"),
        null,
        null,
        Instant.now(),
        null);
  }

  @Test
  void getStatsDelegatesToService() throws Exception {
    given(routeService.stats(any(), any(), any(), any())).willReturn(null);

    mockMvc
        .perform(
            get(ApiPathConstants.ROUTE_BASE_PATH + "/stats")
                .param("from", "2026-10-01")
                .param("to", "2026-10-31")
                .param("granularity", "DAY"))
        .andExpect(status().isOk());
  }
}
