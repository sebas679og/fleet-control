package com.fleet.control.drivers.models.entities;

import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.models.enums.LicenseCategory;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "drivers")
public class Driver {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;

  @Column(name = "email", nullable = false, length = 120)
  private String email;

  @Column(name = "phone", nullable = false, length = 30)
  private String phone;

  @Column(name = "license_number", nullable = false, length = 30, unique = true)
  private String licenseNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "license_category", nullable = false, length = 5)
  private LicenseCategory licenseCategory;

  @Column(name = "license_expires_at", nullable = false)
  private LocalDate licenseExpiresAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private DriverStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
