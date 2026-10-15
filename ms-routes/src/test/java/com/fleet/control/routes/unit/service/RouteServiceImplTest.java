package com.fleet.control.routes.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fleet.control.routes.commons.client.DriversClient;
import com.fleet.control.routes.commons.client.VehiclesClient;
import com.fleet.control.routes.commons.client.dto.DriverRef;
import com.fleet.control.routes.commons.client.dto.UpdateVehicleStatusRequest;
import com.fleet.control.routes.commons.client.dto.VehicleRef;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import com.fleet.control.routes.commons.exception.exception.DriverNotEligibleException;
import com.fleet.control.routes.commons.exception.exception.InvalidRouteStateException;
import com.fleet.control.routes.commons.exception.exception.RouteNotFoundException;
import com.fleet.control.routes.commons.exception.exception.RouteOverlapException;
import com.fleet.control.routes.commons.exception.exception.ServiceUnavailableException;
import com.fleet.control.routes.commons.exception.exception.VehicleNotAvailableException;
import com.fleet.control.routes.commons.exception.exception.VehicleNotFoundException;
import com.fleet.control.routes.models.dtos.request.CompleteRouteRequest;
import com.fleet.control.routes.models.dtos.request.CreateRouteRequest;
import com.fleet.control.routes.models.dtos.response.RouteResponse;
import com.fleet.control.routes.models.entities.Route;
import com.fleet.control.routes.models.enums.RouteStatus;
import com.fleet.control.routes.models.mappers.RouteMapper;
import com.fleet.control.routes.repository.RouteRepository;
import com.fleet.control.routes.service.impl.RouteServiceImpl;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for route planning (US-09) and execution (US-10): happy paths plus the exception edge
 * cases (409s, 404s, 503 with nothing saved, compensation on failed save, retryable complete).
 */
@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

  @Mock private RouteRepository routeRepository;

  @Mock private RouteMapper routeMapper;

  @Mock private VehiclesClient vehiclesClient;

  @Mock private DriversClient driversClient;

  @InjectMocks private RouteServiceImpl service;

  private CreateRouteRequest validRequest(UUID vehicleId, UUID driverId) {
    return new CreateRouteRequest(
        vehicleId,
        driverId,
        "Madrid",
        "Barcelona",
        Instant.now().plus(2, ChronoUnit.DAYS),
        240,
        new BigDecimal("620.50"));
  }

  private RouteResponse plannedResponse(CreateRouteRequest request) {
    return new RouteResponse(
        UUID.randomUUID(),
        request.vehicleId(),
        request.driverId(),
        request.origin(),
        request.destination(),
        request.plannedStart(),
        request.estimatedDurationMin(),
        request.plannedDistanceKm(),
        RouteStatus.PLANNED,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void createPlansRouteWhenVehicleAndDriverAreValid() {
    var request = validRequest(UUID.randomUUID(), UUID.randomUUID());
    given(vehiclesClient.getById(request.vehicleId()))
        .willReturn(new VehicleRef("AVAILABLE", "VAN", 10000));
    given(driversClient.getById(request.driverId()))
        .willReturn(new DriverRef("ACTIVE", "B", LocalDate.now().plusDays(30)));
    given(routeRepository.existsOverlap(any(), any(), any(), any())).willReturn(false);
    given(routeRepository.save(any(Route.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(routeMapper.toResponse(any(Route.class))).willReturn(plannedResponse(request));

    RouteResponse result = service.create(request, null);

    assertThat(result.status()).isEqualTo(RouteStatus.PLANNED);
    ArgumentCaptor<Route> captor = ArgumentCaptor.forClass(Route.class);
    verify(routeRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(RouteStatus.PLANNED);
  }

  @Test
  void createWithOutOfServiceVehicleReturns409() {
    var request = validRequest(UUID.randomUUID(), UUID.randomUUID());
    given(vehiclesClient.getById(request.vehicleId()))
        .willReturn(new VehicleRef("OUT_OF_SERVICE", "VAN", 10000));

    var ex = assertThrows(VehicleNotAvailableException.class, () -> service.create(request, null));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VEHICLE_NOT_AVAILABLE);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void createWithExpiredLicenseReturns409() {
    var request = validRequest(UUID.randomUUID(), UUID.randomUUID());
    given(vehiclesClient.getById(request.vehicleId()))
        .willReturn(new VehicleRef("AVAILABLE", "VAN", 10000));
    given(driversClient.getById(request.driverId()))
        .willReturn(new DriverRef("ACTIVE", "B", LocalDate.now().minusDays(1)));

    var ex = assertThrows(DriverNotEligibleException.class, () -> service.create(request, null));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.DRIVER_NOT_ELIGIBLE);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void createWithOverlapReturns409() {
    var request = validRequest(UUID.randomUUID(), UUID.randomUUID());
    given(vehiclesClient.getById(request.vehicleId()))
        .willReturn(new VehicleRef("AVAILABLE", "VAN", 10000));
    given(driversClient.getById(request.driverId()))
        .willReturn(new DriverRef("ACTIVE", "B", LocalDate.now().plusDays(30)));
    given(routeRepository.existsOverlap(any(), any(), any(), any())).willReturn(true);

    var ex = assertThrows(RouteOverlapException.class, () -> service.create(request, null));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.ROUTE_OVERLAP);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void createWhenVehiclesIsDownReturns503AndSavesNothing() {
    var request = validRequest(UUID.randomUUID(), UUID.randomUUID());
    given(vehiclesClient.getById(request.vehicleId())).willThrow(mock(FeignException.class));

    var ex = assertThrows(ServiceUnavailableException.class, () -> service.create(request, null));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(ex.getService()).isEqualTo("vehicles");
    verify(routeRepository, never()).save(any(Route.class));
  }

  private Route plannedRoute(UUID vehicleId, UUID driverId) {
    return Route.builder()
        .id(UUID.randomUUID())
        .vehicleId(vehicleId)
        .driverId(driverId)
        .origin("Madrid")
        .destination("Barcelona")
        .plannedStart(Instant.now().plus(2, ChronoUnit.DAYS))
        .estimatedDurationMin(240)
        .plannedDistanceKm(new BigDecimal("620.50"))
        .status(RouteStatus.PLANNED)
        .build();
  }

  private Route inProgressRoute(UUID vehicleId, UUID driverId, int startOdometerKm) {
    Route route = plannedRoute(vehicleId, driverId);
    route.setStatus(RouteStatus.IN_PROGRESS);
    route.setStartedAt(Instant.now().minus(1, ChronoUnit.HOURS));
    route.setStartOdometerKm(startOdometerKm);
    return route;
  }

  @Test
  void startMovesPlannedRouteToInProgressAndVehicleToInUse() {
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    Route route = plannedRoute(vehicleId, driverId);
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(vehicleId)).willReturn(new VehicleRef("AVAILABLE", "VAN", 10000));
    given(routeRepository.save(any(Route.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(routeMapper.toResponse(any(Route.class)))
        .willAnswer(
            invocation -> {
              Route saved = invocation.getArgument(0);
              return new RouteResponse(
                  saved.getId(),
                  saved.getVehicleId(),
                  saved.getDriverId(),
                  saved.getOrigin(),
                  saved.getDestination(),
                  saved.getPlannedStart(),
                  saved.getEstimatedDurationMin(),
                  saved.getPlannedDistanceKm(),
                  saved.getStatus(),
                  saved.getStartedAt(),
                  saved.getEndedAt(),
                  saved.getStartOdometerKm(),
                  saved.getEndOdometerKm(),
                  saved.getActualDistanceKm(),
                  saved.getNotes(),
                  null,
                  saved.getCreatedAt(),
                  null);
            });

    RouteResponse started = service.start(route.getId());

    assertThat(started.status()).isEqualTo(RouteStatus.IN_PROGRESS);
    ArgumentCaptor<Route> captor = ArgumentCaptor.forClass(Route.class);
    verify(routeRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(RouteStatus.IN_PROGRESS);
    assertThat(captor.getValue().getStartedAt()).isNotNull();
    assertThat(captor.getValue().getStartOdometerKm()).isEqualTo(10000);
    verify(vehiclesClient).updateStatus(vehicleId, new UpdateVehicleStatusRequest("IN_USE", 10000));
  }

  @Test
  void startWithNonPlannedRouteReturns409() {
    Route route = inProgressRoute(UUID.randomUUID(), UUID.randomUUID(), 10000);
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));

    var ex = assertThrows(InvalidRouteStateException.class, () -> service.start(route.getId()));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_ROUTE_STATE);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    verify(vehiclesClient, never()).getById(any());
    verify(vehiclesClient, never()).updateStatus(any(), any());
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void startWithMissingRouteReturns404() {
    UUID id = UUID.randomUUID();
    given(routeRepository.findById(id)).willReturn(Optional.empty());

    var ex = assertThrows(RouteNotFoundException.class, () -> service.start(id));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void startWithBusyVehicleReturns409() {
    UUID vehicleId = UUID.randomUUID();
    Route route = plannedRoute(vehicleId, UUID.randomUUID());
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(vehicleId)).willReturn(new VehicleRef("IN_USE", "VAN", 10000));

    var ex = assertThrows(VehicleNotAvailableException.class, () -> service.start(route.getId()));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VEHICLE_NOT_AVAILABLE);
    verify(vehiclesClient, never()).updateStatus(any(), any());
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void startWhenSaveFailsRevertsVehicleToAvailable() {
    UUID vehicleId = UUID.randomUUID();
    Route route = plannedRoute(vehicleId, UUID.randomUUID());
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(vehicleId)).willReturn(new VehicleRef("AVAILABLE", "VAN", 10000));
    given(routeRepository.save(any(Route.class)))
        .willThrow(new DataAccessResourceFailureException("db down"));

    assertThrows(DataAccessException.class, () -> service.start(route.getId()));

    InOrder order = Mockito.inOrder(vehiclesClient);
    order
        .verify(vehiclesClient)
        .updateStatus(vehicleId, new UpdateVehicleStatusRequest("IN_USE", 10000));
    order
        .verify(vehiclesClient)
        .updateStatus(vehicleId, new UpdateVehicleStatusRequest("AVAILABLE", 10000));
  }

  @Test
  void startWhenVehiclesIsDownReturns503AndChangesNothing() {
    Route route = plannedRoute(UUID.randomUUID(), UUID.randomUUID());
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(route.getVehicleId())).willThrow(mock(FeignException.class));

    var ex = assertThrows(ServiceUnavailableException.class, () -> service.start(route.getId()));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    verify(vehiclesClient, never()).updateStatus(any(), any());
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void startWithMissingVehicleReturns404() {
    Route route = plannedRoute(UUID.randomUUID(), UUID.randomUUID());
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(route.getVehicleId()))
        .willThrow(mock(FeignException.NotFound.class));

    var ex = assertThrows(VehicleNotFoundException.class, () -> service.start(route.getId()));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void completeMovesInProgressRouteToCompletedAndReleasesVehicle() {
    UUID vehicleId = UUID.randomUUID();
    UUID driverId = UUID.randomUUID();
    Route route = inProgressRoute(vehicleId, driverId, 10000);
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(vehicleId)).willReturn(new VehicleRef("IN_USE", "VAN", 10150));
    given(routeRepository.save(any(Route.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(routeMapper.toResponse(any(Route.class)))
        .willAnswer(
            invocation -> {
              Route saved = invocation.getArgument(0);
              return new RouteResponse(
                  saved.getId(),
                  saved.getVehicleId(),
                  saved.getDriverId(),
                  saved.getOrigin(),
                  saved.getDestination(),
                  saved.getPlannedStart(),
                  saved.getEstimatedDurationMin(),
                  saved.getPlannedDistanceKm(),
                  saved.getStatus(),
                  saved.getStartedAt(),
                  saved.getEndedAt(),
                  saved.getStartOdometerKm(),
                  saved.getEndOdometerKm(),
                  saved.getActualDistanceKm(),
                  saved.getNotes(),
                  null,
                  saved.getCreatedAt(),
                  null);
            });

    RouteResponse completed =
        service.complete(
            route.getId(), new CompleteRouteRequest(new BigDecimal("632.10"), "Traffic delay"));

    assertThat(completed.status()).isEqualTo(RouteStatus.COMPLETED);
    assertThat(completed.endOdometerKm()).isEqualTo(10632);
    verify(vehiclesClient)
        .updateStatus(vehicleId, new UpdateVehicleStatusRequest("AVAILABLE", 10632));
    ArgumentCaptor<Route> captor = ArgumentCaptor.forClass(Route.class);
    verify(routeRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(RouteStatus.COMPLETED);
    assertThat(captor.getValue().getEndedAt()).isNotNull();
    assertThat(captor.getValue().getEndOdometerKm()).isEqualTo(10632);
    assertThat(captor.getValue().getActualDistanceKm()).isEqualByComparingTo("632.10");
    assertThat(captor.getValue().getNotes()).isEqualTo("Traffic delay");
  }

  @Test
  void completeWithNonInProgressRouteReturns409() {
    Route route = plannedRoute(UUID.randomUUID(), UUID.randomUUID());
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));

    var ex =
        assertThrows(
            InvalidRouteStateException.class,
            () ->
                service.complete(
                    route.getId(), new CompleteRouteRequest(new BigDecimal("10.00"), null)));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_ROUTE_STATE);
    verify(vehiclesClient, never()).getById(any());
    verify(vehiclesClient, never()).updateStatus(any(), any());
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void completeWhenVehiclesIsDownKeepsRouteInProgressForRetry() {
    Route route = inProgressRoute(UUID.randomUUID(), UUID.randomUUID(), 10000);
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(route.getVehicleId()))
        .willReturn(new VehicleRef("IN_USE", "VAN", 10150));
    Mockito.doThrow(mock(FeignException.class)).when(vehiclesClient).updateStatus(any(), any());

    var ex =
        assertThrows(
            ServiceUnavailableException.class,
            () ->
                service.complete(
                    route.getId(), new CompleteRouteRequest(new BigDecimal("10.00"), null)));

    assertThat(ex.getService()).isEqualTo("vehicles");
    assertThat(route.getStatus()).isEqualTo(RouteStatus.IN_PROGRESS);
    verify(routeRepository, never()).save(any(Route.class));
  }

  @Test
  void completeWithVehicleConflictReturns409() {
    Route route = inProgressRoute(UUID.randomUUID(), UUID.randomUUID(), 10000);
    given(routeRepository.findById(route.getId())).willReturn(Optional.of(route));
    given(vehiclesClient.getById(route.getVehicleId()))
        .willReturn(new VehicleRef("IN_USE", "VAN", 10150));
    Mockito.doThrow(mock(FeignException.Conflict.class))
        .when(vehiclesClient)
        .updateStatus(any(), any());

    var ex =
        assertThrows(
            InvalidRouteStateException.class,
            () ->
                service.complete(
                    route.getId(), new CompleteRouteRequest(new BigDecimal("10.00"), null)));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_ROUTE_STATE);
    verify(routeRepository, never()).save(any(Route.class));
  }
}
