package com.fleet.control.drivers.service.impl;

import com.fleet.control.drivers.commons.exception.exception.DriverAlreadyExistsException;
import com.fleet.control.drivers.commons.exception.exception.DriverNotFoundException;
import com.fleet.control.drivers.commons.exception.exception.ValidationException;
import com.fleet.control.drivers.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.models.dtos.response.PageResponse;
import com.fleet.control.drivers.models.entities.Driver;
import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.models.mappers.DriverMapper;
import com.fleet.control.drivers.repository.DriverRepository;
import com.fleet.control.drivers.service.DriverService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default implementation of {@link DriverService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverServiceImpl implements DriverService {

  private static final int MAX_PAGE_SIZE = 100;

  private final DriverRepository driverRepository;
  private final DriverMapper driverMapper;

  @Override
  @Transactional
  public DriverResponse create(CreateDriverRequest request) {
    if (driverRepository.existsByLicenseNumber(request.licenseNumber())) {
      throw new DriverAlreadyExistsException(request.licenseNumber());
    }

    Driver driver = driverMapper.toEntity(request);
    driver.setStatus(DriverStatus.ACTIVE);

    return driverMapper.toResponse(driverRepository.save(driver));
  }

  @Override
  public PageResponse<DriverResponse> list(
      DriverStatus status, Integer licenseExpiringInDays, int page, int size) {

    if (page < 0) {
      throw new ValidationException("page", "Page index must be zero or greater");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new ValidationException("size", "Page size must be between 1 and " + MAX_PAGE_SIZE);
    }

    LocalDate expiresBefore =
        licenseExpiringInDays == null ? null : LocalDate.now().plusDays(licenseExpiringInDays);

    Pageable pageable = PageRequest.of(page, size);
    Page<Driver> result = driverRepository.search(status, expiresBefore, pageable);

    return PageResponse.of(
        result.getContent().stream().map(driverMapper::toResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements());
  }

  @Override
  public DriverResponse getById(UUID id) {
    return driverRepository
        .findById(id)
        .map(driverMapper::toResponse)
        .orElseThrow(() -> new DriverNotFoundException(id));
  }

  @Override
  @Transactional
  public DriverResponse update(UUID id, UpdateDriverRequest request) {
    Driver driver =
        driverRepository.findById(id).orElseThrow(() -> new DriverNotFoundException(id));

    if (driverRepository.existsByLicenseNumberAndIdNot(request.licenseNumber(), id)) {
      throw new DriverAlreadyExistsException(request.licenseNumber());
    }

    driverMapper.updateToEntity(request, driver);

    return driverMapper.toResponse(driverRepository.save(driver));
  }
}
