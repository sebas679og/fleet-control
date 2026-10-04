package com.fleet.control.auth;

import org.springframework.boot.SpringApplication;

public class TestMsAuthApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsAuthApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
