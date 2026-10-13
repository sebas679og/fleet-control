package com.fleet.control.drivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.repository.DriverRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the demo profile Flyway seed (V2__demo_drivers.sql) against real PostgreSQL: 20
 * deterministic drivers with the documented status and licence-expiry distribution.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("demo")
class DemoDataContainerTest {

  @Autowired private DriverRepository driverRepository;

  @Test
  void seedsTwentyDriversWithDocumentedDistribution() {
    assertEquals(20, driverRepository.count());

    var driverOne =
        driverRepository.findById(UUID.fromString("a6efc71c-384f-3cb0-8f2c-6d0d82e754fe"));
    assertTrue(driverOne.isPresent());
    assertEquals("DL-DEMO-0001", driverOne.get().getLicenseNumber());

    var all = driverRepository.findAll();
    assertEquals(17, all.stream().filter(d -> d.getStatus() == DriverStatus.ACTIVE).count());
    assertEquals(2, all.stream().filter(d -> d.getStatus() == DriverStatus.ON_LEAVE).count());
    assertEquals(1, all.stream().filter(d -> d.getStatus() == DriverStatus.SUSPENDED).count());

    LocalDate today = LocalDate.now();
    assertEquals(
        3,
        all.stream()
            .filter(
                d ->
                    !d.getLicenseExpiresAt().isBefore(today)
                        && !d.getLicenseExpiresAt().isAfter(today.plusDays(30)))
            .count());
    assertEquals(1, all.stream().filter(d -> d.getLicenseExpiresAt().isBefore(today)).count());
  }
}
