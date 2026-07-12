package com.restaurant.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableAsync
public class Application {
	public static void main(String[] args) {
		// Load .env file. System.setProperty() sets Java properties (used by Spring),
		// but the Google GenAI SDK reads OS env vars via System.getenv() — a different
		// mechanism. So we must pass the API key explicitly to the Client builder.
		Dotenv dotenv = Dotenv.configure()
				.directory("./inventory-service") // relative to project root CWD
				.ignoreIfMissing() // fall back gracefully if missing
				.load();
		dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
		SpringApplication.run(Application.class, args);
	}
}