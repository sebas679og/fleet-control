package com.fleet.control.fuel.service.impl;

import com.fleet.control.fuel.commons.client.VehiclesClient;
import com.fleet.control.fuel.commons.client.dto.VehicleRef;
import com.fleet.control.fuel.commons.exception.exception.InvalidOdometerException;
import com.fleet.control.fuel.commons.exception.exception.ServiceUnavailableException;
import com.fleet.control.fuel.commons.exception.exception.ValidationException;
import com.fleet.control.fuel.commons.exception.exception.VehicleNotFoundException;
import com.fleet.control.fuel.models.dto.request.CreateFuelRecordRequest;
import com.fleet.control.fuel.models.dto.response.FuelConsumptionResponse;
import com.fleet.control.fuel.models.dto.response.FuelConsumptionResponse.VehicleConsumption;
import com.fleet.control.fuel.models.dto.response.FuelRecordResponse;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse.Point;
import com.fleet.control.fuel.models.dto.response.PageResponse;
import com.fleet.control.fuel.models.entities.FuelRecord;
import com.fleet.control.fuel.models.enums.FuelType;
import com.fleet.control.fuel.models.enums.Granularity;
import com.fleet.control.fuel.models.mappers.FuelRecordMapper;
import com.fleet.control.fuel.repository.FuelRepository;
import com.fleet.control.fuel.service.FuelService;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Refuel registration, history, aggregated consumption and statistics. */
@Service
@RequiredArgsConstructor
public class FuelServiceImpl implements FuelService {

  private static final int MAX_PAGE_SIZE = 100;

  private final FuelRepository fuelRepository;
  private final FuelRecordMapper fuelRecordMapper;
  private final VehiclesClient vehiclesClient;
  private final FuelReporting fuelReporting;

  @Override
  @Transactional
  public FuelRecordResponse create(CreateFuelRecordRequest request) {
    VehicleRef vehicle = fetchVehicle(request.vehicleId());
    validateCapacity(request, vehicle);
    validateOdometer(request);
    FuelRecord entity = fuelRecordMapper.toEntity(request);
    entity.setFuelType(FuelType.valueOf(vehicle.fuelType()));
    entity.setTotalCost(FuelMath.costOf(request.liters(), request.pricePerLiter()));
    entity.setConsumptionL100km(consumptionForNewRefuel(entity));
    return fuelRecordMapper.toDto(fuelRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<FuelRecordResponse> list(
      UUID vehicleId, LocalDate from, LocalDate to, int page, int size) {
    validatePage(page, size);
    if (from != null && to != null && from.isAfter(to)) {
      throw new ValidationException("from", "from must be on or before to");
    }
    Instant fromInstant = from == null ? null : fuelReporting.startOfDay(from);
    Instant toInstant = to == null ? null : fuelReporting.startOfNextDay(to);
    Page<FuelRecord> result =
        fuelRepository.search(vehicleId, fromInstant, toInstant, PageRequest.of(page, size));
    return PageResponse.of(
        result.getContent().stream().map(fuelRecordMapper::toDto).toList(),
        page,
        size,
        result.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public FuelConsumptionResponse consumption(LocalDate from, LocalDate to) {
    requireRange(from, to);
    List<FuelRecord> records =
        fuelRepository.findInPeriod(
            fuelReporting.startOfDay(from), fuelReporting.startOfNextDay(to), null);
    List<VehicleConsumption> vehicles =
        FuelMath.groupByVehicle(records).entrySet().stream()
            .map(entry -> fuelReporting.toVehicleConsumption(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(VehicleConsumption::cost).reversed())
            .toList();
    return new FuelConsumptionResponse(from, to, vehicles);
  }

  @Override
  @Transactional(readOnly = true)
  public FuelStatsResponse stats(
      LocalDate from, LocalDate to, Granularity granularity, UUID vehicleId) {
    requireRange(from, to);
    Granularity bucket = granularity == null ? Granularity.DAY : granularity;
    List<FuelRecord> records =
        fuelRepository.findInPeriod(
            fuelReporting.startOfDay(from), fuelReporting.startOfNextDay(to), vehicleId);
    List<Point> series =
        fuelReporting.bucketsBetween(from, to, bucket).stream()
            .map(period -> fuelReporting.pointOf(period, bucket, records, from, to))
            .toList();
    return new FuelStatsResponse(from, to, bucket, fuelReporting.totalsOf(records), series);
  }

  /** Rejects refuels exceeding the vehicle tank capacity with 400. */
  private void validateCapacity(CreateFuelRecordRequest request, VehicleRef vehicle) {
    if (vehicle.tankCapacityL() != null
        && request.liters().compareTo(BigDecimal.valueOf(vehicle.tankCapacityL())) > 0) {
      throw new ValidationException(
          "liters", "must not exceed the tank capacity (" + vehicle.tankCapacityL() + " L)");
    }
  }

  /** Rejects odometer rollbacks against the last recorded refuel with 409. */
  private void validateOdometer(CreateFuelRecordRequest request) {
    fuelRepository
        .findTopByVehicleIdOrderByRefueledAtDesc(request.vehicleId())
        .ifPresent(
            last -> {
              if (request.odometerKm() < last.getOdometerKm()) {
                throw new InvalidOdometerException(last.getOdometerKm(), request.odometerKm());
              }
            });
  }

  /** Computes consumption for a full tank or returns null when it cannot be computed. */
  private BigDecimal consumptionForNewRefuel(FuelRecord entity) {
    if (!entity.isFullTank()) {
      return null;
    }
    return fuelRepository
        .findTopByVehicleIdAndFullTankTrueAndRefueledAtBeforeOrderByRefueledAtDesc(
            entity.getVehicleId(), entity.getRefueledAt())
        .map(previous -> consumptionSince(previous, entity))
        .orElse(null);
  }

  /** Applies the (liters since previous full tank / km between full tanks) x 100 formula. */
  private BigDecimal consumptionSince(FuelRecord previous, FuelRecord current) {
    int distanceKm = current.getOdometerKm() - previous.getOdometerKm();
    if (distanceKm <= 0) {
      return null;
    }
    BigDecimal litersSince =
        fuelRepository
            .sumLitersBetween(
                current.getVehicleId(), previous.getRefueledAt(), current.getRefueledAt())
            .add(current.getLiters());
    return FuelMath.averagePer100km(litersSince, distanceKm);
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

  /** Validates pagination parameters with 400. */
  private void validatePage(int page, int size) {
    if (page < 0) {
      throw new ValidationException("page", "page must be 0 or greater");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new ValidationException("size", "size must be between 1 and " + MAX_PAGE_SIZE);
    }
  }

  /** Requires a closed date range with 400. */
  private void requireRange(LocalDate from, LocalDate to) {
    if (from == null) {
      throw new ValidationException("from", "from is required");
    }
    if (to == null) {
      throw new ValidationException("to", "to is required");
    }
    if (from.isAfter(to)) {
      throw new ValidationException("from", "from must be on or before to");
    }
  }
}
