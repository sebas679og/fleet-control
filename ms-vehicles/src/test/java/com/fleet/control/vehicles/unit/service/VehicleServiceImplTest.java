package com.fleet.control.vehicles.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fleet.control.vehicles.common.exceptions.ApiVehicleException;
import com.fleet.control.vehicles.common.exceptions.enums.ErrorCode;
import com.fleet.control.vehicles.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.models.entities.Vehicle;
import com.fleet.control.vehicles.models.enums.FuelType;
import com.fleet.control.vehicles.models.enums.VehicleStatus;
import com.fleet.control.vehicles.models.enums.VehicleType;
import com.fleet.control.vehicles.models.mappers.VehicleMapper;
import com.fleet.control.vehicles.models.mappers.VehicleMapperImpl;
import com.fleet.control.vehicles.repository.VehicleRepository;
import com.fleet.control.vehicles.service.impl.VehicleServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for the fleet inventory user story only: create (AVAILABLE / 409 duplicate), paginated
 * list with filters, detail / 404, update keeping plate-status-odometer, and 400 VALIDATION_ERROR
 * with details for out-of-range year.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

  @Mock private VehicleRepository vehicleRepository;

  @Mock private VehicleMapper vehicleMapper;

  @InjectMocks private VehicleServiceImpl service;

  private static final int VALID_YEAR = LocalDate.now().getYear();

  private CreateVehicleRequest validCreateRequest() {
    return new CreateVehicleRequest(
        "4821-KDF", "Ford", "Transit", VALID_YEAR, VehicleType.VAN, FuelType.DIESEL, 80, 45210);
  }

  private VehicleResponse vehicleResponse(UUID id, String plate, VehicleStatus status) {
    return new VehicleResponse(
        id,
        plate,
        "Ford",
        "Transit",
        VALID_YEAR,
        VehicleType.VAN,
        FuelType.DIESEL,
        80,
        45210,
        status,
        null,
        null);
  }

  @Test
  void createSetsAvailableStatus() {
    var request = validCreateRequest();
    var toSave = Vehicle.builder().plate(request.plate()).build();
    given(vehicleRepository.existsByPlate(request.plate())).willReturn(false);
    given(vehicleMapper.toEntity(request)).willReturn(toSave);
    given(vehicleRepository.save(toSave))
        .willReturn(
            Vehicle.builder().plate(request.plate()).status(VehicleStatus.AVAILABLE).build());
    given(vehicleMapper.toResponse(any(Vehicle.class)))
        .willReturn(vehicleResponse(UUID.randomUUID(), request.plate(), VehicleStatus.AVAILABLE));

    VehicleResponse result = service.create(request);

    assertThat(result.status()).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(toSave.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    verify(vehicleRepository).save(toSave);
  }

  @Test
  void createWithDuplicatePlateReturns409() {
    var request = validCreateRequest();
    given(vehicleRepository.existsByPlate(request.plate())).willReturn(true);

    var ex =
        assertThrows(
            ApiVehicleException.VehicleAlreadyExistsException.class, () -> service.create(request));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VEHICLE_ALREADY_EXISTS);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void listForwardsStatusTypeAndPlateFilters() {
    var vehicle = Vehicle.builder().plate("4821-KDF").build();
    var response = vehicleResponse(UUID.randomUUID(), "4821-KDF", VehicleStatus.AVAILABLE);
    var page = new PageImpl<>(List.of(vehicle));
    given(vehicleRepository.search(any(), any(), any(), any(Pageable.class))).willReturn(page);
    given(vehicleMapper.toResponse(vehicle)).willReturn(response);

    var result = service.list(VehicleStatus.AVAILABLE, VehicleType.VAN, "4821", 0, 20);

    assertThat(result.content()).containsExactly(response);
    ArgumentCaptor<VehicleStatus> statusCaptor = ArgumentCaptor.forClass(VehicleStatus.class);
    ArgumentCaptor<VehicleType> typeCaptor = ArgumentCaptor.forClass(VehicleType.class);
    ArgumentCaptor<String> plateCaptor = ArgumentCaptor.forClass(String.class);
    verify(vehicleRepository)
        .search(
            statusCaptor.capture(),
            typeCaptor.capture(),
            plateCaptor.capture(),
            any(Pageable.class));
    assertThat(statusCaptor.getValue()).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(typeCaptor.getValue()).isEqualTo(VehicleType.VAN);
    assertThat(plateCaptor.getValue()).isEqualTo("4821");
  }

  @Test
  void getByIdReturnsDetail() {
    UUID id = UUID.randomUUID();
    var vehicle = Vehicle.builder().id(id).build();
    var response = vehicleResponse(id, "4821-KDF", VehicleStatus.AVAILABLE);
    given(vehicleRepository.findById(id)).willReturn(Optional.of(vehicle));
    given(vehicleMapper.toResponse(vehicle)).willReturn(response);

    assertThat(service.getById(id)).isEqualTo(response);
  }

  @Test
  void getByIdNotFoundReturns404() {
    UUID id = UUID.randomUUID();
    given(vehicleRepository.findById(id)).willReturn(Optional.empty());

    var ex =
        assertThrows(ApiVehicleException.VehicleNotFoundException.class, () -> service.getById(id));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updateKeepsPlateStatusAndOdometer() {
    // Real mapper: proves @Mapping(ignore) on plate/status/odometer is honoured.
    var realMapper = new VehicleMapperImpl();
    var serviceWithRealMapper = new VehicleServiceImpl(vehicleRepository, realMapper);
    UUID id = UUID.randomUUID();
    var stored =
        Vehicle.builder()
            .id(id)
            .plate("ORIGINAL")
            .make("Ford")
            .model("Transit")
            .year(2020)
            .type(VehicleType.VAN)
            .fuelType(FuelType.DIESEL)
            .tankCapacityL(80)
            .odometerKm(50000)
            .status(VehicleStatus.IN_USE)
            .build();
    var request =
        new UpdateVehicleRequest(
            "Mercedes", "Sprinter", VALID_YEAR, VehicleType.TRUCK, FuelType.DIESEL, 120);
    given(vehicleRepository.findById(id)).willReturn(Optional.of(stored));
    given(vehicleRepository.save(any(Vehicle.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    serviceWithRealMapper.update(id, request);

    ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
    verify(vehicleRepository).save(captor.capture());
    Vehicle saved = captor.getValue();
    assertThat(saved.getPlate()).isEqualTo("ORIGINAL");
    assertThat(saved.getStatus()).isEqualTo(VehicleStatus.IN_USE);
    assertThat(saved.getOdometerKm()).isEqualTo(50000);
    assertThat(saved.getMake()).isEqualTo("Mercedes");
    assertThat(saved.getTankCapacityL()).isEqualTo(120);
  }

  @Test
  void updateNotFoundReturns404() {
    UUID id = UUID.randomUUID();
    var request =
        new UpdateVehicleRequest(
            "Ford", "Transit", VALID_YEAR, VehicleType.VAN, FuelType.DIESEL, 80);
    given(vehicleRepository.findById(id)).willReturn(Optional.empty());

    var ex =
        assertThrows(
            ApiVehicleException.VehicleNotFoundException.class, () -> service.update(id, request));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
  }

  @Test
  void createWithYearOutOfRangeReturns400WithDetails() {
    var request =
        new CreateVehicleRequest(
            "4821-KDF", "Ford", "Transit", 1800, VehicleType.VAN, FuelType.DIESEL, 80, 0);
    given(vehicleRepository.existsByPlate(request.plate())).willReturn(false);

    var ex =
        assertThrows(
            ApiVehicleException.InvalidVehicleYearException.class, () -> service.create(request));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getDetails()).hasSize(1);
    assertThat(ex.getDetails().get(0).field()).isEqualTo("year");
  }

  @Test
  void listWithNegativePageReturns400WithDetails() {
    var ex =
        assertThrows(
            ApiVehicleException.InvalidPaginationException.class,
            () -> service.list(null, null, null, -1, 20));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getDetails().get(0).field()).isEqualTo("page");
  }
}
