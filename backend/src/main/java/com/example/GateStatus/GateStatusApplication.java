package com.example.GateStatus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
//@EnableMongoRepositories(basePackages = "com.example.GateStatus.domain")
public class GateStatusApplication {

	public static void main(String[] args) {
		SpringApplication.run(GateStatusApplication.class, args);
	}

}
