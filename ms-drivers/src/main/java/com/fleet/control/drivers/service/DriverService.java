package com.fleet.control.drivers.service;

import com.fleet.control.drivers.commons.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.commons.models.dtos.response.PageResponse;
import com.fleet.control.drivers.commons.models.enums.DriverStatus;

import java.util.UUID;

public interface DriverService {

    DriverResponse create(CreateDriverRequest request);

    PageResponse<DriverResponse> list(
            DriverStatus status,
            Integer licenseExpiringInDays,
            int page,
            int size
    );

    DriverResponse getById(UUID id);

    DriverResponse update(UUID id, UpdateDriverRequest request);

}
