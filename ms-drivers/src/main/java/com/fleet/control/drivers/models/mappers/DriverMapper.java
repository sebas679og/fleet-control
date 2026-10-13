package com.fleet.control.drivers.models.mappers;

import com.fleet.control.drivers.models.dtos.request.CreateDriverRequest;
import com.fleet.control.drivers.models.dtos.request.UpdateDriverRequest;
import com.fleet.control.drivers.models.dtos.response.DriverResponse;
import com.fleet.control.drivers.models.entities.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface DriverMapper {

  DriverResponse toResponse(Driver driver);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Driver toEntity(CreateDriverRequest createDriverRequest);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateToEntity(UpdateDriverRequest updateDriverRequest, @MappingTarget Driver driver);
}
