package com.fleet.control.routes.commons.client;

import com.fleet.control.routes.commons.client.dto.UpdateVehicleStatusRequest;
import com.fleet.control.routes.commons.client.dto.VehicleRef;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Downstream vehicle lookup used to validate route plans. */
@FeignClient(
    name = "ms-vehicles",
    url = "${vehicles.service.url}",
    configuration = FeignAuthConfig.class)
public interface VehiclesClient {

  /** Returns the vehicle detail or 404 when it does not exist. */
  @GetMapping("/api/vehicles/{vehicleId}")
  VehicleRef getById(@PathVariable("vehicleId") UUID vehicleId);

  /** Applies a status transition (optionally advancing the odometer). */
  @PatchMapping("/api/vehicles/{vehicleId}/status")
  void updateStatus(
      @PathVariable("vehicleId") UUID vehicleId, @RequestBody UpdateVehicleStatusRequest request);
}
