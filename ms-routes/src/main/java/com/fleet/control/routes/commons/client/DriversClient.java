package com.fleet.control.routes.commons.client;

import com.fleet.control.routes.commons.client.dto.DriverRef;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Downstream driver lookup used to validate route plans. */
@FeignClient(
    name = "ms-drivers",
    url = "${drivers.service.url}",
    configuration = FeignAuthConfig.class)
public interface DriversClient {

  /** Returns the driver detail or 404 when it does not exist. */
  @GetMapping("/api/drivers/{driverId}")
  DriverRef getById(@PathVariable("driverId") UUID driverId);
}
