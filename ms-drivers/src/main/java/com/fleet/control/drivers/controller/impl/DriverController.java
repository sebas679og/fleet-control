package com.fleet.control.drivers.controller.impl;

import com.fleet.control.drivers.controller.DriverApi;
import com.fleet.control.drivers.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.models.dtos.response.PageResponse;
import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.service.DriverService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DriverController implements DriverApi {
  private final DriverService driverService;

  @Override
  public ResponseEntity<DriverResponse> create(CreateDriverRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED).body(driverService.create(request));
  }

  @Override
  public ResponseEntity<PageResponse<DriverResponse>> list(
      DriverStatus status, Integer licenseExpiringInDays, int page, int size) {
    return ResponseEntity.ok(driverService.list(status, licenseExpiringInDays, page, size));
  }

  @Override
  public ResponseEntity<DriverResponse> getById(UUID driverId) {

    return ResponseEntity.ok(driverService.getById(driverId));
  }

  @Override
  public ResponseEntity<DriverResponse> update(UUID driverId, UpdateDriverRequest request) {

    return ResponseEntity.ok(driverService.update(driverId, request));
  }
}
