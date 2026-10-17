package com.fleet.control.fuel.controller.impl;

import com.fleet.control.fuel.controller.FuelApi;
import com.fleet.control.fuel.models.dto.request.CreateFuelRecordRequest;
import com.fleet.control.fuel.models.dto.response.FuelConsumptionResponse;
import com.fleet.control.fuel.models.dto.response.FuelRecordResponse;
import com.fleet.control.fuel.models.dto.response.FuelStatsResponse;
import com.fleet.control.fuel.models.dto.response.PageResponse;
import com.fleet.control.fuel.models.enums.Granularity;
import com.fleet.control.fuel.service.FuelService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Refuel endpoints: delegates to the service with the documented status codes. */
@RestController
@RequiredArgsConstructor
public class FuelController implements FuelApi {

  private final FuelService fuelService;

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<FuelRecordResponse> create(CreateFuelRecordRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(fuelService.create(request));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<PageResponse<FuelRecordResponse>> list(
      UUID vehicle, LocalDate from, LocalDate to, int page, int size) {
    return ResponseEntity.ok(fuelService.list(vehicle, from, to, page, size));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<FuelConsumptionResponse> consumption(LocalDate from, LocalDate to) {
    return ResponseEntity.ok(fuelService.consumption(from, to));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<FuelStatsResponse> stats(
      LocalDate from, LocalDate to, Granularity granularity, UUID vehicle) {
    return ResponseEntity.ok(fuelService.stats(from, to, granularity, vehicle));
  }
}
