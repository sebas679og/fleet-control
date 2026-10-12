package com.fleet.control.vehicles.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fleet.control.vehicles.common.models.entities.Vehicle;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import com.fleet.control.vehicles.config.VehicleDemoSeeder;
import com.fleet.control.vehicles.repository.VehicleRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the demo seeder: idempotency, volume, distribution and determinism. */
@ExtendWith(MockitoExtension.class)
class VehicleDemoSeederTest {

  @Mock private VehicleRepository vehicleRepository;

  private VehicleDemoSeeder seeder() {
    VehicleDemoSeeder seeder = new VehicleDemoSeeder(vehicleRepository);
    ReflectionTestUtils.setField(seeder, "seed", 42L);
    return seeder;
  }

  @Test
  void skipsWhenTableHasData() {
    given(vehicleRepository.count()).willReturn(3L);

    seeder().run(null);

    verify(vehicleRepository, never()).saveAll(anyList());
  }

  @Test
  void inserts25VehiclesWhenEmpty() {
    given(vehicleRepository.count()).willReturn(0L);

    seeder().run(null);

    ArgumentCaptor<List<Vehicle>> captor = ArgumentCaptor.forClass(List.class);
    verify(vehicleRepository).saveAll(captor.capture());
    List<Vehicle> saved = captor.getValue();
    assertThat(saved).hasSize(VehicleDemoSeeder.VEHICLE_COUNT);
    assertThat(saved).extracting(Vehicle::getPlate).doesNotHaveDuplicates();
    assertThat(saved).extracting(Vehicle::getId).doesNotHaveDuplicates();

    Map<VehicleStatus, Long> byStatus =
        saved.stream().collect(Collectors.groupingBy(Vehicle::getStatus, Collectors.counting()));
    assertThat(byStatus.get(VehicleStatus.AVAILABLE)).isEqualTo(14L);
    assertThat(byStatus.get(VehicleStatus.IN_USE)).isEqualTo(8L);
    assertThat(byStatus.get(VehicleStatus.IN_MAINTENANCE)).isEqualTo(2L);
    assertThat(byStatus.get(VehicleStatus.OUT_OF_SERVICE)).isEqualTo(1L);

    Map<VehicleType, Long> byType =
        saved.stream().collect(Collectors.groupingBy(Vehicle::getType, Collectors.counting()));
    assertThat(byType.get(VehicleType.CAR)).isEqualTo(9L);
    assertThat(byType.get(VehicleType.VAN)).isEqualTo(10L);
    assertThat(byType.get(VehicleType.TRUCK)).isEqualTo(4L);
    assertThat(byType.get(VehicleType.MOTORCYCLE)).isEqualTo(2L);
  }

  @Test
  void sameSeedProducesSameVehicles() {
    List<Vehicle> first = seeder().buildVehicles(new Faker(new Random(42L)));
    List<Vehicle> second = seeder().buildVehicles(new Faker(new Random(42L)));

    assertThat(second).extracting(Vehicle::getId)
        .containsExactlyElementsOf(first.stream().map(Vehicle::getId).toList());
    assertThat(second).extracting(Vehicle::getPlate)
        .containsExactlyElementsOf(first.stream().map(Vehicle::getPlate).toList());
    assertThat(first.get(0).getId())
        .isEqualTo(UUID.nameUUIDFromBytes("vehicle-1".getBytes(StandardCharsets.UTF_8)));
  }
}
