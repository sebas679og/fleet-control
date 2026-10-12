package com.fleet.control.vehicles.service.impl;

import com.fleet.control.vehicles.common.models.dtos.request.CreateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.request.UpdateVehicleRequest;
import com.fleet.control.vehicles.common.models.dtos.response.PageResponse;
import com.fleet.control.vehicles.common.models.dtos.response.VehicleResponse;
import com.fleet.control.vehicles.common.models.entities.Vehicle;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import com.fleet.control.vehicles.common.models.mappers.VehicleMapper;
import com.fleet.control.vehicles.exceptions.ApiVehicleException;
import com.fleet.control.vehicles.repository.VehicleRepository;
import com.fleet.control.vehicles.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

    private static final int MIN_YEAR = 1990;
    private static final int MAX_YEAR_OFFSET = 1;
    private static final int MAX_PAGE_SIZE = 20;

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        if(vehicleRepository.existsByPlate(request.plate())) {
            throw new ApiVehicleException.VehicleAlreadyExistsException(request.plate());
        }

        validateYear(request.year());

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public PageResponse<VehicleResponse> list(VehicleStatus status, VehicleType vehicleType,
                                              String plate, int page, int size) {
        if (page < 0) {
            throw new ApiVehicleException.InvalidPaginationException("page",
                    "page must be 0 or greater");
        }
        if (size <= 0) {
            throw new ApiVehicleException.InvalidPaginationException("size",
                    "size must be greater than 0");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        Page<Vehicle> result = vehicleRepository.search(status, vehicleType, plate, pageable);

        return PageResponse.of(
                result.getContent().stream().map(vehicleMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Override
    public VehicleResponse getById(UUID id) {
        return vehicleRepository.findById(id).map(vehicleMapper::toResponse)
                .orElseThrow(() -> new ApiVehicleException.VehicleNotFoundException(id));
    }

    @Override
    @Transactional
    public VehicleResponse update(UUID id, UpdateVehicleRequest request) {


        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ApiVehicleException.VehicleNotFoundException(id));

        validateYear(request.year());
        vehicleMapper.updateEntity(request, vehicle);

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    private void validateYear(Integer year) {
        int max = LocalDate.now().getYear() + MAX_YEAR_OFFSET;
        if (year == null || year < MIN_YEAR || year > max) {
            throw new ApiVehicleException.InvalidVehicleYearException(year, MIN_YEAR, max);
        }
    }
}
