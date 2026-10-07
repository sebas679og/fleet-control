package com.fleet.control.gateway.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fleet.control.gateway.constants.HealthConstants;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the aggregated /health response shape (US-04). */
class HealthControllerTest {

  private static final String CLOSED_PORT_URL = "http://localhost:9";

  private HealthController controllerWithClosedServices() {
    HealthController controller = new HealthController();
    ReflectionTestUtils.setField(controller, "authUrl", CLOSED_PORT_URL);
    ReflectionTestUtils.setField(controller, "vehiclesUrl", CLOSED_PORT_URL);
    ReflectionTestUtils.setField(controller, "driversUrl", CLOSED_PORT_URL);
    ReflectionTestUtils.setField(controller, "routesUrl", CLOSED_PORT_URL);
    ReflectionTestUtils.setField(controller, "maintenanceUrl", CLOSED_PORT_URL);
    ReflectionTestUtils.setField(controller, "fuelUrl", CLOSED_PORT_URL);
    ReflectionTestUtils.setField(controller, "alertsUrl", CLOSED_PORT_URL);
    ReflectionTestUtils.setField(controller, "dashboardUrl", CLOSED_PORT_URL);
    return controller;
  }

  @Test
  @SuppressWarnings("unchecked")
  void unreachableServicesAreReportedDown() {
    Map<String, Object> body =
        controllerWithClosedServices().health().block(Duration.ofSeconds(30));

    assertNotNull(body);
    assertEquals(HealthConstants.STATUS_UP, body.get(HealthConstants.KEY_GATEWAY));
    assertNotNull(body.get(HealthConstants.KEY_TIMESTAMP));

    Map<String, String> services = (Map<String, String>) body.get(HealthConstants.KEY_SERVICES);
    assertNotNull(services);
    assertEquals(8, services.size());
    services.values().forEach(status -> assertEquals(HealthConstants.STATUS_DOWN, status));
  }
}
