package dev.mars.apex.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for APEX REST API.
 * 
 * Allows cross-origin requests from the YAML Manager UI and other frontend applications.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-22
 * @version 1.0
 */
@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:8082",  // apex-yaml-manager UI
                    "http://localhost:3000",  // Common React dev server port
                    "http://localhost:4200",  // Common Angular dev server port
                    "http://localhost:5173",  // Common Vite dev server port
                    "http://localhost:8080"   // Same origin (for testing)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache preflight response for 1 hour
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = 
            new org.springframework.web.cors.CorsConfiguration();
        
        configuration.addAllowedOrigin("http://localhost:8082");  // apex-yaml-manager UI
        configuration.addAllowedOrigin("http://localhost:3000");  // React dev server
        configuration.addAllowedOrigin("http://localhost:4200");  // Angular dev server
        configuration.addAllowedOrigin("http://localhost:5173");  // Vite dev server
        configuration.addAllowedOrigin("http://localhost:8080");  // Same origin
        
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
