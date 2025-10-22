package com.tiger.pocs.kafka.config;

import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.domain.KafkaResponse;
import com.tiger.pocs.kafka.support.KafkaTestParameterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith({MockitoExtension.class, KafkaTestParameterResolver.class})
class KafkaConfigUtilsTest {

    @Mock
    private KafkaConfigurationSupport mockConfigSupport;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    private KafkaProperties kafkaProperties;

    @BeforeEach
    void setUp(KafkaProperties testKafkaProperties) {
        this.kafkaProperties = testKafkaProperties;
        KafkaConfigUtils.setConfigurationSupport(mockConfigSupport);
    }

    @Test
    void shouldCreateSanitizedConfiguration() {
        // Given
        KafkaProperties sanitizedProps = new KafkaProperties();
        when(mockConfigSupport.sanitizeConfiguration(kafkaProperties)).thenReturn(sanitizedProps);

        // When
        KafkaProperties result = KafkaConfigUtils.createSanitizedConfiguration(kafkaProperties);

        // Then
        assertSame(sanitizedProps, result);
    }

    @Test
    void shouldValidateTopicExists() {
        // Given - mock the validateTopic method to avoid real Kafka connections
        doNothing().when(mockConfigSupport).validateTopic(anyString(), any(KafkaTemplate.class), any(KafkaProperties.class));
        
        // Ensure our mock is set before the test
        KafkaConfigUtils.setConfigurationSupport(mockConfigSupport);
        
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> 
            KafkaConfigUtils.validateTopicExists("testTopic", kafkaTemplate, kafkaProperties));
        
        // Verify the mock was called
        verify(mockConfigSupport).validateTopic("testTopic", kafkaTemplate, kafkaProperties);
    }

    @Test
    void shouldGetConfiguredTopicsInfo() {
        // Given
        Map<String, KafkaResponse.TopicInfo> expectedTopics = Map.of("topic1", 
            new KafkaResponse.TopicInfo("topic1", "group1", true));
        when(mockConfigSupport.getTopicInfo(kafkaProperties)).thenReturn(expectedTopics);

        // When
        var result = KafkaConfigUtils.getConfiguredTopicsInfo(kafkaProperties);

        // Then
        assertEquals(expectedTopics, result);
    }

    @Test
    void shouldCheckKafkaConnection() {
        // Given
        when(mockConfigSupport.isConnected(kafkaTemplate)).thenReturn(true);

        // When
        boolean result = KafkaConfigUtils.isKafkaConnected(kafkaTemplate);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldCheckIfTopicExists() {
        // Given
        when(mockConfigSupport.validateTopicExists("testTopic", kafkaProperties)).thenReturn(true);

        // When
        boolean result = KafkaConfigUtils.doesTopicExist("testTopic", kafkaProperties);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldCreateConfigSupportWhenNull() {
        // Given
        KafkaConfigUtils.setConfigurationSupport(null);
        
        // Set bootstrap servers to DISABLED to prevent real connections
        kafkaProperties.setBootstrapServers("DISABLED");

        // When/Then - should create new instance and not throw
        assertDoesNotThrow(() -> KafkaConfigUtils.doesTopicExist("topic", kafkaProperties));
    }
}