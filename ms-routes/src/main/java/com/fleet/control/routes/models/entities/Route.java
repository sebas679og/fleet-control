package com.fleet.control.routes.models.entities;

import com.fleet.control.routes.models.enums.RouteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Persistent route planned with a vehicle and a driver. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "routes")
public class Route {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "vehicle_id", nullable = false)
  private UUID vehicleId;

  @Column(name = "driver_id", nullable = false)
  private UUID driverId;

  @Column(name = "origin", nullable = false, length = 200)
  private String origin;

  @Column(name = "destination", nullable = false, length = 200)
  private String destination;

  @Column(name = "planned_start", nullable = false)
  private Instant plannedStart;

  @Column(name = "estimated_duration_min", nullable = false)
  private Integer estimatedDurationMin;

  @Column(name = "planned_distance_km", nullable = false, precision = 10, scale = 2)
  private BigDecimal plannedDistanceKm;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RouteStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "start_odometer_km")
  private Integer startOdometerKm;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "end_odometer_km")
  private Integer endOdometerKm;

  @Column(name = "actual_distance_km", precision = 10, scale = 2)
  private BigDecimal actualDistanceKm;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "created_by")
  private UUID createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
