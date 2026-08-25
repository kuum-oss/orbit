package com.orbit.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrbitProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrbitProcessorApplication.class, args);
    }
}
