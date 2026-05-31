package net.jrodolfo.solace.broker.api.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for Cross-Origin Resource Sharing (CORS).
 * <p>
 * CORS is a mechanism that allows client web applications loaded in one domain
 * to interact with resources in a different domain. This class configures
 * the allowed origins, methods, and headers for the API.
 */
@Configuration
public class CorsConfig {

    /**
     * Configures CORS mappings for the application.
     * Allows requests from specified local development origins and supports common HTTP methods.
     *
     * @return a {@link WebMvcConfigurer} with CORS configurations applied.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Allow all endpoints
                        .allowedOrigins("http://localhost:5173", "http://localhost:5174") // Allow React app origin
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(true) // Allow sending credentials like cookies
                        .maxAge(3600); // Cache the preflight request for 1 hour
            }
        };
    }
}