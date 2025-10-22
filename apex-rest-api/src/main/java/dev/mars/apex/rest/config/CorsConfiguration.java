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
                .allowedOriginPatterns(
                    "http://localhost:*",     // Any localhost port
                    "file://*",               // File protocol for local HTML files
                    "null"                    // For file:// origins that appear as null
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(false)  // Must be false when using allowedOriginPatterns with "*"
                .maxAge(3600); // Cache preflight response for 1 hour
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
            new org.springframework.web.cors.CorsConfiguration();

        configuration.addAllowedOriginPattern("http://localhost:*");  // Any localhost port
        configuration.addAllowedOriginPattern("file://*");            // File protocol for local HTML files
        configuration.addAllowedOrigin("null");                       // For file:// origins that appear as null

        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(false);  // Must be false when using patterns with "*"
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
