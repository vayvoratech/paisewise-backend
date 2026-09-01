package in.sapphirus.rupee.learn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LearnServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearnServiceApplication.class, args);
    }
}
