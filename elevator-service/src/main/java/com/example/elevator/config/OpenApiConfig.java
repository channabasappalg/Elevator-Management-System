package com.example.elevator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI elevatorOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Elevator Management System API")
                        .description("API for managing elevators, requests, and monitoring status.")
                        .version("v1.0"));
    }
}