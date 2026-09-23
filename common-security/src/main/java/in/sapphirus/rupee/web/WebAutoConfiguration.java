package in.sapphirus.rupee.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the shared exception handler in every service that imports common-security. */
@Configuration
public class WebAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
