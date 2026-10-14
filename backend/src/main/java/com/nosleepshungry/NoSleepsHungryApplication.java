package com.nosleepshungry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NoSleepsHungryApplication {
    public static void main(String[] args) {
        SpringApplication.run(NoSleepsHungryApplication.class, args);
    }
}
