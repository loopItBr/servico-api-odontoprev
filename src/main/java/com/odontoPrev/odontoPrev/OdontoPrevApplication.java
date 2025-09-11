package com.odontoPrev.odontoPrev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableFeignClients(basePackages = "com.odontoPrev.odontoPrev.infrastructure.client")
public class OdontoPrevApplication {

	public static void main(String[] args) {
		SpringApplication.run(OdontoPrevApplication.class, args);
	}

}
