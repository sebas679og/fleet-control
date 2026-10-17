package com.fleet.control.fuel.service.impl;

import com.fleet.control.fuel.models.entities.FuelRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Pure arithmetic and grouping helpers for fuel calculations. */
public final class FuelMath {

  private static final int SCALE = 2;

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private FuelMath() {}

  /** Groups records by vehicle preserving encounter order within each group. */
  public static Map<UUID, List<FuelRecord>> groupByVehicle(List<FuelRecord> records) {
    return records.stream().collect(Collectors.groupingBy(FuelRecord::getVehicleId));
  }

  /** Sums refueled liters. */
  public static BigDecimal sumLiters(List<FuelRecord> records) {
    return records.stream().map(FuelRecord::getLiters).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Sums total costs. */
  public static BigDecimal sumCost(List<FuelRecord> records) {
    return records.stream().map(FuelRecord::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Returns the odometer difference between the last and first record, minimum zero. */
  public static int odometerDistance(List<FuelRecord> records) {
    if (records.size() < 2) {
      return 0;
    }
    int first = records.get(0).getOdometerKm();
    int last = records.get(records.size() - 1).getOdometerKm();
    return Math.max(0, last - first);
  }

  /** Sums odometer distances per vehicle, ignoring non-positive diffs. */
  public static int totalDistance(List<FuelRecord> records) {
    return groupByVehicle(records).values().stream().mapToInt(FuelMath::odometerDistance).sum();
  }

  /** Computes liters x price rounded to two decimals. */
  public static BigDecimal costOf(BigDecimal liters, BigDecimal pricePerLiter) {
    return liters.multiply(pricePerLiter).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /** Computes liters x 100 / km rounded to two decimals, or null when not computable. */
  public static BigDecimal averagePer100km(BigDecimal liters, int distanceKm) {
    if (distanceKm <= 0) {
      return null;
    }
    return liters
        .multiply(ONE_HUNDRED)
        .divide(BigDecimal.valueOf(distanceKm), SCALE, RoundingMode.HALF_UP);
  }

  /** Computes the average price per liter, or null when there are no liters. */
  public static BigDecimal averagePrice(BigDecimal cost, BigDecimal liters) {
    if (liters.signum() <= 0) {
      return null;
    }
    return cost.divide(liters, SCALE, RoundingMode.HALF_UP);
  }
}
