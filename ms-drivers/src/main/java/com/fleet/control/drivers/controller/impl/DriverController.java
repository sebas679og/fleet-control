package com.fleet.control.drivers.controller.impl;

import com.fleet.control.drivers.commons.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.commons.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.commons.models.dtos.response.PageResponse;
import com.fleet.control.drivers.commons.models.enums.DriverStatus;
import com.fleet.control.drivers.controller.DriverApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DriverController implements DriverApi {
    @Override
    public ResponseEntity<DriverResponse> create(CreateDriverRequest request) {
        return null;
    }

    @Override
    public ResponseEntity<PageResponse<DriverResponse>> list(DriverStatus status, Integer licenseExpiringInDays, int page, int size) {
        return null;
    }

    @Override
    public ResponseEntity<DriverResponse> getById(UUID driverId) {
        return null;
    }

    @Override
    public ResponseEntity<DriverResponse> update(UUID driverId, UpdateDriverRequest request) {
        return null;
    }
}
