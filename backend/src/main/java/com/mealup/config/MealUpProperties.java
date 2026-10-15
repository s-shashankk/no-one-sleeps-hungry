package com.mealup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Application settings under the "mealup" prefix. The admin block is only used when no account exists yet. */
@ConfigurationProperties(prefix = "mealup")
public record MealUpProperties(@DefaultValue Admin admin) {

    public record Admin(String fullName, String phone, String email, String password) { }
}
