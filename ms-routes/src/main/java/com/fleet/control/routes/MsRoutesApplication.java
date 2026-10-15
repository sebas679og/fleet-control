package com.fleet.control.routes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Main application class for Routes Service. */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.fleet.control.routes.commons.client")
public class MsRoutesApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsRoutesApplication.class, args);
  }
}
