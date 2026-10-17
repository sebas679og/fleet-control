package com.fleet.control.fuel.models.mappers;

import com.fleet.control.fuel.models.dto.request.CreateFuelRecordRequest;
import com.fleet.control.fuel.models.dto.response.FuelRecordResponse;
import com.fleet.control.fuel.models.entities.FuelRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps between refuel requests, entities and responses. */
@Mapper
public interface FuelRecordMapper {

  FuelRecordResponse toDto(FuelRecord fuelRecord);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "fuelType", ignore = true)
  @Mapping(target = "totalCost", ignore = true)
  @Mapping(target = "consumptionL100km", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  FuelRecord toEntity(CreateFuelRecordRequest request);
}
