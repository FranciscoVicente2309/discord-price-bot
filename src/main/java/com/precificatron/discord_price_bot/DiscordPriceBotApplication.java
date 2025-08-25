package com.precificatron.discord_price_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DiscordPriceBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscordPriceBotApplication.class, args);
	}

}
