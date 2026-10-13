package com.fleet.control.vehicles.service;

import com.fleet.control.vehicles.common.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateStatusRequest;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.response.PageResponse;
import com.fleet.control.vehicles.common.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;

import java.util.UUID;

/** Service contract for vehicle operations. */
public interface VehicleService {

  VehicleResponse create(CreateVehicleRequest request);

  PageResponse<VehicleResponse> list(
      VehicleStatus status, VehicleType vehicleType,
      String plate, int page, int size);

  VehicleResponse getById(UUID id);

  VehicleResponse update(UUID id, UpdateVehicleRequest request);

  VehicleResponse updateStatus(UUID id, UpdateStatusRequest request);
}
