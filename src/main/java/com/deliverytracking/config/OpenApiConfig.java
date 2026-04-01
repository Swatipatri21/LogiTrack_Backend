package com.deliverytracking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Delivery Tracking System API",
                version = "v1",
                description = "A complete logistics delivery tracking platform that allows administrators to create shipments, " +
                        "staff to update delivery statuses, and customers to track their shipments in real-time.",
                contact = @Contact(
                        name = "Delivery Tracking Team",
                        email = "support@deliverytracking.com"
                )
        ),
        servers = {
                @Server(url = "/", description = "Local Development Server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Bearer Token Authentication. Enter your token in the format: Bearer {token}",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
	@Bean
	public RestTemplate restTemplate() {
	    return new RestTemplate();
	}
}
