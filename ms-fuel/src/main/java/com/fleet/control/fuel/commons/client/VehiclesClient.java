package com.fleet.control.fuel.commons.client;

import com.fleet.control.fuel.commons.client.dto.VehicleRef;
import com.fleet.control.fuel.config.FeignAuthConfig;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Downstream vehicle lookup used to validate refuels and enrich responses. */
@FeignClient(
    name = "ms-vehicles",
    url = "${vehicles.service.url}",
    configuration = FeignAuthConfig.class)
public interface VehiclesClient {

  /** Returns the vehicle detail or 404 when it does not exist. */
  @GetMapping("/api/vehicles/{vehicleId}")
  VehicleRef getById(@PathVariable("vehicleId") UUID vehicleId);
}
