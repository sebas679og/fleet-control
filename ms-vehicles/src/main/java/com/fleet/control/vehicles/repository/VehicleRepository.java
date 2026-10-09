package com.fleet.control.vehicles.repository;

import com.fleet.control.vehicles.common.models.entities.Vehicle;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByPlate(String plate);

    boolean existsByPlateAndIdNot(String plate, UUID id); // scalable future update

    @Query("""
        SELECT v FROM Vehicle v
        WHERE (:status IS NULL OR v.status = :status)
          AND (:type   IS NULL OR v.type   = :type)
          AND (:plate  IS NULL OR LOWER(v.plate) LIKE LOWER(CONCAT('%', :plate, '%')))
        """)
    Page<Vehicle> search(
            @Param("status") VehicleStatus status,
            @Param("type") VehicleType type,
            @Param("plate")  String plate,
            Pageable pageable);
}