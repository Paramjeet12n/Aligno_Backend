package com.realtimeboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.net.URI;

@SpringBootApplication
public class RealtimeboardBackendApplication {

	public static void main(String[] args) {
		String dbUrl = System.getenv("DATABASE_URL");
		if (dbUrl == null || dbUrl.isEmpty()) {
			dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		}
		
		if (dbUrl != null && !dbUrl.isEmpty()) {
			try {
				// Prevent double jdbc:
				String cleanUrl = dbUrl.replaceFirst("^jdbc:", "");
				URI dbUri = new URI(cleanUrl);
				
				String userInfo = dbUri.getUserInfo();
				if (userInfo != null) {
					String[] split = userInfo.split(":", 2);
					System.setProperty("spring.datasource.username", split[0]);
					if (split.length > 1) {
						System.setProperty("spring.datasource.password", split[1]);
					}
				}
				
				String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + 
					(dbUri.getPort() != -1 ? dbUri.getPort() : 5432) + 
					dbUri.getPath() + 
					(dbUri.getQuery() != null ? "?" + dbUri.getQuery() : "");
					
				System.setProperty("spring.datasource.url", jdbcUrl);
			} catch (Exception e) {
				System.err.println("Failed to parse DB URL: " + e.getMessage());
			}
		} else {
			// Fallback placeholder so Spring doesn't crash on empty missing props
			System.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/realtimeboard");
			System.setProperty("spring.datasource.username", "postgres");
		}
		
		SpringApplication.run(RealtimeboardBackendApplication.class, args);
	}

}
