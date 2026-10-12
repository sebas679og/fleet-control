package com.fleet.control.vehicles.config;

import com.fleet.control.vehicles.common.models.entities.Vehicle;
import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import com.fleet.control.vehicles.repository.VehicleRepository;
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
 * Demo data generator: inserts 25 deterministic vehicles when the table is empty.
 *
 * <p>Identifiers are derived from the seed ({@code vehicle-1} ... {@code vehicle-25})
 * so other services can reference the same vehicles without calling this service
 * during startup. Only runs with the {@code demo} profile.
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class VehicleDemoSeeder implements ApplicationRunner {

  public static final int VEHICLE_COUNT = 25;

  private static final List<VehicleType> TYPES = List.of(
      VehicleType.CAR, VehicleType.CAR, VehicleType.CAR, VehicleType.CAR, VehicleType.CAR,
      VehicleType.CAR, VehicleType.CAR, VehicleType.CAR, VehicleType.CAR,
      VehicleType.VAN, VehicleType.VAN, VehicleType.VAN, VehicleType.VAN, VehicleType.VAN,
      VehicleType.VAN, VehicleType.VAN, VehicleType.VAN, VehicleType.VAN, VehicleType.VAN,
      VehicleType.TRUCK, VehicleType.TRUCK, VehicleType.TRUCK, VehicleType.TRUCK,
      VehicleType.MOTORCYCLE, VehicleType.MOTORCYCLE);

  private static final List<VehicleStatus> STATUSES = List.of(
      VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE,
      VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE,
      VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE,
      VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE,
      VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE,
      VehicleStatus.IN_USE, VehicleStatus.IN_USE, VehicleStatus.IN_USE, VehicleStatus.IN_USE,
      VehicleStatus.IN_USE, VehicleStatus.IN_USE, VehicleStatus.IN_USE, VehicleStatus.IN_USE,
      VehicleStatus.IN_MAINTENANCE, VehicleStatus.IN_MAINTENANCE,
      VehicleStatus.OUT_OF_SERVICE);

  private final VehicleRepository vehicleRepository;

  @Value("${demo.seed:42}")
  private long seed;

  @Override
  public void run(ApplicationArguments args) {
    if (vehicleRepository.count() > 0) {
      if (log.isInfoEnabled()) {
        log.info("Demo seeder skipped: vehicles table already has data");
      }
      return;
    }
    List<Vehicle> vehicles = buildVehicles(new Faker(new Random(seed)));
    vehicleRepository.saveAll(vehicles);
    if (log.isInfoEnabled()) {
      log.info("Demo seeder inserted {} vehicles", vehicles.size());
    }
  }

  public List<Vehicle> buildVehicles(Faker faker) {
    List<Vehicle> vehicles = new ArrayList<>(VEHICLE_COUNT);
    Set<String> plates = new HashSet<>();
    for (int i = 1; i <= VEHICLE_COUNT; i++) {
      VehicleType type = TYPES.get(i - 1);
      vehicles.add(Vehicle.builder()
          .id(UUID.nameUUIDFromBytes(("vehicle-" + i).getBytes(StandardCharsets.UTF_8)))
          .plate(uniquePlate(faker, plates))
          .make(makeFor(type, faker))
          .model("Model-" + i)
          .year(faker.number().numberBetween(2015, LocalDate.now().getYear() + 2))
          .type(type)
          .fuelType(fuelFor(type, faker))
          .tankCapacityL(tankFor(type, faker))
          .odometerKm(faker.number().numberBetween(5_000, 200_000))
          .status(STATUSES.get(i - 1))
          .build());
    }
    return vehicles;
  }

  private String uniquePlate(Faker faker, Set<String> plates) {
    String plate;
    do {
      plate = String.format("%04d-%s%s%s",
          faker.number().numberBetween(0, 10_000),
          faker.lorem().characters(1).toUpperCase(Locale.ROOT),
          faker.lorem().characters(1).toUpperCase(Locale.ROOT),
          faker.lorem().characters(1).toUpperCase(Locale.ROOT));
    } while (!plates.add(plate));
    return plate;
  }

  private String makeFor(VehicleType type, Faker faker) {
    return switch (type) {
      case CAR -> faker.options().option("Renault", "Toyota", "Volkswagen", "Seat");
      case VAN -> faker.options().option("Ford", "Mercedes-Benz", "Citroen", "Peugeot");
      case TRUCK -> faker.options().option("Mercedes-Benz", "Volvo", "Scania", "MAN");
      case MOTORCYCLE -> faker.options().option("Honda", "Yamaha", "Kawasaki");
    };
  }

  private FuelType fuelFor(VehicleType type, Faker faker) {
    return switch (type) {
      case TRUCK -> FuelType.DIESEL;
      case MOTORCYCLE -> faker.options().option(FuelType.GASOLINE, FuelType.HYBRID);
      default -> faker.options().option(FuelType.DIESEL, FuelType.GASOLINE, FuelType.LPG,
          FuelType.HYBRID);
    };
  }

  private int tankFor(VehicleType type, Faker faker) {
    return switch (type) {
      case CAR -> faker.number().numberBetween(40, 60);
      case VAN -> faker.number().numberBetween(70, 90);
      case TRUCK -> faker.number().numberBetween(350, 450);
      case MOTORCYCLE -> faker.number().numberBetween(12, 20);
    };
  }
}
