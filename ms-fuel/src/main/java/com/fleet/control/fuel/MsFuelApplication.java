package com.fleet.control.fuel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Main application class for Fuel Service. */
@EnableFeignClients(basePackages = "com.fleet.control.fuel.commons.client")
@SpringBootApplication(exclude = SimpleDiscoveryClientAutoConfiguration.class)
public class MsFuelApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsFuelApplication.class, args);
  }
}
