package com.fleet.control.routes.repository;

import com.fleet.control.routes.models.entities.Route;
import com.fleet.control.routes.models.enums.RouteStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

  /** Filtros opcionales del listado. */
  @Query(
      """
            SELECT r FROM Route r
            WHERE (CAST(:vehicleId AS uuid) IS NULL OR r.vehicleId = :vehicleId)
              AND (CAST(:driverId AS uuid) IS NULL OR r.driverId = :driverId)
              AND (CAST(:status AS string) IS NULL OR r.status = :status)
              AND (CAST(:from AS timestamp) IS NULL OR r.plannedStart >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR r.plannedStart <= :to)
            """)
  Page<Route> search(
      @Param("vehicleId") UUID vehicleId,
      @Param("driverId") UUID driverId,
      @Param("status") RouteStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  /**
   * Comprueba si existe una ruta PLANNED o IN_PROGRESS que se solape con [start, end) para el mismo
   * vehículo o el mismo conductor. El fin de cada ruta existente se calcula en Java como
   * plannedStart + estimatedDurationMin para evitar funciones JPQL no portables.
   */
  @Query(
      """
            SELECT r FROM Route r
            WHERE r.status IN (RouteStatus.PLANNED,
                               RouteStatus.IN_PROGRESS)
              AND (r.vehicleId = :vehicleId OR r.driverId = :driverId)
              AND r.plannedStart < :end
            """)
  List<Route> findOverlapCandidates(
      @Param("vehicleId") UUID vehicleId,
      @Param("driverId") UUID driverId,
      @Param("end") Instant end);

  /** Prevención de solape: existe candidata cuyo fin supera start. */
  default boolean existsOverlap(UUID vehicleId, UUID driverId, Instant start, Instant end) {
    return findOverlapCandidates(vehicleId, driverId, end).stream()
        .anyMatch(
            r ->
                r.getPlannedStart() != null
                    && r.getEstimatedDurationMin() != null
                    && r.getPlannedStart()
                        .plus(r.getEstimatedDurationMin(), ChronoUnit.MINUTES)
                        .isAfter(start));
  }

  /** Rutas completadas para la serie temporal. */
  @Query(
      """
            SELECT r FROM Route r
            WHERE r.status = RouteStatus.COMPLETED
              AND r.endedAt >= :from
              AND r.endedAt < :to
              AND (CAST(:vehicleId AS uuid) IS NULL OR r.vehicleId = :vehicleId)
            """)
  List<Route> findCompletedBetween(
      @Param("from") Instant from, @Param("to") Instant to, @Param("vehicleId") UUID vehicleId);
}
