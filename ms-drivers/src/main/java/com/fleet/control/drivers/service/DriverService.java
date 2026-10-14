package com.fleet.control.drivers.service;

import com.fleet.control.drivers.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.models.dtos.response.PageResponse;
import com.fleet.control.drivers.models.enums.DriverStatus;
import java.util.UUID;

/** Driver management operations: create, list, detail and update. */
public interface DriverService {

  DriverResponse create(CreateDriverRequest request);

  PageResponse<DriverResponse> list(
      DriverStatus status, Integer licenseExpiringInDays, int page, int size);

  DriverResponse getById(UUID id);

  DriverResponse update(UUID id, UpdateDriverRequest request);
}
