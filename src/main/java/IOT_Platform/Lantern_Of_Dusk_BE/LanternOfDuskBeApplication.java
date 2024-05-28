package IOT_Platform.Lantern_Of_Dusk_BE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LanternOfDuskBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(LanternOfDuskBeApplication.class, args);
	}

}
