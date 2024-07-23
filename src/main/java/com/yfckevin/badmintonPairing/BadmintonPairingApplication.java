package com.yfckevin.badmintonPairing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BadmintonPairingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BadmintonPairingApplication.class, args);
	}

}
