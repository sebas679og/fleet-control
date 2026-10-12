package com.fleet.control.vehicles.controller.impl;

import com.fleet.control.vehicles.common.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.response.PageResponse;
import com.fleet.control.vehicles.common.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import com.fleet.control.vehicles.controller.VehicleApi;
import com.fleet.control.vehicles.service.VehicleService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST implementation of the vehicle inventory endpoints. */
@RestController
@RequiredArgsConstructor
public class VehicleApiController implements VehicleApi {

  private final VehicleService vehicleService;

  @Override
  public ResponseEntity<VehicleResponse> create(
      @RequestBody @Valid CreateVehicleRequest request) {
    VehicleResponse response = vehicleService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<PageResponse<VehicleResponse>> list(
      @RequestParam(required = false) VehicleStatus status,
      @RequestParam(required = false, name = "type") VehicleType vehicleType,
      @RequestParam(required = false) String plate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(vehicleService.list(status, vehicleType, plate, page, size));
  }

  @Override
  public ResponseEntity<VehicleResponse> getById(@PathVariable UUID vehicleId) {
    return ResponseEntity.ok(vehicleService.getById(vehicleId));
  }

  @Override
  public ResponseEntity<VehicleResponse> update(
      @PathVariable UUID vehicleId, @RequestBody @Valid UpdateVehicleRequest request) {
    return ResponseEntity.ok(vehicleService.update(vehicleId, request));
  }
}
