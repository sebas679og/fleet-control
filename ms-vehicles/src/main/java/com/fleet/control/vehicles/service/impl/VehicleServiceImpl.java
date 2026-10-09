package com.fleet.control.vehicles.service.impl;

import com.fleet.control.vehicles.common.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.response.PageResponse;
import com.fleet.control.vehicles.common.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import com.fleet.control.vehicles.service.VehicleService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VehicleServiceImpl implements VehicleService {


    @Override
    public VehicleResponse create(CreateVehicleRequest request) {
        return null;
    }

    @Override
    public PageResponse<VehicleResponse> list(VehicleStatus status, VehicleType vehicleType,
                                              String plate, int page, int size) {
        return null;
    }

    @Override
    public VehicleResponse getById(UUID id) {
        return null;
    }

    @Override
    public VehicleResponse update(UUID id, UpdateVehicleRequest request) {
        return null;
    }
}
