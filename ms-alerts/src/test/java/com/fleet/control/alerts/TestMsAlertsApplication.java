package com.fleet.control.alerts;

import org.springframework.boot.SpringApplication;

/**
 * Test application entry point used to start the application with Testcontainers-based
 * infrastructure for integration testing.
 */
public class TestMsAlertsApplication {

  /**
   * Starts the application using the Testcontainers configuration.
   *
   * @param args command-line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.from(MsAlertsApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
