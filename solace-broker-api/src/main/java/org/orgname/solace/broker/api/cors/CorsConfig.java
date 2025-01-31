package org.orgname.solace.broker.api.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * What is Cross-Origin Resource Sharing? Cross-origin resource sharing (CORS)
 * is a mechanism for integrating applications. CORS defines a way for client
 * web applications that are loaded in one domain to interact with resources
 * in a different domain.
 */
@Configuration
public class CorsConfig {

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