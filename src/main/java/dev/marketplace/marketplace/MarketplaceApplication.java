package dev.marketplace.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CrossOrigin(origins = "http://localhost:3000")
@SpringBootApplication
@ComponentScan(basePackages = "dev.marketplace.marketplace")
public class MarketplaceApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MarketplaceApplication.class);
	
	@Autowired
	private Environment environment;

	public static void main(String[] args) {
		SpringApplication.run(MarketplaceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("Active profiles: {}", String.join(", ", environment.getActiveProfiles()));
		logger.info("Default profiles: {}", String.join(", ", environment.getDefaultProfiles()));
	}

}
