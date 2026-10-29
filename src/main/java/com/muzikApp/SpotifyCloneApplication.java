package com.muzikApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpotifyCloneApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotifyCloneApplication.class, args);
	}

}
