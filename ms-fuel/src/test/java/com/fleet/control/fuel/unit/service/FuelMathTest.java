package com.fleet.control.fuel.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fleet.control.fuel.models.entities.FuelRecord;
import com.fleet.control.fuel.service.impl.FuelMath;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Happy-path unit tests for the pure fuel arithmetic helpers. */
class FuelMathTest {

  private static FuelRecord record(UUID vehicleId, String liters, String cost, int odometer) {
    return FuelRecord.builder()
        .vehicleId(vehicleId)
        .refueledAt(Instant.parse("2026-10-01T08:00:00Z"))
        .liters(new BigDecimal(liters))
        .totalCost(new BigDecimal(cost))
        .odometerKm(odometer)
        .fullTank(true)
        .build();
  }

  @Test
  void costOfMultipliesLitersByPrice() {
    assertEquals(
        new BigDecimal("61.56"), FuelMath.costOf(new BigDecimal("38"), new BigDecimal("1.62")));
  }

  @Test
  void averagePer100kmAppliesDocumentedFormula() {
    assertEquals(new BigDecimal("11.60"), FuelMath.averagePer100km(new BigDecimal("58"), 500));
  }

  @Test
  void averagePer100kmReturnsNullWithoutDistance() {
    assertNull(FuelMath.averagePer100km(new BigDecimal("58"), 0));
  }

  @Test
  void odometerDistanceUsesFirstAndLastRecord() {
    UUID vehicleId = UUID.randomUUID();
    List<FuelRecord> records =
        List.of(record(vehicleId, "20", "32.40", 45000), record(vehicleId, "38", "61.56", 45500));

    assertEquals(500, FuelMath.odometerDistance(records));
  }

  @Test
  void odometerDistanceNeedsTwoRecords() {
    UUID vehicleId = UUID.randomUUID();

    assertEquals(0, FuelMath.odometerDistance(List.of(record(vehicleId, "20", "32.40", 45000))));
  }

  @Test
  void totalsAggregateLitersCostAndAverages() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    List<FuelRecord> records =
        List.of(
            record(first, "30", "48.60", 1000),
            record(first, "20", "33.00", 1500),
            record(second, "10", "16.20", 5000),
            record(second, "10", "16.50", 5200));

    assertEquals(new BigDecimal("70"), FuelMath.sumLiters(records));
    assertEquals(new BigDecimal("114.30"), FuelMath.sumCost(records));
    assertEquals(700, FuelMath.totalDistance(records));
    assertEquals(
        new BigDecimal("1.63"),
        FuelMath.averagePrice(new BigDecimal("114.30"), new BigDecimal("70")));
  }
}
