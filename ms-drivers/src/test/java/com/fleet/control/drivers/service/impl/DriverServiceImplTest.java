package com.fleet.control.drivers.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleet.control.drivers.commons.exception.exception.DriverAlreadyExistsException;
import com.fleet.control.drivers.commons.exception.exception.DriverNotFoundException;
import com.fleet.control.drivers.commons.exception.exception.ValidationException;
import com.fleet.control.drivers.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.models.dtos.response.PageResponse;
import com.fleet.control.drivers.models.entities.Driver;
import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.models.enums.LicenseCategory;
import com.fleet.control.drivers.models.mappers.DriverMapper;
import com.fleet.control.drivers.repository.DriverRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

  @Mock private DriverRepository driverRepository;

  @Spy private DriverMapper driverMapper = Mappers.getMapper(DriverMapper.class);

  @InjectMocks private DriverServiceImpl driverService;

  private static CreateDriverRequest createRequest(String license) {
    return new CreateDriverRequest(
        "Jane Doe",
        "jane.doe@example.com",
        "+34 600 123 456",
        license,
        LicenseCategory.C,
        LocalDate.now().plusYears(2));
  }

  private static Driver storedDriver(UUID id, String license) {
    Driver driver = Mappers.getMapper(DriverMapper.class).toEntity(createRequest(license));
    driver.setId(id);
    driver.setStatus(DriverStatus.ACTIVE);
    return driver;
  }

  @Test
  void createForcesActiveStatus() {
    when(driverRepository.existsByLicenseNumber("DL-1")).thenReturn(false);
    when(driverRepository.save(any(Driver.class)))
        .thenAnswer(
            invocation -> {
              Driver driver = invocation.getArgument(0);
              driver.setId(UUID.randomUUID());
              return driver;
            });

    DriverResponse response = driverService.create(createRequest("DL-1"));

    assertEquals(DriverStatus.ACTIVE, response.status());
    assertEquals("DL-1", response.licenseNumber());
  }

  @Test
  void createWithDuplicateLicenseThrows() {
    when(driverRepository.existsByLicenseNumber("DL-1")).thenReturn(true);

    assertThrows(
        DriverAlreadyExistsException.class, () -> driverService.create(createRequest("DL-1")));
    verify(driverRepository, never()).save(any(Driver.class));
  }

  @Test
  void getByIdReturnsDriver() {
    UUID id = UUID.randomUUID();
    when(driverRepository.findById(id)).thenReturn(Optional.of(storedDriver(id, "DL-1")));

    DriverResponse response = driverService.getById(id);

    assertEquals(id, response.id());
    assertEquals("jane.doe@example.com", response.email());
  }

  @Test
  void getByIdUnknownThrows() {
    UUID id = UUID.randomUUID();
    when(driverRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(DriverNotFoundException.class, () -> driverService.getById(id));
  }

  @Test
  void updateAppliesAllFields() {
    UUID id = UUID.randomUUID();
    Driver stored = storedDriver(id, "DL-1");
    when(driverRepository.findById(id)).thenReturn(Optional.of(stored));
    when(driverRepository.existsByLicenseNumberAndIdNot("DL-2", id)).thenReturn(false);
    when(driverRepository.save(any(Driver.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateDriverRequest request =
        new UpdateDriverRequest(
            "Jane Doe",
            "jane.doe@example.com",
            "+34 600 999 999",
            "DL-2",
            LicenseCategory.C,
            LocalDate.now().plusYears(3),
            DriverStatus.SUSPENDED);

    DriverResponse response = driverService.update(id, request);

    assertEquals("+34 600 999 999", response.phone());
    assertEquals("DL-2", response.licenseNumber());
    assertEquals(DriverStatus.SUSPENDED, response.status());
  }

  @Test
  void updateUnknownThrows() {
    UUID id = UUID.randomUUID();
    when(driverRepository.findById(id)).thenReturn(Optional.empty());

    UpdateDriverRequest request =
        new UpdateDriverRequest(
            "Jane Doe",
            "jane.doe@example.com",
            "+34 600 999 999",
            "DL-2",
            LicenseCategory.C,
            LocalDate.now().plusYears(3),
            DriverStatus.ACTIVE);

    assertThrows(DriverNotFoundException.class, () -> driverService.update(id, request));
  }

  @Test
  void updateWithDuplicateLicenseThrows() {
    UUID id = UUID.randomUUID();
    when(driverRepository.findById(id)).thenReturn(Optional.of(storedDriver(id, "DL-1")));
    when(driverRepository.existsByLicenseNumberAndIdNot("DL-2", id)).thenReturn(true);

    UpdateDriverRequest request =
        new UpdateDriverRequest(
            "Jane Doe",
            "jane.doe@example.com",
            "+34 600 999 999",
            "DL-2",
            LicenseCategory.C,
            LocalDate.now().plusYears(3),
            DriverStatus.ACTIVE);

    assertThrows(DriverAlreadyExistsException.class, () -> driverService.update(id, request));
  }

  @Test
  void listReturnsPage() {
    UUID id = UUID.randomUUID();
    Driver stored = storedDriver(id, "DL-1");
    when(driverRepository.search(any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(stored), PageRequest.of(0, 20), 1));

    PageResponse<DriverResponse> page = driverService.list(null, null, 0, 20);

    assertEquals(1, page.content().size());
    assertEquals(0, page.page());
    assertEquals(1, page.totalElements());
    assertEquals(1, page.totalPages());
  }

  @Test
  void listWithNegativePageThrows() {
    assertThrows(ValidationException.class, () -> driverService.list(null, null, -1, 20));
    verify(driverRepository, never()).search(any(), any(), any());
  }

  @Test
  void listWithZeroSizeThrows() {
    assertThrows(ValidationException.class, () -> driverService.list(null, null, 0, 0));
    verify(driverRepository, never()).search(any(), any(), any());
  }

  @Test
  void listWithOversizedPageThrows() {
    assertThrows(ValidationException.class, () -> driverService.list(null, null, 0, 101));
    verify(driverRepository, never()).search(any(), any(), any());
  }
}
