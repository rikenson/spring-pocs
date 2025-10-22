package com.tiger.pocs.kafka;

import com.tiger.pocs.kafka.config.KafkaConfigUtils;
import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.support.KafkaTestParameterResolver;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, KafkaTestParameterResolver.class})
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private SendResult<String, Object> sendResult;
    @Mock
    private RecordMetadata recordMetadata;
    private KafkaProducer kafkaProducer;
    private KafkaProperties kafkaProperties;

    @BeforeEach
    void setUp(KafkaProperties testKafkaProperties) {
        this.kafkaProperties = testKafkaProperties;
        this.kafkaProducer = new KafkaProducer(kafkaTemplate, kafkaProperties, applicationContext);
        lenient().when(applicationContext.getBean(KafkaProducer.class)).thenReturn(kafkaProducer);
    }

    @Test
    void shouldPublishPayloadSuccessfully() {
        String topicName = "clientsTopic";
        Object payload = "test-payload";
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topicName, kafkaProperties)).thenReturn(true);
            boolean result = kafkaProducer.validateAndPublishPayload(topicName, "key", payload);
            assertTrue(result);
            verify(kafkaTemplate).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldFailWhenTopicDoesNotExist() {
        String topicName = "nonExistentTopic";
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topicName, kafkaProperties)).thenReturn(false);
            boolean result = kafkaProducer.validateAndPublishPayload(topicName, "key", "payload");
            assertFalse(result);
            verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldHandleExceptionsGracefully() {
        // Given
        String topicName = "clientsTopic";
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new RuntimeException("Kafka error"));
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topicName, kafkaProperties)).thenReturn(true);
            assertDoesNotThrow(() -> kafkaProducer.publishPayload(topicName, "key", "payload"));
        }
    }

    @Test
    void shouldValidateTopicConfiguration() {
        String configuredTopic = "clientsTopic";
        String unconfiguredTopic = "randomTopic";

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(anyString(), any())).thenReturn(true);
            kafkaProducer.publishPayload(configuredTopic, "key", "payload");
            kafkaProducer.publishPayload(unconfiguredTopic, "key", "payload");
            verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldHandleNullPayload() {
        // Given
        String topic = "clientsTopic";
        String key = "test-key";
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties)).thenReturn(true);
            
            // When
            kafkaProducer.publishPayload(topic, key, null);
            
            // Then
            verify(kafkaTemplate).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldHandleEmptyTopicName() {
        // Given
        String emptyTopic = "";
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(emptyTopic, kafkaProperties)).thenReturn(false);
            
            // When
            kafkaProducer.publishPayload(emptyTopic, "key", "payload");
            
            // Then
            verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldHandleEmptyKey() {
        // Given
        String topic = "clientsTopic";
        String emptyKey = "";
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties)).thenReturn(true);
            
            // When
            kafkaProducer.publishPayload(topic, emptyKey, "payload");
            
            // Then
            verify(kafkaTemplate).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldReturnFalseWhenValidateAndPublishFailsDueToTopicValidation() {
        // Given
        String topic = "invalidTopic";
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties)).thenReturn(false);
            
            // When
            boolean result = kafkaProducer.validateAndPublishPayload(topic, "key", "payload");
            
            // Then
            assertFalse(result);
            verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldReturnTrueWhenValidateAndPublishSucceeds() {
        // Given
        String topic = "validTopic";
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties)).thenReturn(true);
            
            // When
            boolean result = kafkaProducer.validateAndPublishPayload(topic, "key", "payload");
            
            // Then
            assertTrue(result);
            verify(kafkaTemplate).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldCallTwoParameterPublishPayloadMethod() {
        // Given
        String topic = "clientsTopic";
        Object payload = "test-payload";
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties)).thenReturn(true);
            
            // When
            kafkaProducer.publishPayload(topic, payload);
            
            // Then
            verify(applicationContext).getBean(KafkaProducer.class);
            verify(kafkaTemplate).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldHandleExceptionInTopicValidation() {
        // Given
        String topic = "problematicTopic";
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties))
                     .thenThrow(new RuntimeException("Validation error"));
            
            // When
            boolean result = kafkaProducer.validateAndPublishPayload(topic, "key", "payload");
            
            // Then
            assertFalse(result);
            verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldHandleExceptionInAsyncTopicValidation() {
        // Given
        String topic = "problematicTopic";
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties))
                     .thenThrow(new RuntimeException("Validation error"));
            
            // When
            kafkaProducer.publishPayload(topic, "key", "payload");
            
            // Then
            verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        }
    }

    @Test
    void shouldHandleSuccessfulAsyncSend() throws Exception {
        // Given
        String topic = "clientsTopic";
        String key = "test-key";
        Object payload = "test-payload";
        
        when(recordMetadata.offset()).thenReturn(12345L);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties)).thenReturn(true);
            
            // When
            kafkaProducer.publishPayload(topic, key, payload);
            
            // Complete the future with success
            future.complete(sendResult);
            
            // Then
            verify(kafkaTemplate).send(any(ProducerRecord.class));
            verify(sendResult).getRecordMetadata();
            verify(recordMetadata).offset();
        }
    }

    @Test
    void shouldHandleFailedAsyncSend() throws Exception {
        // Given
        String topic = "clientsTopic";
        String key = "test-key";
        Object payload = "test-payload";
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.doesTopicExist(topic, kafkaProperties)).thenReturn(true);
            
            // When
            kafkaProducer.publishPayload(topic, key, payload);
            
            // Complete the future with exception
            RuntimeException exception = new RuntimeException("Send failed");
            future.completeExceptionally(exception);
            
            // Then
            verify(kafkaTemplate).send(any(ProducerRecord.class));
        }
    }
}