package com.fleet.control.fuel.service.impl;

import com.fleet.control.fuel.commons.client.VehiclesClient;
import com.fleet.control.fuel.commons.client.dto.VehicleRef;
import com.fleet.control.fuel.commons.exception.exception.ServiceUnavailableException;
import com.fleet.control.fuel.models.dto.response.FuelConsumptionResponse.VehicleConsumption;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse.Point;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse.Totals;
import com.fleet.control.fuel.models.entities.FuelRecord;
import com.fleet.control.fuel.models.enums.Granularity;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Builds consumption rows, totals and time buckets for fuel reports. */
@Component
@RequiredArgsConstructor
public class FuelReporting {

  private final VehiclesClient vehiclesClient;

  /** Aggregates a single vehicle consumption row with plate and type enrichment. */
  public VehicleConsumption toVehicleConsumption(UUID vehicleId, List<FuelRecord> records) {
    BigDecimal liters = FuelMath.sumLiters(records);
    BigDecimal cost = FuelMath.sumCost(records);
    int distanceKm = FuelMath.odometerDistance(records);
    VehicleRef vehicle = fetchVehicleLenient(vehicleId);
    return new VehicleConsumption(
        vehicleId,
        vehicle == null ? null : vehicle.plate(),
        vehicle == null ? null : vehicle.type(),
        liters,
        cost,
        distanceKm,
        FuelMath.averagePer100km(liters, distanceKm));
  }

  /** Aggregates totals for the requested period, zeros when there are no records. */
  public Totals totalsOf(List<FuelRecord> records) {
    if (records.isEmpty()) {
      return new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    BigDecimal liters = FuelMath.sumLiters(records);
    BigDecimal cost = FuelMath.sumCost(records);
    return new Totals(
        liters,
        cost,
        FuelMath.averagePrice(cost, liters),
        FuelMath.averagePer100km(liters, FuelMath.totalDistance(records)));
  }

  /** Builds a single series point clamped to the requested range. */
  public Point pointOf(
      LocalDate period,
      Granularity granularity,
      List<FuelRecord> records,
      LocalDate from,
      LocalDate to) {
    Instant periodFrom = maxInstant(startOfDay(from), startOfDay(period));
    Instant periodTo = minInstant(startOfNextDay(to), startOfDay(nextBucket(period, granularity)));
    List<FuelRecord> inBucket =
        records.stream()
            .filter(
                record ->
                    !record.getRefueledAt().isBefore(periodFrom)
                        && record.getRefueledAt().isBefore(periodTo))
            .toList();
    if (inBucket.isEmpty()) {
      return new Point(period, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    BigDecimal liters = FuelMath.sumLiters(inBucket);
    BigDecimal cost = FuelMath.sumCost(inBucket);
    return new Point(
        period,
        liters,
        cost,
        FuelMath.averagePrice(cost, liters),
        FuelMath.averagePer100km(liters, FuelMath.totalDistance(inBucket)));
  }

  /** Lists every bucket start covering the range, starting at its natural boundary. */
  public List<LocalDate> bucketsBetween(LocalDate from, LocalDate to, Granularity granularity) {
    return Stream.iterate(
            bucketStart(from, granularity),
            period -> !period.isAfter(to),
            period -> nextBucket(period, granularity))
        .toList();
  }

  /** Converts a date to the start of day in UTC. */
  public Instant startOfDay(LocalDate date) {
    return date.atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  /** Converts an inclusive end date to the exclusive upper bound in UTC. */
  public Instant startOfNextDay(LocalDate date) {
    return date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  /** Returns the natural start of the bucket containing the date (Monday for weeks). */
  private LocalDate bucketStart(LocalDate date, Granularity granularity) {
    return switch (granularity) {
      case DAY -> date;
      case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> date.withDayOfMonth(1);
    };
  }

  /** Advances one bucket of the given granularity. */
  private LocalDate nextBucket(LocalDate period, Granularity granularity) {
    return switch (granularity) {
      case DAY -> period.plusDays(1);
      case WEEK -> period.plusWeeks(1);
      case MONTH -> period.plusMonths(1);
    };
  }

  /** Returns the later of two instants. */
  private Instant maxInstant(Instant first, Instant second) {
    return first.isAfter(second) ? first : second;
  }

  /** Returns the earlier of two instants. */
  private Instant minInstant(Instant first, Instant second) {
    return first.isBefore(second) ? first : second;
  }

  /** Best-effort vehicle lookup for reports: unknown vehicles enrich with nulls. */
  private VehicleRef fetchVehicleLenient(UUID vehicleId) {
    try {
      return vehiclesClient.getById(vehicleId);
    } catch (FeignException.NotFound e) {
      return null;
    } catch (FeignException e) {
      throw new ServiceUnavailableException("vehicles");
    }
  }
}
