package com.fleet.control.drivers.config;

import com.fleet.control.drivers.models.entities.Driver;
import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.models.enums.LicenseCategory;
import com.fleet.control.drivers.repository.DriverRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Demo data generator: inserts 20 deterministic drivers when the table is empty.
 *
 * <p>Identifiers are derived from the seed ({@code driver-1} ... {@code driver-20}) so other
 * services can reference the same drivers without calling this service during startup. Drivers
 * 17-19 have a license expiring within 30 days and driver 20 has an already expired license. Only
 * runs with the {@code demo} profile.
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class DriverDemoSeeder implements ApplicationRunner {

  public static final int DRIVER_COUNT = 20;

  private final DriverRepository driverRepository;

  @Value("${demo.seed:42}")
  private long seed;

  @Override
  public void run(ApplicationArguments args) {
    if (driverRepository.count() > 0) {
      if (log.isInfoEnabled()) {
        log.info("Demo seeder skipped: drivers table already has data");
      }
      return;
    }
    List<Driver> drivers = buildDrivers(new Faker(new Random(seed)));
    driverRepository.saveAll(drivers);
    if (log.isInfoEnabled()) {
      log.info("Demo seeder inserted {} drivers", drivers.size());
    }
  }

  /** Builds the deterministic demo driver list. */
  public List<Driver> buildDrivers(Faker faker) {
    List<Driver> drivers = new ArrayList<>(DRIVER_COUNT);
    Set<String> licenses = new HashSet<>();
    for (int i = 1; i <= DRIVER_COUNT; i++) {
      LicenseCategory category = categoryFor(i, faker);
      drivers.add(
          Driver.builder()
              .id(UUID.nameUUIDFromBytes(("driver-" + i).getBytes(StandardCharsets.UTF_8)))
              .fullName(faker.name().fullName())
              .email("driver" + i + "@" + faker.internet().domainName().toLowerCase(Locale.ROOT))
              .phone(faker.phoneNumber().phoneNumber())
              .licenseNumber(uniqueLicense(faker, licenses, i))
              .licenseCategory(category)
              .licenseExpiresAt(expiresAtFor(i, faker))
              .status(statusFor(i))
              .build());
    }
    return drivers;
  }

  private String uniqueLicense(Faker faker, Set<String> licenses, int index) {
    String license = String.format("DL-DEMO-%04d", index);
    if (!licenses.add(license)) {
      license = "DL-" + faker.number().numberBetween(1000, 9999) + "-" + index;
      licenses.add(license);
    }
    return license;
  }

  private LicenseCategory categoryFor(int index, Faker faker) {
    if (index % 5 == 0) {
      return LicenseCategory.A;
    }
    if (index % 2 == 0) {
      return LicenseCategory.C;
    }
    return faker.options().option(LicenseCategory.B, LicenseCategory.C);
  }

  private LocalDate expiresAtFor(int index, Faker faker) {
    LocalDate now = LocalDate.now();
    if (index == DRIVER_COUNT) {
      return now.minusDays(faker.number().numberBetween(1, 20));
    }
    if (index >= DRIVER_COUNT - 3) {
      return now.plusDays(faker.number().numberBetween(1, 30));
    }
    return now.plusDays(faker.number().numberBetween(31, 1095));
  }

  private DriverStatus statusFor(int index) {
    if (index == DRIVER_COUNT - 1) {
      return DriverStatus.SUSPENDED;
    }
    if (index % 7 == 0) {
      return DriverStatus.ON_LEAVE;
    }
    return DriverStatus.ACTIVE;
  }
}
