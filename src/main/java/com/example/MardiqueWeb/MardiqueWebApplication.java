package com.example.MardiqueWeb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MardiqueWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(MardiqueWebApplication.class, args);
	}

}
