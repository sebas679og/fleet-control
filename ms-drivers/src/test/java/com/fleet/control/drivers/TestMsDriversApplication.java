package com.fleet.control.drivers;

import org.springframework.boot.SpringApplication;

public class TestMsDriversApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsDriversApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
