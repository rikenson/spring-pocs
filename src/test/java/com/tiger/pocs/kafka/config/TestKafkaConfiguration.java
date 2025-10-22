package com.tiger.pocs.kafka.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestKafkaConfiguration {

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // Return a mock instead of real KafkaTemplate to avoid connections
        return mock(KafkaTemplate.class);
    }

    @Bean
    @Primary
    public KafkaConfigurationSupport kafkaConfigurationSupport() {
        // Return a mock that doesn't try to connect to Kafka
        return mock(KafkaConfigurationSupport.class);
    }
}