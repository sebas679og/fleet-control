package com.fleet.control.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.repository.VehicleRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the demo profile Flyway seed (V2__demo_vehicles.sql) against real PostgreSQL: 25
 * deterministic vehicles with the documented type and status distribution.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("demo")
class DemoDataContainerTest {

  @Autowired private VehicleRepository vehicleRepository;

  @Test
  void seedsTwentyFiveVehiclesWithDocumentedDistribution() {
    assertEquals(25, vehicleRepository.count());

    var vehicleOne =
        vehicleRepository.findById(UUID.fromString("99b5202f-f181-3828-9461-3571f0f67f94"));
    assertTrue(vehicleOne.isPresent());
    assertEquals("2137KLM", vehicleOne.get().getPlate());

    var all = vehicleRepository.findAll();
    assertEquals(14, all.stream().filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count());
    assertEquals(8, all.stream().filter(v -> v.getStatus() == VehicleStatus.IN_USE).count());
    assertEquals(
        2, all.stream().filter(v -> v.getStatus() == VehicleStatus.IN_MAINTENANCE).count());
    assertEquals(
        1, all.stream().filter(v -> v.getStatus() == VehicleStatus.OUT_OF_SERVICE).count());
  }
}
