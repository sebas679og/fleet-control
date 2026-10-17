package com.fleet.control.routes.controller.impl;

import com.fleet.control.routes.controller.RouteApi;
import com.fleet.control.routes.models.dtos.request.CompleteRouteRequest;
import com.fleet.control.routes.models.dtos.request.CreateRouteRequest;
import com.fleet.control.routes.models.dtos.response.PageResponse;
import com.fleet.control.routes.models.dtos.response.RouteResponse;
import com.fleet.control.routes.models.dtos.response.RouteStatsResponse;
import com.fleet.control.routes.models.enums.Granularity;
import com.fleet.control.routes.models.enums.RouteStatus;
import com.fleet.control.routes.service.RouteService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Route endpoints: delegates to the service with the documented status codes. */
@RestController
@RequiredArgsConstructor
public class RouteController implements RouteApi {
  private final RouteService routeService;

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<RouteResponse> create(CreateRouteRequest request) {
    // TODO: resolve createdBy from JWT sub once SecurityConfig lands.
    return ResponseEntity.status(HttpStatus.CREATED).body(routeService.create(request, null));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<PageResponse<RouteResponse>> list(
      UUID vehicle,
      UUID driver,
      RouteStatus status,
      LocalDate from,
      LocalDate to,
      int page,
      int size) {
    return ResponseEntity.ok(routeService.list(vehicle, driver, status, from, to, page, size));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<RouteResponse> getById(UUID routeId) {
    return ResponseEntity.ok(routeService.getById(routeId));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<RouteResponse> start(UUID routeId) {
    return ResponseEntity.ok(routeService.start(routeId));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<RouteResponse> complete(UUID routeId, CompleteRouteRequest request) {
    return ResponseEntity.ok(routeService.complete(routeId, request));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ResponseEntity<RouteStatsResponse> stats(
      LocalDate from, LocalDate to, Granularity granularity, UUID vehicle) {
    return ResponseEntity.ok(routeService.stats(from, to, granularity, vehicle));
  }
}
