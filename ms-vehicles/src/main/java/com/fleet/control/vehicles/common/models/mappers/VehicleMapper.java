package com.fleet.control.vehicles.common.models.mappers;

import com.fleet.control.vehicles.common.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.common.models.entities.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface VehicleMapper {

    VehicleResponse toResponse(Vehicle v);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)      // Always create in AVAILABLE status
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vehicle toEntity(CreateVehicleRequest r);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "plate", ignore = true)
    @Mapping(target = "odometerKm",  ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    void updateEntity(UpdateVehicleRequest r, @MappingTarget Vehicle v);

}
