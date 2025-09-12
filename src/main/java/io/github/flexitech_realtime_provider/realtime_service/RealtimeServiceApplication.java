package io.github.flexitech_realtime_provider.realtime_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"io.github.flexitech_realtime_provider.realtime_service", "io.github.flexitech_realtime_provider.common"})
public class RealtimeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealtimeServiceApplication.class, args);
	}

}
