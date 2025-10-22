package com.tiger.pocs.kafka.config;

import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.support.KafkaTestParameterResolver;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, KafkaTestParameterResolver.class})
class KafkaConfigurationSupportTest {

    private KafkaConfigurationSupport configSupport;
    private KafkaProperties kafkaProperties;

    @BeforeEach
    void setUp(KafkaProperties testKafkaProperties) {
        this.configSupport = new KafkaConfigurationSupport();
        this.kafkaProperties = testKafkaProperties;
    }

    @Test
    void shouldBuildConsumerConfigWithBasicSettings() {
        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        assertEquals("localhost:9093", config.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("test-group", config.get(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals("SASL_PLAINTEXT", config.get("security.protocol"));
        assertTrue(config.get(SaslConfigs.SASL_JAAS_CONFIG).toString().contains("test-key"));
    }

    @Test
    void shouldBuildProducerConfigWithDifferentCredentials() {
        Map<String, Object> config = configSupport.buildProducerConfig(kafkaProperties);
        assertEquals("localhost:9093", config.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(JsonSerializer.class, config.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        assertEquals("SASL_PLAINTEXT", config.get("security.protocol"));
        assertTrue(config.get(SaslConfigs.SASL_JAAS_CONFIG).toString().contains("producer-key"));
    }

    @Test
    void shouldMaskSensitiveDataInSanitizedConfig() {
        KafkaProperties sanitized = configSupport.sanitizeConfiguration(kafkaProperties);
        assertEquals("localhost:9093", sanitized.getBootstrapServers());
        assertEquals("******", sanitized.getSecurity().getApiKey());
        assertEquals("******", sanitized.getSecurity().getApiSecret());
    }

    @Test
    void shouldReturnTopicInformation() {
        var topicInfo = configSupport.getTopicInfo(kafkaProperties);
        assertEquals(2, topicInfo.size());
        assertTrue(topicInfo.containsKey("clientsTopic"));
        assertEquals("clientsTopic", topicInfo.get("clientsTopic").getName());
    }

    @Test
    void shouldDetectKafkaConnection() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        when(template.metrics()).thenReturn(Map.of());
        assertTrue(configSupport.isConnected(template));
    }

    @Test
    void shouldHandleConnectionFailure() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        when(template.metrics()).thenThrow(new RuntimeException("Connection failed"));
        assertFalse(configSupport.isConnected(template));
    }

    @Test
    void shouldBuildAdminConfig() {
        Map<String, Object> config = configSupport.buildAdminConfig(kafkaProperties);
        assertEquals("localhost:9093", config.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("SASL_PLAINTEXT", config.get("security.protocol"));
        assertTrue(config.get(SaslConfigs.SASL_JAAS_CONFIG).toString().contains("test-key"));
    }

    @Test
    void shouldValidateTopicExists() {
        // Mock the AdminClient creation to avoid real Kafka connection
        try (MockedStatic<AdminClient> mockedAdminClient = mockStatic(AdminClient.class)) {
            AdminClient mockAdminClient = mock(AdminClient.class);
            mockedAdminClient.when(() -> AdminClient.create(any(Map.class))).thenReturn(mockAdminClient);
            
            // Mock the admin client to throw exception (simulating connection failure)
            when(mockAdminClient.listTopics()).thenThrow(new RuntimeException("Connection failed"));
            
            boolean exists = configSupport.validateTopicExists("test-topic", kafkaProperties);
            assertFalse(exists);
            verify(mockAdminClient).close();
        }
    }

    @Test
    void shouldValidateTopicExistsWhenTopicFound() throws Exception {
        // Given
        AdminClient mockAdminClient = mock(AdminClient.class);
        ListTopicsResult mockListTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> mockFuture = mock(KafkaFuture.class);
        Set<String> topicNames = Set.of("test-topic", "other-topic");

        when(mockListTopicsResult.names()).thenReturn(mockFuture);
        when(mockFuture.get(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(topicNames);
        when(mockAdminClient.listTopics()).thenReturn(mockListTopicsResult);

        try (MockedStatic<AdminClient> mockedAdminClient = mockStatic(AdminClient.class)) {
            mockedAdminClient.when(() -> AdminClient.create(any(Map.class))).thenReturn(mockAdminClient);

            // When
            boolean exists = configSupport.validateTopicExists("test-topic", kafkaProperties);

            // Then
            assertTrue(exists);
            verify(mockAdminClient).close();
        }
    }

    @Test
    void shouldValidateTopicExistsWhenTopicNotFound() throws Exception {
        // Given
        AdminClient mockAdminClient = mock(AdminClient.class);
        ListTopicsResult mockListTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> mockFuture = mock(KafkaFuture.class);
        Set<String> topicNames = Set.of("other-topic", "another-topic");

        when(mockListTopicsResult.names()).thenReturn(mockFuture);
        when(mockFuture.get(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(topicNames);
        when(mockAdminClient.listTopics()).thenReturn(mockListTopicsResult);

        try (MockedStatic<AdminClient> mockedAdminClient = mockStatic(AdminClient.class)) {
            mockedAdminClient.when(() -> AdminClient.create(any(Map.class))).thenReturn(mockAdminClient);

            // When
            boolean exists = configSupport.validateTopicExists("test-topic", kafkaProperties);

            // Then
            assertFalse(exists);
            verify(mockAdminClient).close();
        }
    }

    @Test
    void shouldHandleTimeoutExceptionInValidateTopicExists() throws Exception {
        // Given
        AdminClient mockAdminClient = mock(AdminClient.class);
        ListTopicsResult mockListTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> mockFuture = mock(KafkaFuture.class);

        when(mockListTopicsResult.names()).thenReturn(mockFuture);
        when(mockFuture.get(5, java.util.concurrent.TimeUnit.SECONDS))
                .thenThrow(new java.util.concurrent.TimeoutException("Timeout"));
        when(mockAdminClient.listTopics()).thenReturn(mockListTopicsResult);

        try (MockedStatic<AdminClient> mockedAdminClient = mockStatic(AdminClient.class)) {
            mockedAdminClient.when(() -> AdminClient.create(any(Map.class))).thenReturn(mockAdminClient);

            // When
            boolean exists = configSupport.validateTopicExists("test-topic", kafkaProperties);

            // Then
            assertFalse(exists);
            verify(mockAdminClient).close();
        }
    }

    @Test
    void shouldHandleValidateTopicMethod() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        // Should not throw exception (backward compatibility method)
        assertDoesNotThrow(() -> 
            configSupport.validateTopic("test-topic", template, kafkaProperties));
    }

    @Test
    void shouldSkipValidationWhenBootstrapServersIsEmpty() {
        // Given
        kafkaProperties.setBootstrapServers("");
        
        // When
        boolean exists = configSupport.validateTopicExists("test-topic", kafkaProperties);
        
        // Then
        assertFalse(exists);
    }

    @Test
    void shouldSkipValidationWhenBootstrapServersIsNull() {
        // Given
        kafkaProperties.setBootstrapServers(null);
        
        // When
        boolean exists = configSupport.validateTopicExists("test-topic", kafkaProperties);
        
        // Then
        assertFalse(exists);
    }

    @Test
    void shouldHandleNullSaslMechanism() {
        kafkaProperties.getSecurity().setSaslMechanism(null);
        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        assertFalse(config.containsKey(SaslConfigs.SASL_MECHANISM));
        assertFalse(config.containsKey(SaslConfigs.SASL_JAAS_CONFIG));
    }

    @Test
    void shouldHandleNullTruststoreLocation() {
        kafkaProperties.getSecurity().setTruststoreLocation(null);
        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        assertFalse(config.containsKey(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
    }

    @Test
    void shouldHandleDisabledSecurity() {
        kafkaProperties.getSecurity().setEnabled(false);
        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        assertFalse(config.containsKey("security.protocol"));
        assertFalse(config.containsKey(SaslConfigs.SASL_MECHANISM));
    }

    @Test
    void shouldHandleDisabledProducerSecurity() {
        kafkaProperties.getProducer().getSecurity().setEnabled(false);
        Map<String, Object> config = configSupport.buildProducerConfig(kafkaProperties);
        assertFalse(config.containsKey("security.protocol"));
        assertFalse(config.containsKey(SaslConfigs.SASL_MECHANISM));
    }

    @Test
    void shouldHandleNullValues() {
        var consumer = kafkaProperties.getConsumer();
        consumer.setKeyDeserializer(null);
        consumer.setValueDeserializer(null);
        consumer.setAutoOffsetReset(null);
        consumer.setSessionTimeout(null);
        consumer.setHeartbeatInterval(null);
        consumer.setMaxPollInterval(null);

        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        assertEquals(StringDeserializer.class, config.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, config.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals("earliest", config.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        
        // Null durations should not be added to config
        assertFalse(config.containsKey(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG));
        assertFalse(config.containsKey(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
        assertFalse(config.containsKey(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
    }

    @Test
    void shouldMaskEmptyStrings() {
        kafkaProperties.getSecurity().setApiKey("");
        kafkaProperties.getSecurity().setApiSecret("");
        kafkaProperties.getSecurity().setTruststoreLocation("");
        kafkaProperties.getSecurity().setTruststorePassword("");

        KafkaProperties sanitized = configSupport.sanitizeConfiguration(kafkaProperties);
        assertEquals("", sanitized.getSecurity().getApiKey());
        assertEquals("", sanitized.getSecurity().getApiSecret());
        assertEquals("", sanitized.getSecurity().getTruststoreLocation());
        assertEquals("", sanitized.getSecurity().getTruststorePassword());
    }

    @Test
    void shouldMaskNullValues() {
        kafkaProperties.getSecurity().setApiKey(null);
        kafkaProperties.getSecurity().setApiSecret(null);
        kafkaProperties.getSecurity().setTruststoreLocation(null);
        kafkaProperties.getSecurity().setTruststorePassword(null);

        KafkaProperties sanitized = configSupport.sanitizeConfiguration(kafkaProperties);
        assertNull(sanitized.getSecurity().getApiKey());
        assertNull(sanitized.getSecurity().getApiSecret());
        assertNull(sanitized.getSecurity().getTruststoreLocation());
        assertNull(sanitized.getSecurity().getTruststorePassword());
    }

    @Test
    void shouldCopyAllBasicProperties() {
        KafkaProperties sanitized = configSupport.sanitizeConfiguration(kafkaProperties);
        assertEquals(kafkaProperties.getBootstrapServers(), sanitized.getBootstrapServers());
        assertEquals(kafkaProperties.getDefaultGroupId(), sanitized.getDefaultGroupId());
        assertEquals(kafkaProperties.getTopics(), sanitized.getTopics());
        assertEquals(kafkaProperties.getConsumer(), sanitized.getConsumer());
    }

    @Test
    void shouldCopyAllSecurityProperties() {
        KafkaProperties sanitized = configSupport.sanitizeConfiguration(kafkaProperties);
        assertEquals(kafkaProperties.getSecurity().isEnabled(), sanitized.getSecurity().isEnabled());
        assertEquals(kafkaProperties.getSecurity().getProtocol(), sanitized.getSecurity().getProtocol());
        assertEquals(kafkaProperties.getSecurity().getSaslMechanism(), sanitized.getSecurity().getSaslMechanism());
        assertEquals(kafkaProperties.getSecurity().getEndpointIdentificationAlgorithm(), sanitized.getSecurity().getEndpointIdentificationAlgorithm());
        assertEquals(kafkaProperties.getSecurity().isSkipHostnameVerification(), sanitized.getSecurity().isSkipHostnameVerification());
        assertEquals(kafkaProperties.getSecurity().getTruststoreType(), sanitized.getSecurity().getTruststoreType());
        assertEquals(kafkaProperties.getSecurity().isCheckHostname(), sanitized.getSecurity().isCheckHostname());
    }

    @Test
    void shouldHandleNullTruststoreTypeInSslConfig() {
        // Keep truststoreLocation so SSL config is added, but set truststoreType to null
        kafkaProperties.getSecurity().setTruststoreType(null);
        // Ensure truststoreLocation is not null so SSL config is processed
        kafkaProperties.getSecurity().setTruststoreLocation("/path/to/truststore");
        
        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        assertEquals("JKS", config.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
    }

    @Test
    void shouldBuildCorrectJaasConfig() {
        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        String jaasConfig = (String) config.get(SaslConfigs.SASL_JAAS_CONFIG);
        String expected = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test-key\" password=\"test-secret\";";
        assertEquals(expected, jaasConfig);
    }

    @Test
    void shouldTransformMapCorrectly() {
        var result = configSupport.getTopicInfo(kafkaProperties);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("clientsTopic"));
        assertTrue(result.containsKey("accountsTopic"));
        
        assertEquals("clientsTopic", result.get("clientsTopic").getName());
        assertEquals("test-clients-group", result.get("clientsTopic").getGroupId());
        assertTrue(result.get("clientsTopic").isAutoSubscribe());
    }

    @Test
    void shouldConfigureTimeoutValuesWhenPresent() {
        var consumer = kafkaProperties.getConsumer();
        consumer.setSessionTimeout(java.time.Duration.ofSeconds(30));
        consumer.setHeartbeatInterval(java.time.Duration.ofSeconds(5));
        consumer.setMaxPollInterval(java.time.Duration.ofMinutes(5));

        Map<String, Object> config = configSupport.buildConsumerConfig(kafkaProperties);
        
        assertEquals(30000, config.get(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG));
        assertEquals(5000, config.get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
        assertEquals(300000, config.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
    }
}