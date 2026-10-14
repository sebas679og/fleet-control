package com.fleet.control.vehicles.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fleet.control.vehicles.common.exceptions.ApiVehicleException;
import com.fleet.control.vehicles.common.exceptions.enums.ErrorCode;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateStatusRequest;
import com.fleet.control.vehicles.common.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.common.models.entities.Vehicle;
import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import com.fleet.control.vehicles.common.models.mappers.VehicleMapper;
import com.fleet.control.vehicles.repository.VehicleRepository;
import com.fleet.control.vehicles.service.impl.VehicleServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for the vehicle status user story: allowed transitions, odometer advance, ADMIN-only
 * reactivation and 404.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceImplStatusTest {

  @Mock private VehicleRepository vehicleRepository;

  @Mock private VehicleMapper vehicleMapper;

  @InjectMocks private VehicleServiceImpl service;

  private static final int VALID_YEAR = LocalDate.now().getYear();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static Stream<Arguments> allowedTransitions() {
    return Stream.of(
        Arguments.of(VehicleStatus.AVAILABLE, VehicleStatus.IN_USE),
        Arguments.of(VehicleStatus.AVAILABLE, VehicleStatus.IN_MAINTENANCE),
        Arguments.of(VehicleStatus.AVAILABLE, VehicleStatus.OUT_OF_SERVICE),
        Arguments.of(VehicleStatus.IN_USE, VehicleStatus.AVAILABLE),
        Arguments.of(VehicleStatus.IN_USE, VehicleStatus.OUT_OF_SERVICE),
        Arguments.of(VehicleStatus.IN_MAINTENANCE, VehicleStatus.AVAILABLE),
        Arguments.of(VehicleStatus.IN_MAINTENANCE, VehicleStatus.OUT_OF_SERVICE));
  }

  private Vehicle vehicle(UUID id, VehicleStatus status, int odometerKm) {
    return Vehicle.builder().id(id).plate("4821-KDF").status(status).odometerKm(odometerKm).build();
  }

  private VehicleResponse vehicleResponse(UUID id, VehicleStatus status, int odometerKm) {
    return new VehicleResponse(
        id,
        "4821-KDF",
        "Ford",
        "Transit",
        VALID_YEAR,
        VehicleType.VAN,
        FuelType.DIESEL,
        80,
        odometerKm,
        status,
        null,
        null);
  }

  private void authenticateAs(String role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  @ParameterizedTest
  @MethodSource("allowedTransitions")
  void allowedTransitionUpdatesStatus(VehicleStatus from, VehicleStatus to) {
    UUID id = UUID.randomUUID();
    Vehicle found = vehicle(id, from, 45210);
    given(vehicleRepository.findById(id)).willReturn(Optional.of(found));
    given(vehicleRepository.save(found)).willReturn(found);
    given(vehicleMapper.toResponse(any(Vehicle.class))).willReturn(vehicleResponse(id, to, 45210));

    VehicleResponse result = service.updateStatus(id, new UpdateStatusRequest(to, null));

    assertThat(result.status()).isEqualTo(to);
    assertThat(found.getStatus()).isEqualTo(to);
    verify(vehicleRepository).save(found);
  }

  @Test
  void reactivationRequiresAdmin() {
    UUID id = UUID.randomUUID();
    authenticateAs("ADMIN");
    Vehicle found = vehicle(id, VehicleStatus.OUT_OF_SERVICE, 45210);
    given(vehicleRepository.findById(id)).willReturn(Optional.of(found));
    given(vehicleRepository.save(found)).willReturn(found);
    given(vehicleMapper.toResponse(any(Vehicle.class)))
        .willReturn(vehicleResponse(id, VehicleStatus.AVAILABLE, 45210));

    VehicleResponse result =
        service.updateStatus(id, new UpdateStatusRequest(VehicleStatus.AVAILABLE, null));

    assertThat(result.status()).isEqualTo(VehicleStatus.AVAILABLE);
  }

  @Test
  void managerReactivationReturns403() {
    UUID id = UUID.randomUUID();
    authenticateAs("MANAGER");
    given(vehicleRepository.findById(id))
        .willReturn(Optional.of(vehicle(id, VehicleStatus.OUT_OF_SERVICE, 45210)));

    assertThrows(
        AccessDeniedException.class,
        () -> service.updateStatus(id, new UpdateStatusRequest(VehicleStatus.AVAILABLE, null)));
    verify(vehicleRepository, never()).save(any(Vehicle.class));
  }

  @Test
  void disallowedTransitionReturns409() {
    UUID id = UUID.randomUUID();
    given(vehicleRepository.findById(id))
        .willReturn(Optional.of(vehicle(id, VehicleStatus.IN_USE, 45210)));

    var ex =
        assertThrows(
            ApiVehicleException.InvalidStatusTransitionException.class,
            () ->
                service.updateStatus(
                    id, new UpdateStatusRequest(VehicleStatus.IN_MAINTENANCE, null)));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    verify(vehicleRepository, never()).save(any(Vehicle.class));
  }

  @Test
  void sameStatusReturns409() {
    UUID id = UUID.randomUUID();
    given(vehicleRepository.findById(id))
        .willReturn(Optional.of(vehicle(id, VehicleStatus.AVAILABLE, 45210)));

    assertThrows(
        ApiVehicleException.InvalidStatusTransitionException.class,
        () -> service.updateStatus(id, new UpdateStatusRequest(VehicleStatus.AVAILABLE, null)));
  }

  @Test
  void lowerOdometerReturns409() {
    UUID id = UUID.randomUUID();
    given(vehicleRepository.findById(id))
        .willReturn(Optional.of(vehicle(id, VehicleStatus.AVAILABLE, 45210)));

    var ex =
        assertThrows(
            ApiVehicleException.InvalidOdometerException.class,
            () -> service.updateStatus(id, new UpdateStatusRequest(VehicleStatus.IN_USE, 45209)));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_ODOMETER);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    verify(vehicleRepository, never()).save(any(Vehicle.class));
  }

  @Test
  void equalOdometerIsSaved() {
    UUID id = UUID.randomUUID();
    Vehicle found = vehicle(id, VehicleStatus.AVAILABLE, 45210);
    given(vehicleRepository.findById(id)).willReturn(Optional.of(found));
    given(vehicleRepository.save(found)).willReturn(found);
    given(vehicleMapper.toResponse(any(Vehicle.class)))
        .willReturn(vehicleResponse(id, VehicleStatus.IN_USE, 45210));

    VehicleResponse result =
        service.updateStatus(id, new UpdateStatusRequest(VehicleStatus.IN_USE, 45210));

    ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
    verify(vehicleRepository).save(captor.capture());
    assertThat(captor.getValue().getOdometerKm()).isEqualTo(45210);
    assertThat(result.status()).isEqualTo(VehicleStatus.IN_USE);
  }

  @Test
  void higherOdometerIsSaved() {
    UUID id = UUID.randomUUID();
    Vehicle found = vehicle(id, VehicleStatus.AVAILABLE, 45210);
    given(vehicleRepository.findById(id)).willReturn(Optional.of(found));
    given(vehicleRepository.save(found)).willReturn(found);
    given(vehicleMapper.toResponse(any(Vehicle.class)))
        .willReturn(vehicleResponse(id, VehicleStatus.IN_USE, 45310));

    VehicleResponse result =
        service.updateStatus(id, new UpdateStatusRequest(VehicleStatus.IN_USE, 45310));

    assertThat(found.getOdometerKm()).isEqualTo(45310);
    assertThat(result.status()).isEqualTo(VehicleStatus.IN_USE);
  }

  @Test
  void unknownVehicleReturns404() {
    UUID id = UUID.randomUUID();
    given(vehicleRepository.findById(id)).willReturn(Optional.empty());

    var ex =
        assertThrows(
            ApiVehicleException.VehicleNotFoundException.class,
            () -> service.updateStatus(id, new UpdateStatusRequest(VehicleStatus.IN_USE, null)));

    assertThat(ex.getCode()).isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
