package com.fleet.control.vehicles.common.models.entities;

import java.time.Instant;
import java.util.UUID;

import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


/**
 *  vehicle
 *
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "/vehicles")
@Entity
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String plate;

    @Column(nullable = false, length = 40)
    private String make;

    @Column(nullable = false, length = 40)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "tank_capacity_l", nullable = false)
    private Integer tankCapacityL;

    @Column(name = "odometer_km", nullable = false)
    private Integer odometerKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
