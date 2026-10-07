package com.fleet.control.gateway.health;

import com.fleet.control.gateway.constants.HealthConstants;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/health")
public class HealthController {

  @Value("${AUTH_SERVICE_URL:http://localhost:8081}")
  private String authUrl;

  @Value("${VEHICLES_SERVICE_URL:http://localhost:8082}")
  private String vehiclesUrl;

  @Value("${DRIVERS_SERVICE_URL:http://localhost:8083}")
  private String driversUrl;

  @Value("${ROUTES_SERVICE_URL:http://localhost:8085}")
  private String routesUrl;

  @Value("${MAINTENANCE_SERVICE_URL:http://localhost:8086}")
  private String maintenanceUrl;

  @Value("${FUEL_SERVICE_URL:http://localhost:8087}")
  private String fuelUrl;

  @Value("${ALERTS_SERVICE_URL:http://localhost:8088}")
  private String alertsUrl;

  @Value("${DASHBOARD_SERVICE_URL:http://localhost:8089}")
  private String dashboardUrl;

  private final WebClient webClient = WebClient.create();

  @GetMapping
  public Mono<Map<String, Object>> health() {
    return Mono.zip(
            check(authUrl),
            check(vehiclesUrl),
            check(driversUrl),
            check(routesUrl),
            check(maintenanceUrl),
            check(fuelUrl),
            check(alertsUrl),
            check(dashboardUrl))
        .map(
            tuple -> {
              Map<String, Object> response = new HashMap<>();
              response.put(HealthConstants.KEY_GATEWAY, HealthConstants.STATUS_UP);
              response.put(HealthConstants.KEY_TIMESTAMP, Instant.now());

              Map<String, String> services = new HashMap<>();
              services.put("ms-auth", tuple.getT1());
              services.put("ms-vehicles", tuple.getT2());
              services.put("ms-drivers", tuple.getT3());
              services.put("ms-routes", tuple.getT4());
              services.put("ms-maintenance", tuple.getT5());
              services.put("ms-fuel", tuple.getT6());
              services.put("ms-alerts", tuple.getT7());
              services.put("ms-dashboard", tuple.getT8());

              response.put(HealthConstants.KEY_SERVICES, services);
              return response;
            });
  }

  private Mono<String> check(String url) {
    return webClient
        .get()
        .uri(url + HealthConstants.ACTUATOR_HEALTH_PATH)
        .retrieve()
        .toBodilessEntity()
        .map(response -> HealthConstants.STATUS_UP)
        .timeout(java.time.Duration.ofSeconds(2))
        .onErrorReturn(HealthConstants.STATUS_DOWN);
  }
}
