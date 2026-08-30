package com.orbit.orchestrator.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamundaConfig {

    private static final Logger log = LoggerFactory.getLogger(CamundaConfig.class);

    @Bean
    public CommandLineRunner camundaEngineLogger(ProcessEngine processEngine) {
        return args -> {
            log.info("Camunda Process Engine initialized: '{}'", processEngine.getName());
        };
    }
}
