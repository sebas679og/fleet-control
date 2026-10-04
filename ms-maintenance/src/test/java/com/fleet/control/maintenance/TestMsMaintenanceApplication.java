package com.fleet.control.maintenance;

import org.springframework.boot.SpringApplication;

public class TestMsMaintenanceApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsMaintenanceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
