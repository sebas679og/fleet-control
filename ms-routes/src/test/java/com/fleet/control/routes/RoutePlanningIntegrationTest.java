package com.fleet.control.routes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleet.control.routes.commons.client.DriversClient;
import com.fleet.control.routes.commons.client.VehiclesClient;
import com.fleet.control.routes.commons.client.dto.DriverRef;
import com.fleet.control.routes.commons.client.dto.UpdateVehicleStatusRequest;
import com.fleet.control.routes.commons.client.dto.VehicleRef;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end route planning against a real PostgreSQL Testcontainer with mocked downstream clients.
 * Exercises the real service, repository, mapper, controller and exception handler.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RoutePlanningIntegrationTest {

  private static final String BASE = "/api/routes";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private VehiclesClient vehiclesClient;

  @MockitoBean private DriversClient driversClient;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private final UUID vehicleId = UUID.randomUUID();
  private final UUID driverId = UUID.randomUUID();

  @BeforeEach
  void stubDownstream() {
    given(vehiclesClient.getById(any())).willReturn(new VehicleRef("AVAILABLE", "VAN", 10000));
    given(driversClient.getById(any()))
        .willReturn(new DriverRef("ACTIVE", "B", LocalDate.now().plusDays(30)));
  }

  private Map<String, Object> validBody(UUID vehicle, UUID driver) {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicle.toString());
    body.put("driverId", driver.toString());
    body.put("origin", "Madrid");
    body.put("destination", "Barcelona");
    body.put("plannedStart", Instant.now().plus(2, ChronoUnit.DAYS).toString());
    body.put("estimatedDurationMin", 240);
    body.put("plannedDistanceKm", new BigDecimal("620.50"));
    return body;
  }

  private String planRoute(UUID vehicle, UUID driver) throws Exception {
    String response =
        mockMvc
            .perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validBody(vehicle, driver))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PLANNED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }

  @Test
  void planListAndDetailFlow() throws Exception {
    String id = planRoute(vehicleId, driverId);

    mockMvc
        .perform(
            get(BASE)
                .param("vehicle", vehicleId.toString())
                .param("status", "PLANNED")
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(id));

    mockMvc
        .perform(get(BASE + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
        .andExpect(jsonPath("$.plannedDistanceKm").value(620.50));
  }

  @Test
  void overlappingRouteReturns409() throws Exception {
    planRoute(vehicleId, driverId);

    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.ROUTE_OVERLAP.name()));
  }

  @Test
  void unavailableVehicleReturns409() throws Exception {
    given(vehiclesClient.getById(vehicleId))
        .willReturn(new VehicleRef("OUT_OF_SERVICE", "VAN", 10000));

    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.VEHICLE_NOT_AVAILABLE.name()));
  }

  @Test
  void ineligibleDriverReturns409() throws Exception {
    given(driversClient.getById(driverId))
        .willReturn(new DriverRef("ACTIVE", "B", LocalDate.now().minusDays(1)));

    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.DRIVER_NOT_ELIGIBLE.name()));
  }

  @Test
  void downstreamDownReturns503AndSavesNothing() throws Exception {
    given(vehiclesClient.getById(any())).willThrow(Mockito.mock(FeignException.class));

    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value(ErrorCode.SERVICE_UNAVAILABLE.name()))
        .andExpect(jsonPath("$.service").value("vehicles"));

    mockMvc
        .perform(get(BASE).param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void invalidPaginationReturns400WithDetails() throws Exception {
    mockMvc
        .perform(get(BASE).param("page", "-1").param("size", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCode.VALIDATION_ERROR.name()))
        .andExpect(jsonPath("$.details[0].field").value("page"));
  }

  @Test
  void missingRouteReturns404() throws Exception {
    mockMvc
        .perform(get(BASE + "/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value(ErrorCode.ROUTE_NOT_FOUND.name()));
  }

  @Test
  void missingVehicleReturns404() throws Exception {
    given(vehiclesClient.getById(any())).willThrow(Mockito.mock(FeignException.NotFound.class));

    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value(ErrorCode.VEHICLE_NOT_FOUND.name()));
  }

  @Test
  void missingDriverReturns404() throws Exception {
    given(driversClient.getById(any())).willThrow(Mockito.mock(FeignException.NotFound.class));

    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody(vehicleId, driverId))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value(ErrorCode.DRIVER_NOT_FOUND.name()));
  }

  @Test
  void oversizedPageReturns400WithDetails() throws Exception {
    mockMvc
        .perform(get(BASE).param("page", "0").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCode.VALIDATION_ERROR.name()))
        .andExpect(jsonPath("$.details[0].field").value("size"));
  }

  @Test
  void invertedDateRangeReturns400WithDetails() throws Exception {
    mockMvc
        .perform(
            get(BASE)
                .param("from", LocalDate.now().plusDays(5).toString())
                .param("to", LocalDate.now().toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCode.VALIDATION_ERROR.name()))
        .andExpect(jsonPath("$.details[0].field").value("from"));
  }

  @Test
  void malformedBodyReturns400() throws Exception {
    mockMvc
        .perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{invalid json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(ErrorCode.VALIDATION_ERROR.name()));
  }

  @Test
  void startMovesRouteToInProgressAndPersists() throws Exception {
    String id = planRoute(vehicleId, driverId);

    mockMvc
        .perform(post(BASE + "/" + id + "/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.startedAt").exists())
        .andExpect(jsonPath("$.startOdometerKm").value(10000));

    Mockito.verify(vehiclesClient)
        .updateStatus(eq(vehicleId), eq(new UpdateVehicleStatusRequest("IN_USE", 10000)));

    mockMvc
        .perform(get(BASE + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void startTwiceReturns409InvalidState() throws Exception {
    String id = planRoute(vehicleId, driverId);

    mockMvc.perform(post(BASE + "/" + id + "/start")).andExpect(status().isOk());

    mockMvc
        .perform(post(BASE + "/" + id + "/start"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.INVALID_ROUTE_STATE.name()));
  }

  @Test
  void startWithBusyVehicleReturns409() throws Exception {
    String id = planRoute(vehicleId, driverId);
    given(vehiclesClient.getById(vehicleId)).willReturn(new VehicleRef("IN_USE", "VAN", 10000));

    mockMvc
        .perform(post(BASE + "/" + id + "/start"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.VEHICLE_NOT_AVAILABLE.name()));

    Mockito.verify(vehiclesClient, Mockito.never()).updateStatus(any(), any());
  }

  @Test
  void completeFlowPersistsCompletedAndReleasesVehicle() throws Exception {
    String id = planRoute(vehicleId, driverId);
    mockMvc.perform(post(BASE + "/" + id + "/start")).andExpect(status().isOk());

    mockMvc
        .perform(
            post(BASE + "/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actualDistanceKm\": 632.10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.endedAt").exists())
        .andExpect(jsonPath("$.endOdometerKm").value(10632))
        .andExpect(jsonPath("$.actualDistanceKm").value(632.10));

    Mockito.verify(vehiclesClient)
        .updateStatus(eq(vehicleId), eq(new UpdateVehicleStatusRequest("AVAILABLE", 10632)));

    mockMvc
        .perform(get(BASE + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void completeRetryAfterDownstreamFailure() throws Exception {
    String id = planRoute(vehicleId, driverId);
    mockMvc.perform(post(BASE + "/" + id + "/start")).andExpect(status().isOk());
    Mockito.doThrow(Mockito.mock(FeignException.class))
        .doNothing()
        .when(vehiclesClient)
        .updateStatus(any(), any());

    mockMvc
        .perform(
            post(BASE + "/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actualDistanceKm\": 10.00}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value(ErrorCode.SERVICE_UNAVAILABLE.name()))
        .andExpect(jsonPath("$.service").value("vehicles"));

    mockMvc
        .perform(get(BASE + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    mockMvc
        .perform(
            post(BASE + "/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actualDistanceKm\": 10.00}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.endOdometerKm").value(10010));
  }

  @Test
  void completePlannedRouteReturns409() throws Exception {
    String id = planRoute(vehicleId, driverId);

    mockMvc
        .perform(
            post(BASE + "/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actualDistanceKm\": 10.00}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value(ErrorCode.INVALID_ROUTE_STATE.name()));
  }
}
