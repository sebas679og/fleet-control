package com.fleet.control.routes.service;

import com.fleet.control.routes.models.dtos.request.CompleteRouteRequest;
import com.fleet.control.routes.models.dtos.request.CreateRouteRequest;
import com.fleet.control.routes.models.dtos.response.PageResponse;
import com.fleet.control.routes.models.dtos.response.RouteResponse;
import com.fleet.control.routes.models.dtos.response.RouteStatsResponse;
import com.fleet.control.routes.models.enums.Granularity;
import com.fleet.control.routes.models.enums.RouteStatus;
import java.time.LocalDate;
import java.util.UUID;

/** Route planning, execution and history operations. */
public interface RouteService {

  RouteResponse create(CreateRouteRequest request, UUID createdBy);

  PageResponse<RouteResponse> list(
      UUID vehicleId,
      UUID driverId,
      RouteStatus status,
      LocalDate from,
      LocalDate to,
      int page,
      int size);

  RouteResponse getById(UUID id);

  RouteResponse start(UUID id);

  RouteResponse complete(UUID id, CompleteRouteRequest request);

  RouteStatsResponse stats(LocalDate from, LocalDate to, Granularity granularity, UUID vehicleId);
}
