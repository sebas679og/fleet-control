package com.fleet.control.vehicles.repository;

import com.fleet.control.vehicles.common.models.entities.Vehicle;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for vehicle persistence and search. */
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

  boolean existsByPlate(String plate);

  boolean existsByPlateAndIdNot(String plate, UUID id); // scalable future update

  @Query(
      """
        SELECT v FROM Vehicle v
        WHERE (:status IS NULL OR v.status = :status)
          AND (:type   IS NULL OR v.type   = :type)
          AND (CAST(:plate AS string) IS NULL
            OR LOWER(v.plate) LIKE LOWER(CONCAT('%', CAST(:plate AS string), '%')))
        """)
  Page<Vehicle> search(
      @Param("status") VehicleStatus status,
      @Param("type") VehicleType type,
      @Param("plate") String plate,
      Pageable pageable);
}
