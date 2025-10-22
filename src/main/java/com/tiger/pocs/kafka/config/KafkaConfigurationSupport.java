package com.tiger.pocs.kafka.config;

import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.domain.KafkaResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KafkaConfigurationSupport {

    private static final String MASKED_VALUE = "******";

    public Map<String, Object> buildConsumerConfig(KafkaProperties properties) {
        return buildConfig(properties, this::addConsumerSpecificConfig);
    }

    public Map<String, Object> buildProducerConfig(KafkaProperties properties) {
        return buildProducerConfig(properties, this::addProducerSpecificConfig);
    }

    public Map<String, Object> buildAdminConfig(KafkaProperties properties) {
        var config = new HashMap<String, Object>();
        addBootstrapConfig(config, properties);
        addSecurityConfig(config, properties);
        return config;
    }

    public KafkaProperties sanitizeConfiguration(KafkaProperties original) {
        var sanitized = copyBasicProperties(original);
        sanitized.setSecurity(sanitizeSecurity(original.getSecurity()));
        return sanitized;
    }

    public Map<String, KafkaResponse.TopicInfo> getTopicInfo(KafkaProperties properties) {
        return transformMap(
                properties.getTopics(),
                Map.Entry::getKey,
                entry -> KafkaResponse.TopicInfo.from(entry.getValue()));
    }

    public boolean isConnected(KafkaTemplate<String, Object> kafkaTemplate) {
        try {
            kafkaTemplate.metrics();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateTopicExists(String topic, KafkaProperties properties) {
        // For test environments, avoid creating AdminClient if bootstrap servers are disabled
        if (properties.getBootstrapServers() == null || 
            properties.getBootstrapServers().equals("DISABLED") ||
            properties.getBootstrapServers().isEmpty()) {
            log.debug("Kafka disabled for tests - skipping topic validation for: {}", topic);
            return false;
        }
        
        try (AdminClient adminClient = AdminClient.create(buildAdminConfig(properties))) {
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> topicNames = topicsResult.names().get(5, TimeUnit.SECONDS);
            return topicNames.contains(topic);
        } catch (Exception e) {
            log.warn("Failed to validate topic '{}' existence: {}", topic, e.getMessage());
            return false;
        }
    }

    public void validateTopic(String topic, KafkaTemplate<String, Object> kafkaTemplate, KafkaProperties properties) {
        // Keep the existing method for backward compatibility
    }

    private Map<String, Object> buildConfig(
            KafkaProperties properties, BiConsumer<Map<String, Object>, KafkaProperties> specificConfigurer) {
        var config = new HashMap<String, Object>();

        addBootstrapConfig(config, properties);
        addSecurityConfig(config, properties);
        specificConfigurer.accept(config, properties);

        return config;
    }

    private Map<String, Object> buildProducerConfig(
            KafkaProperties properties, BiConsumer<Map<String, Object>, KafkaProperties> specificConfigurer) {
        var config = new HashMap<String, Object>();

        addBootstrapConfig(config, properties);
        addProducerSecurityConfig(config, properties);
        specificConfigurer.accept(config, properties);

        return config;
    }

    private void addBootstrapConfig(Map<String, Object> config, KafkaProperties properties) {
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
    }

    private void addConsumerSpecificConfig(Map<String, Object> config, KafkaProperties properties) {
        var consumer = properties.getConsumer();

        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getDefaultGroupId());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, getValueOrDefault(consumer.getKeyDeserializer(), StringDeserializer.class));
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, getValueOrDefault(consumer.getValueDeserializer(), StringDeserializer.class));
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, getValueOrDefault(consumer.getAutoOffsetReset(), "earliest"));
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumer.isEnableAutoCommit());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumer.getMaxPollRecords());
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumer.getRequestTimeout());

        addTimeoutConfig(config, consumer);
    }

    private void addProducerSpecificConfig(Map<String, Object> config, KafkaProperties properties) {
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
    }

    private void addTimeoutConfig(Map<String, Object> config, KafkaProperties.Consumer consumer) {
        putIfNotNull(config, ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
                consumer.getSessionTimeout(), duration -> (int) duration.toMillis());
        putIfNotNull(config, ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                consumer.getHeartbeatInterval(), duration -> (int) duration.toMillis());
        putIfNotNull(config, ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                consumer.getMaxPollInterval(), duration -> (int) duration.toMillis());
    }

    private void addSecurityConfig(Map<String, Object> config, KafkaProperties properties) {
        var security = properties.getSecurity();
        if (!security.isEnabled()) return;

        config.put("security.protocol", security.getProtocol());

        addSaslConfig(config, security);
        addSslConfig(config, security);
    }

    private void addProducerSecurityConfig(Map<String, Object> config, KafkaProperties properties) {
        var producerSecurity = properties.getProducer().getSecurity();
        if (!producerSecurity.isEnabled()) return;

        config.put("security.protocol", producerSecurity.getProtocol());

        addSaslConfig(config, producerSecurity);
        addSslConfig(config, producerSecurity);
    }

    private void addSaslConfig(Map<String, Object> config, KafkaProperties.Security security) {
        if (security.getSaslMechanism() == null) return;

        config.put(SaslConfigs.SASL_MECHANISM, security.getSaslMechanism());
        config.put(SaslConfigs.SASL_JAAS_CONFIG, buildJaasConfig(security));
    }

    private void addSslConfig(Map<String, Object> config, KafkaProperties.Security security) {
        if (security.getTruststoreLocation() == null) return;

        config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, security.getTruststoreLocation());
        config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, security.getTruststorePassword());
        config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, getValueOrDefault(security.getTruststoreType(), "JKS"));
        config.put("ssl.check.hostname", security.isCheckHostname());
        config.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, security.getEndpointIdentificationAlgorithm());
    }

    private String buildJaasConfig(KafkaProperties.Security security) {
        return String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                security.getApiKey(), security.getApiSecret());
    }

    private KafkaProperties copyBasicProperties(KafkaProperties original) {
        var copy = new KafkaProperties();
        copy.setBootstrapServers(original.getBootstrapServers());
        copy.setDefaultGroupId(original.getDefaultGroupId());
        copy.setTopics(original.getTopics());
        copy.setConsumer(original.getConsumer());
        return copy;
    }

    private KafkaProperties.Security sanitizeSecurity(KafkaProperties.Security original) {
        var sanitized = new KafkaProperties.Security();

        // Copy non-sensitive properties
        sanitized.setEnabled(original.isEnabled());
        sanitized.setProtocol(original.getProtocol());
        sanitized.setSaslMechanism(original.getSaslMechanism());
        sanitized.setEndpointIdentificationAlgorithm(original.getEndpointIdentificationAlgorithm());
        sanitized.setSkipHostnameVerification(original.isSkipHostnameVerification());
        sanitized.setTruststoreType(original.getTruststoreType());
        sanitized.setCheckHostname(original.isCheckHostname());

        // Mask sensitive properties
        sanitized.setApiKey(maskIfPresent(original.getApiKey()));
        sanitized.setApiSecret(maskIfPresent(original.getApiSecret()));
        sanitized.setTruststoreLocation(maskIfPresent(original.getTruststoreLocation()));
        sanitized.setTruststorePassword(maskIfPresent(original.getTruststorePassword()));

        return sanitized;
    }

    private String maskIfPresent(String value) {
        return value != null && !value.isEmpty() ? MASKED_VALUE : value;
    }

    private <K, V, R> Map<K, R> transformMap(
            Map<K, V> source,
            Function<Map.Entry<K, V>, K> keyMapper,
            Function<Map.Entry<K, V>, R> valueMapper) {
        return source.entrySet().stream()
                .collect(Collectors.toMap(keyMapper, valueMapper));
    }

    private <T> T getValueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private <T, R> void putIfNotNull(Map<String, Object> config, String key, T value, Function<T, R> transformer) {
        if (value != null) {
            config.put(key, transformer.apply(value));
        }
    }
}