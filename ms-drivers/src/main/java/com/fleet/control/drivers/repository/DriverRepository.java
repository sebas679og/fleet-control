package com.fleet.control.drivers.repository;

import com.fleet.control.drivers.models.entities.Driver;
import com.fleet.control.drivers.models.enums.DriverStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

  boolean existsByLicenseNumber(String licenseNumber);

  boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

  @Query(
      """
            SELECT d FROM Driver d
            WHERE (:status IS NULL OR d.status = :status)
              AND (:expiresBefore IS NULL OR d.licenseExpiresAt <= :expiresBefore)
            """)
  Page<Driver> search(
      @Param("status") DriverStatus status,
      @Param("expiresBefore") LocalDate expiresBefore,
      Pageable pageable);
}
