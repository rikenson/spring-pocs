package com.tiger.pocs.kafka.config;

import com.tiger.pocs.kafka.domain.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnProperty(prefix = "kafka", name = "bootstrap-servers")
public class ModernKafkaConfiguration {

    private final KafkaConfigurationSupport configSupport;

    @PostConstruct
    public void init() {
        KafkaConfigUtils.setConfigurationSupport(configSupport);
    }

    @Bean
    public ConsumerFactory<String, Object> kafkaConsumerFactory(KafkaProperties properties) {
        log.info("🚀 Creating Kafka consumer factory for: {}", properties.getBootstrapServers());
        return new DefaultKafkaConsumerFactory<>(configSupport.buildConsumerConfig(properties));
    }

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory(KafkaProperties properties) {
        return new DefaultKafkaProducerFactory<>(configSupport.buildProducerConfig(properties));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        return factory;
    }
}