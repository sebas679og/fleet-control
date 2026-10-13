package com.fleet.control.drivers.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleet.control.drivers.models.entities.Driver;
import com.fleet.control.drivers.models.enums.DriverStatus;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

class DriverDemoSeederTest {

  private final DriverDemoSeeder seeder = new DriverDemoSeeder(null);

  private List<Driver> buildDrivers() {
    return seeder.buildDrivers(new Faker(new Random(42)));
  }

  @Test
  void buildDriversReturnsTwentyWithDeterministicIds() {
    List<Driver> drivers = buildDrivers();

    assertEquals(DriverDemoSeeder.DRIVER_COUNT, drivers.size());
    for (int i = 1; i <= DriverDemoSeeder.DRIVER_COUNT; i++) {
      UUID expected = UUID.nameUUIDFromBytes(("driver-" + i).getBytes(StandardCharsets.UTF_8));
      assertEquals(expected, drivers.get(i - 1).getId());
    }
  }

  @Test
  void buildDriversUsesUniqueLicenses() {
    List<Driver> drivers = buildDrivers();

    long distinct = drivers.stream().map(Driver::getLicenseNumber).distinct().count();
    assertEquals(drivers.size(), distinct);
  }

  @Test
  void buildDriversMatchesExpiryDistribution() {
    List<Driver> drivers = buildDrivers();
    LocalDate now = LocalDate.now();

    long expired = drivers.stream().filter(d -> d.getLicenseExpiresAt().isBefore(now)).count();
    long expiringSoon =
        drivers.stream()
            .filter(
                d ->
                    !d.getLicenseExpiresAt().isBefore(now)
                        && d.getLicenseExpiresAt().isBefore(now.plusDays(31)))
            .count();

    assertEquals(1, expired);
    assertEquals(3, expiringSoon);
  }

  @Test
  void buildDriversMatchesStatusDistribution() {
    List<Driver> drivers = buildDrivers();

    long active = drivers.stream().filter(d -> d.getStatus() == DriverStatus.ACTIVE).count();
    long onLeave = drivers.stream().filter(d -> d.getStatus() == DriverStatus.ON_LEAVE).count();
    long suspended = drivers.stream().filter(d -> d.getStatus() == DriverStatus.SUSPENDED).count();

    assertEquals(DriverDemoSeeder.DRIVER_COUNT, active + onLeave + suspended);
    assertTrue(active >= 15);
    assertTrue(suspended >= 1);
  }
}
