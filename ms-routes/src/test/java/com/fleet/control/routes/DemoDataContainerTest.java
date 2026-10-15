package com.fleet.control.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleet.control.routes.models.enums.RouteStatus;
import com.fleet.control.routes.repository.RouteRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the demo profile Flyway seed (V2__demo_routes.sql) against real PostgreSQL: 12
 * deterministic routes reusing the demo vehicle/driver ids, with 7 planned, 2 in progress and 3
 * completed.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("demo")
class DemoDataContainerTest {

  @Autowired private RouteRepository routeRepository;

  @Test
  void seedsTwelveRoutesWithDocumentedDistribution() {
    assertEquals(12, routeRepository.count());

    var routeOne =
        routeRepository.findById(UUID.fromString("8ecea952-9d00-3d65-a0de-1934f44e7c7d"));
    assertTrue(routeOne.isPresent());
    assertEquals("Madrid", routeOne.get().getOrigin());
    assertEquals("Zaragoza", routeOne.get().getDestination());
    assertEquals(RouteStatus.COMPLETED, routeOne.get().getStatus());
    assertEquals(
        UUID.fromString("99b5202f-f181-3828-9461-3571f0f67f94"), routeOne.get().getVehicleId());
    assertEquals(
        UUID.fromString("a6efc71c-384f-3cb0-8f2c-6d0d82e754fe"), routeOne.get().getDriverId());
    assertEquals(0, new BigDecimal("352.00").compareTo(routeOne.get().getActualDistanceKm()));

    var all = routeRepository.findAll();
    assertEquals(7, all.stream().filter(r -> r.getStatus() == RouteStatus.PLANNED).count());
    assertEquals(2, all.stream().filter(r -> r.getStatus() == RouteStatus.IN_PROGRESS).count());
    assertEquals(3, all.stream().filter(r -> r.getStatus() == RouteStatus.COMPLETED).count());
  }

  @Test
  void searchWithoutFiltersFindsAllSeededRoutes() {
    var unfiltered = routeRepository.search(null, null, null, null, null, PageRequest.of(0, 30));

    assertEquals(12, unfiltered.getTotalElements());
  }
}
