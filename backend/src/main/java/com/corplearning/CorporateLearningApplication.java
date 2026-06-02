package com.corplearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CorporateLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorporateLearningApplication.class, args);
    }
}
