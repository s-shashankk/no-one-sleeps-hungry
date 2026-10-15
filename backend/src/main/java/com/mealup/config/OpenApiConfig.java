package com.mealup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI mealUpOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MealUp API")
                .version("v1")
                .description("Records for a community food-rescue initiative: vendors, coworkers and food requests."));
    }
}
