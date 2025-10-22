package com.tiger.pocs.kafka.config;

import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.support.KafkaTestParameterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, KafkaTestParameterResolver.class})
class ModernKafkaConfigurationTest {

    @Mock
    private KafkaConfigurationSupport configSupport;

    private ModernKafkaConfiguration kafkaConfiguration;
    private KafkaProperties kafkaProperties;

    @BeforeEach
    void setUp(KafkaProperties testKafkaProperties) {
        this.kafkaProperties = testKafkaProperties;
        this.kafkaConfiguration = new ModernKafkaConfiguration(configSupport);
    }

    @Test
    void shouldCreateConsumerFactory() {
        when(configSupport.buildConsumerConfig(any())).thenReturn(Map.of("bootstrap.servers", "localhost:9093"));
        ConsumerFactory<String, Object> factory = kafkaConfiguration.kafkaConsumerFactory(kafkaProperties);
        assertNotNull(factory);
        verify(configSupport).buildConsumerConfig(kafkaProperties);
    }

    @Test
    void shouldCreateProducerFactory() {
        when(configSupport.buildProducerConfig(any())).thenReturn(Map.of("bootstrap.servers", "localhost:9093"));
        ProducerFactory<String, Object> factory = kafkaConfiguration.kafkaProducerFactory(kafkaProperties);
        assertNotNull(factory);
        verify(configSupport).buildProducerConfig(kafkaProperties);
    }

    @Test
    void shouldCreateKafkaTemplate() {
        ProducerFactory producerFactory = mock(ProducerFactory.class);
        KafkaTemplate template = kafkaConfiguration.kafkaTemplate(producerFactory);
        assertNotNull(template);
    }

    @Test
    void shouldCreateListenerContainerWithManualAck() {
        ConsumerFactory<String, Object> consumerFactory = mock(ConsumerFactory.class);
        var factory = kafkaConfiguration.kafkaListenerContainerFactory(consumerFactory);
        assertNotNull(factory);
        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE, factory.getContainerProperties().getAckMode());
    }

    @Test
    void shouldSetConfigurationSupportOnInit() {
        // Given - Mock KafkaConfigUtils static method to verify it gets called
        try (MockedStatic<KafkaConfigUtils> mockedKafkaConfigUtils = mockStatic(KafkaConfigUtils.class)) {
            // Configure the mock to do nothing when the static method is called
            mockedKafkaConfigUtils.when(() -> KafkaConfigUtils.setConfigurationSupport(any())).then(invocation -> null);
            
            // When - Call the @PostConstruct method
            kafkaConfiguration.init();
            
            // Then - Verify the static method was called with the correct argument
            mockedKafkaConfigUtils.verify(() -> KafkaConfigUtils.setConfigurationSupport(configSupport));
        }
    }
}