package com.bianzu.bianzu_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BianzuBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BianzuBackendApplication.class, args);
	}

}
