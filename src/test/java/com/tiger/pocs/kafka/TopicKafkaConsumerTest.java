package com.tiger.pocs.kafka;

import com.tiger.pocs.kafka.domain.KafkaMessage;
import com.tiger.pocs.kafka.processor.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicKafkaConsumerTest {

    @Mock
    private ClientEventProcessor clientEventProcessor;
    
    @Mock
    private AccountEventProcessor accountEventProcessor;
    
    @Mock
    private UserEventProcessor userEventProcessor;
    
    @Mock
    private LogOffsetEventProcessor logOffsetEventProcessor;
    
    @Mock
    private UserAccessEventProcessor userAccessEventProcessor;
    
    @Mock
    private Acknowledgment acknowledgment;
    
    private TopicKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TopicKafkaConsumer(
            clientEventProcessor,
            accountEventProcessor, 
            userEventProcessor,
            logOffsetEventProcessor,
            userAccessEventProcessor
        );
    }

    @Test
    void shouldHandleClientsMessageSuccessfully() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("clients-topic", "client-key", "client-value");
        ArgumentCaptor<KafkaMessage<String>> messageCaptor = ArgumentCaptor.forClass(KafkaMessage.class);

        // When
        consumer.handleClientsMessage(record, acknowledgment);

        // Then
        verify(clientEventProcessor).processClientMessage(messageCaptor.capture());
        verify(acknowledgment).acknowledge();
        
        KafkaMessage<String> capturedMessage = messageCaptor.getValue();
        assertEquals("client-key", capturedMessage.getKey());
        assertEquals("client-value", capturedMessage.getValue());
        assertEquals("clients-topic", capturedMessage.getTopic());
        assertEquals(0, capturedMessage.getPartition());
        assertEquals(100L, capturedMessage.getOffset());
    }

    @Test
    void shouldHandleAccountsMessageSuccessfully() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("accounts-topic", "account-key", "account-value");
        ArgumentCaptor<KafkaMessage<String>> messageCaptor = ArgumentCaptor.forClass(KafkaMessage.class);

        // When
        consumer.handleAccountsMessage(record, acknowledgment);

        // Then
        verify(accountEventProcessor).processAccountMessage(messageCaptor.capture());
        verify(acknowledgment).acknowledge();
        
        KafkaMessage<String> capturedMessage = messageCaptor.getValue();
        assertEquals("account-key", capturedMessage.getKey());
        assertEquals("account-value", capturedMessage.getValue());
    }

    @Test
    void shouldHandleUsersMessageSuccessfully() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("users-topic", "user-key", "user-value");

        // When
        consumer.handleUsersMessage(record, acknowledgment);

        // Then
        verify(userEventProcessor).processUserMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleLogOffsetMessageSuccessfully() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("log-offset-topic", "log-key", "log-value");

        // When
        consumer.handleLogOffsetMessage(record, acknowledgment);

        // Then
        verify(logOffsetEventProcessor).processLogOffsetMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleUserAccessMessageSuccessfully() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("user-access-topic", "access-key", "access-value");

        // When
        consumer.handleUserAccessMessage(record, acknowledgment);

        // Then
        verify(userAccessEventProcessor).processUserAccessMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldConvertConsumerRecordWithHeaders() {
        // Given - create record with actual headers
        Headers headers = new RecordHeaders();
        headers.add("header1", "value1".getBytes());
        headers.add("header2", "value2".getBytes());
        
        ConsumerRecord<String, String> record = new ConsumerRecord<String, String>(
            "test-topic", 0, 100L, 1000L, null, 0, 0, "test-key", "test-value", headers, null
        );

        // When
        consumer.handleClientsMessage(record, acknowledgment);

        // Then
        ArgumentCaptor<KafkaMessage<String>> messageCaptor = ArgumentCaptor.forClass(KafkaMessage.class);
        verify(clientEventProcessor).processClientMessage(messageCaptor.capture());
        
        KafkaMessage<String> message = messageCaptor.getValue();
        assertNotNull(message.getHeaders());
        assertFalse(message.getHeaders().isEmpty());
        assertEquals("value1", message.getHeaders().get("header1"));
        assertEquals("value2", message.getHeaders().get("header2"));
        assertEquals(2, message.getHeaders().size());
    }

    @Test
    void shouldHandleProcessingExceptionAndStillAcknowledge() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("clients-topic", "key", "value");
        doThrow(new RuntimeException("Processing failed")).when(clientEventProcessor).processClientMessage(any());

        // When
        consumer.handleClientsMessage(record, acknowledgment);

        // Then
        verify(clientEventProcessor).processClientMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge(); // Should still acknowledge to avoid infinite retries
    }

    @Test
    void shouldHandleAccountProcessingExceptionAndStillAcknowledge() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("accounts-topic", "key", "value");
        doThrow(new RuntimeException("Account processing failed")).when(accountEventProcessor).processAccountMessage(any());

        // When
        consumer.handleAccountsMessage(record, acknowledgment);

        // Then
        verify(accountEventProcessor).processAccountMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleUserProcessingExceptionAndStillAcknowledge() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("users-topic", "key", "value");
        doThrow(new RuntimeException("User processing failed")).when(userEventProcessor).processUserMessage(any());

        // When
        consumer.handleUsersMessage(record, acknowledgment);

        // Then
        verify(userEventProcessor).processUserMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleLogOffsetProcessingExceptionAndStillAcknowledge() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("log-offset-topic", "key", "value");
        doThrow(new RuntimeException("Log offset processing failed")).when(logOffsetEventProcessor).processLogOffsetMessage(any());

        // When
        consumer.handleLogOffsetMessage(record, acknowledgment);

        // Then
        verify(logOffsetEventProcessor).processLogOffsetMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleUserAccessProcessingExceptionAndStillAcknowledge() {
        // Given
        ConsumerRecord<String, String> record = createConsumerRecord("user-access-topic", "key", "value");
        doThrow(new RuntimeException("User access processing failed")).when(userAccessEventProcessor).processUserAccessMessage(any());

        // When
        consumer.handleUserAccessMessage(record, acknowledgment);

        // Then
        verify(userAccessEventProcessor).processUserAccessMessage(any(KafkaMessage.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleNullKeyInConsumerRecord() {
        // Given
        ConsumerRecord<String, String> record = new ConsumerRecord<String, String>(
                "test-topic", 0, 100L, null, "test-value"
        );

        // When
        consumer.handleClientsMessage(record, acknowledgment);

        // Then
        ArgumentCaptor<KafkaMessage<String>> messageCaptor = ArgumentCaptor.forClass(KafkaMessage.class);
        verify(clientEventProcessor).processClientMessage(messageCaptor.capture());
        
        KafkaMessage<String> message = messageCaptor.getValue();
        assertNull(message.getKey());
        assertEquals("test-value", message.getValue());
    }

    @Test
    void shouldHandleEmptyHeaders() {
        // Given
        ConsumerRecord<String, String> record = new ConsumerRecord<String, String>(
                "test-topic", 0, 100L, "key", "value"
        );

        // When
        consumer.handleClientsMessage(record, acknowledgment);

        // Then
        ArgumentCaptor<KafkaMessage<String>> messageCaptor = ArgumentCaptor.forClass(KafkaMessage.class);
        verify(clientEventProcessor).processClientMessage(messageCaptor.capture());
        
        KafkaMessage<String> message = messageCaptor.getValue();
        assertNotNull(message.getHeaders());
        assertTrue(message.getHeaders().isEmpty());
    }

    @Test 
    void shouldValidateTimestampConversion() {
        // Given
        long timestamp = 1634567890123L;
        ConsumerRecord<String, String> record = new ConsumerRecord<String, String>(
                "test-topic", 0, 100L, timestamp, null, 0, 0, "key", "value", new RecordHeaders(), null
        );

        // When
        consumer.handleClientsMessage(record, acknowledgment);

        // Then
        ArgumentCaptor<KafkaMessage<String>> messageCaptor = ArgumentCaptor.forClass(KafkaMessage.class);
        verify(clientEventProcessor).processClientMessage(messageCaptor.capture());
        
        KafkaMessage<String> message = messageCaptor.getValue();
        assertEquals(Instant.ofEpochMilli(timestamp), message.getTimestamp());
    }

    private ConsumerRecord<String, String> createConsumerRecord(String topic, String key, String value) {
        return new ConsumerRecord<String, String>(topic, 0, 100L, key, value);
    }
}