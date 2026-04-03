package lol.clovis.spacemap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpaceMapApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpaceMapApplication.class, args);
	}

}
