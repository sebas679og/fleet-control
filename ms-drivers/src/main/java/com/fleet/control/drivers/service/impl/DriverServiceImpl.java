package com.fleet.control.drivers.service.impl;

import com.fleet.control.drivers.commons.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.commons.models.dtos.response.PageResponse;
import com.fleet.control.drivers.commons.models.enums.DriverStatus;
import com.fleet.control.drivers.service.DriverService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DriverServiceImpl implements DriverService {


    @Override
    public DriverResponse create(CreateDriverRequest request) {
        return null;
    }

    @Override
    public PageResponse<DriverResponse> list(DriverStatus status, Integer licenseExpiringInDays, int page, int size) {
        return null;
    }

    @Override
    public DriverResponse getById(UUID id) {
        return null;
    }

    @Override
    public DriverResponse update(UUID id, UpdateDriverRequest request) {
        return null;
    }
}
