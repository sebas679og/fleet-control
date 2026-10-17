package com.fleet.control.fuel.models.entities;

import com.fleet.control.fuel.models.enums.FuelType;
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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Refuel record with computed cost and consumption between full tanks. */
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "fuel_records")
public class FuelRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "vehicle_id", nullable = false)
  private UUID vehicleId;

  @Column(name = "refuel_at", nullable = false)
  private Instant refueledAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "fuel_type", nullable = false)
  private FuelType fuelType;

  @Column(name = "liters", nullable = false, precision = 10, scale = 2)
  private BigDecimal liters;

  @Column(name = "price_per_liter", nullable = false, precision = 10, scale = 2)
  private BigDecimal pricePerLiter;

  @Column(name = "total_cost", nullable = false)
  private BigDecimal totalCost;

  @Column(name = "odometer_km", nullable = false)
  private Integer odometerKm;

  @Column(name = "full_tank", nullable = false)
  private boolean fullTank;

  @Column(name = "station", length = 120)
  private String station;

  @Column(name = "consumption_l100km", precision = 10, scale = 2)
  private BigDecimal consumptionL100km;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
