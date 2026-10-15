package com.fleet.control.routes.models.mappers;

import com.fleet.control.routes.models.dtos.response.RouteResponse;
import com.fleet.control.routes.models.entities.Route;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface RouteMapper {

  RouteResponse toResponse(Route route);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "startedAt", ignore = true)
  @Mapping(target = "startOdometerKm", ignore = true)
  @Mapping(target = "endedAt", ignore = true)
  @Mapping(target = "endOdometerKm", ignore = true)
  @Mapping(target = "actualDistanceKm", ignore = true)
  @Mapping(target = "notes", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Route toEntity(RouteResponse route);
}
