package com.fleet.control.routes.service.impl;

import com.fleet.control.routes.commons.client.DriversClient;
import com.fleet.control.routes.commons.client.VehiclesClient;
import com.fleet.control.routes.commons.client.dto.DriverRef;
import com.fleet.control.routes.commons.client.dto.UpdateVehicleStatusRequest;
import com.fleet.control.routes.commons.client.dto.VehicleRef;
import com.fleet.control.routes.commons.exception.exception.DriverNotEligibleException;
import com.fleet.control.routes.commons.exception.exception.DriverNotFoundException;
import com.fleet.control.routes.commons.exception.exception.InvalidRouteStateException;
import com.fleet.control.routes.commons.exception.exception.RouteNotFoundException;
import com.fleet.control.routes.commons.exception.exception.RouteOverlapException;
import com.fleet.control.routes.commons.exception.exception.ServiceUnavailableException;
import com.fleet.control.routes.commons.exception.exception.ValidationException;
import com.fleet.control.routes.commons.exception.exception.VehicleNotAvailableException;
import com.fleet.control.routes.commons.exception.exception.VehicleNotFoundException;
import com.fleet.control.routes.models.dtos.request.CompleteRouteRequest;
import com.fleet.control.routes.models.dtos.request.CreateRouteRequest;
import com.fleet.control.routes.models.dtos.response.PageResponse;
import com.fleet.control.routes.models.dtos.response.RouteResponse;
import com.fleet.control.routes.models.dtos.response.RouteStatsResponse;
import com.fleet.control.routes.models.entities.Route;
import com.fleet.control.routes.models.enums.Granularity;
import com.fleet.control.routes.models.enums.RouteStatus;
import com.fleet.control.routes.models.mappers.RouteMapper;
import com.fleet.control.routes.repository.RouteRepository;
import com.fleet.control.routes.service.RouteService;
import feign.FeignException;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Route planning, execution and history. Implements US-09 create/list/detail. */
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

  private static final int MAX_PAGE_SIZE = 100;

  private static final String AVAILABLE_STATUS = "AVAILABLE";

  /** License categories accepted per vehicle type (hierarchical, DGT style). */
  private static final Map<String, Set<String>> CATEGORY_BY_VEHICLE_TYPE =
      Map.of(
          "MOTORCYCLE", Set.of("A"),
          "CAR", Set.of("B", "C"),
          "VAN", Set.of("B", "C"),
          "TRUCK", Set.of("C"));

  private final RouteRepository routeRepository;
  private final RouteMapper routeMapper;
  private final VehiclesClient vehiclesClient;
  private final DriversClient driversClient;

  @Override
  @Transactional
  public RouteResponse create(CreateRouteRequest request, UUID createdBy) {
    VehicleRef vehicle = fetchVehicle(request.vehicleId());
    if (!AVAILABLE_STATUS.equals(vehicle.status())) {
      throw new VehicleNotAvailableException(request.vehicleId());
    }
    DriverRef driver = fetchDriver(request.driverId());
    validateDriver(request, vehicle, driver);

    Instant start = request.plannedStart();
    Instant end = start.plus(request.estimatedDurationMin(), ChronoUnit.MINUTES);
    if (routeRepository.existsOverlap(request.vehicleId(), request.driverId(), start, end)) {
      throw new RouteOverlapException();
    }

    Route saved =
        routeRepository.save(
            Route.builder()
                .vehicleId(request.vehicleId())
                .driverId(request.driverId())
                .origin(request.origin())
                .destination(request.destination())
                .plannedStart(start)
                .estimatedDurationMin(request.estimatedDurationMin())
                .plannedDistanceKm(request.plannedDistanceKm())
                .status(RouteStatus.PLANNED)
                .createdBy(createdBy)
                .build());
    return routeMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<RouteResponse> list(
      UUID vehicleId,
      UUID driverId,
      RouteStatus status,
      LocalDate from,
      LocalDate to,
      int page,
      int size) {
    if (page < 0) {
      throw new ValidationException("page", "page must be 0 or greater");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new ValidationException("size", "size must be between 1 and " + MAX_PAGE_SIZE);
    }
    if (from != null && to != null && from.isAfter(to)) {
      throw new ValidationException("from", "from must be on or before to");
    }
    Instant fromInstant = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant toInstant = to == null ? null : to.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

    Page<Route> result =
        routeRepository.search(
            vehicleId,
            driverId,
            status,
            fromInstant,
            toInstant,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "plannedStart")));
    return PageResponse.of(
        result.getContent().stream().map(routeMapper::toResponse).toList(),
        page,
        size,
        result.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public RouteResponse getById(UUID id) {
    return routeRepository
        .findById(id)
        .map(routeMapper::toResponse)
        .orElseThrow(() -> new RouteNotFoundException(id));
  }

  @Override
  @Transactional
  public RouteResponse start(UUID id) {
    Route route = routeRepository.findById(id).orElseThrow(() -> new RouteNotFoundException(id));
    if (route.getStatus() != RouteStatus.PLANNED) {
      throw new InvalidRouteStateException(
          route.getStatus().name(), RouteStatus.IN_PROGRESS.name());
    }
    VehicleRef vehicle = fetchVehicle(route.getVehicleId());
    if (!AVAILABLE_STATUS.equals(vehicle.status())) {
      throw new VehicleNotAvailableException(route.getVehicleId());
    }
    int startOdometer = vehicle.odometerKm() == null ? 0 : vehicle.odometerKm();
    changeVehicleStatus(
        route.getVehicleId(),
        "IN_USE",
        startOdometer,
        () -> new VehicleNotAvailableException(route.getVehicleId()));

    route.setStatus(RouteStatus.IN_PROGRESS);
    route.setStartedAt(Instant.now());
    route.setStartOdometerKm(startOdometer);
    final Route started;
    try {
      started = routeRepository.save(route);
    } catch (DataAccessException saveFailure) {
      // Any persistence failure must compensate: the vehicle was already moved to IN_USE.
      revertVehicleStatus(route.getVehicleId(), startOdometer);
      throw saveFailure;
    }
    return routeMapper.toResponse(started);
  }

  @Override
  @Transactional
  public RouteResponse complete(UUID id, CompleteRouteRequest request) {
    Route route = routeRepository.findById(id).orElseThrow(() -> new RouteNotFoundException(id));
    if (route.getStatus() != RouteStatus.IN_PROGRESS) {
      throw new InvalidRouteStateException(route.getStatus().name(), RouteStatus.COMPLETED.name());
    }
    fetchVehicle(route.getVehicleId());
    int startOdometer = route.getStartOdometerKm() == null ? 0 : route.getStartOdometerKm();
    int endOdometer =
        startOdometer
            + request.actualDistanceKm().setScale(0, RoundingMode.HALF_UP).intValueExact();
    changeVehicleStatus(
        route.getVehicleId(),
        AVAILABLE_STATUS,
        endOdometer,
        () ->
            new InvalidRouteStateException(
                RouteStatus.IN_PROGRESS.name(), RouteStatus.COMPLETED.name()));

    route.setStatus(RouteStatus.COMPLETED);
    route.setEndedAt(Instant.now());
    route.setEndOdometerKm(endOdometer);
    route.setActualDistanceKm(request.actualDistanceKm());
    route.setNotes(request.notes());
    return routeMapper.toResponse(routeRepository.save(route));
  }

  @Override
  public RouteStatsResponse stats(
      LocalDate from, LocalDate to, Granularity granularity, UUID vehicleId) {
    return null;
  }

  /** Fetches the vehicle or maps downstream failures to 404/503. */
  private VehicleRef fetchVehicle(UUID vehicleId) {
    try {
      return vehiclesClient.getById(vehicleId);
    } catch (FeignException.NotFound e) {
      throw new VehicleNotFoundException(vehicleId);
    } catch (FeignException e) {
      throw new ServiceUnavailableException("vehicles");
    }
  }

  /** Fetches the driver or maps downstream failures to 404/503. */
  private DriverRef fetchDriver(UUID driverId) {
    try {
      return driversClient.getById(driverId);
    } catch (FeignException.NotFound e) {
      throw new DriverNotFoundException(driverId);
    } catch (FeignException e) {
      throw new ServiceUnavailableException("drivers");
    }
  }

  /** Changes the vehicle status or maps downstream failures to 404/409/503. */
  private void changeVehicleStatus(
      UUID vehicleId, String status, int odometerKm, Supplier<RuntimeException> onConflict) {
    try {
      vehiclesClient.updateStatus(vehicleId, new UpdateVehicleStatusRequest(status, odometerKm));
    } catch (FeignException.NotFound e) {
      throw new VehicleNotFoundException(vehicleId);
    } catch (FeignException.Conflict e) {
      throw onConflict.get();
    } catch (FeignException e) {
      throw new ServiceUnavailableException("vehicles");
    }
  }

  /** Best-effort compensation: returns the vehicle to AVAILABLE after a failed route save. */
  private void revertVehicleStatus(UUID vehicleId, int odometerKm) {
    try {
      vehiclesClient.updateStatus(
          vehicleId, new UpdateVehicleStatusRequest(AVAILABLE_STATUS, odometerKm));
    } catch (FeignException ignored) {
      // Original save failure takes precedence; compensation already attempted.
    }
  }

  /** Rejects inactive drivers, expired licenses and incompatible categories with 409. */
  private void validateDriver(CreateRouteRequest request, VehicleRef vehicle, DriverRef driver) {
    if (!"ACTIVE".equals(driver.status())) {
      throw new DriverNotEligibleException(request.driverId());
    }
    LocalDate startDate = request.plannedStart().atZone(ZoneOffset.UTC).toLocalDate();
    if (driver.licenseExpiresAt() == null || driver.licenseExpiresAt().isBefore(startDate)) {
      throw new DriverNotEligibleException(request.driverId());
    }
    Set<String> allowed = CATEGORY_BY_VEHICLE_TYPE.getOrDefault(vehicle.type(), Set.of());
    if (!allowed.contains(driver.licenseCategory())) {
      throw new DriverNotEligibleException(request.driverId());
    }
  }
}
