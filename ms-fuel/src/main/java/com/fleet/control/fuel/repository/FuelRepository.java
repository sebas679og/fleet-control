package com.fleet.control.fuel.repository;

import com.fleet.control.fuel.models.entities.FuelRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Refuel persistence with filtered search, period aggregation and consumption helpers. */
public interface FuelRepository extends JpaRepository<FuelRecord, UUID> {

  /** Último repostaje de un vehículo (para validar el odómetro). */
  Optional<FuelRecord> findTopByVehicleIdOrderByRefueledAtDesc(UUID vehicleId);

  /** Último lleno anterior (para calcular el consumo entre llenos). */
  Optional<FuelRecord> findTopByVehicleIdAndFullTankTrueAndRefueledAtBeforeOrderByRefueledAtDesc(
      UUID vehicleId, Instant before);

  /** Listado paginado con filtros opcionales. */
  @Query(
      """
            SELECT f FROM FuelRecord f
            WHERE (CAST(:vehicleId AS uuid) IS NULL OR f.vehicleId = :vehicleId)
              AND (CAST(:from AS timestamp) IS NULL OR f.refueledAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR f.refueledAt < :to)
            ORDER BY f.refueledAt DESC
            """)
  Page<FuelRecord> search(
      @Param("vehicleId") UUID vehicleId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  /**
   * Repostajes en un periodo, ordenados por vehículo y fecha. Se usa para agregar consumo y
   * estadísticas en el servicio.
   */
  @Query(
      """
            SELECT f FROM FuelRecord f
            WHERE f.refueledAt >= :from
              AND f.refueledAt < :to
              AND (CAST(:vehicleId AS uuid) IS NULL OR f.vehicleId = :vehicleId)
            ORDER BY f.vehicleId ASC, f.refueledAt ASC
            """)
  List<FuelRecord> findInPeriod(
      @Param("from") Instant from, @Param("to") Instant to, @Param("vehicleId") UUID vehicleId);

  /** Litros repostados por un vehículo entre dos instantes (inclusive). */
  @Query(
      """
            SELECT COALESCE(SUM(f.liters), 0) FROM FuelRecord f
            WHERE f.vehicleId = :vehicleId
              AND f.refueledAt > :from
              AND f.refueledAt <= :to
            """)
  BigDecimal sumLitersBetween(
      @Param("vehicleId") UUID vehicleId, @Param("from") Instant from, @Param("to") Instant to);
}
