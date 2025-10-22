package com.tiger.pocs.kafka.config;

import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.domain.KafkaResponse;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

public final class KafkaConfigUtils {

    private static KafkaConfigurationSupport configSupport;

    private KafkaConfigUtils() {
        // Utility class
    }

    public static void setConfigurationSupport(KafkaConfigurationSupport support) {
        configSupport = support;
    }

    public static KafkaProperties createSanitizedConfiguration(KafkaProperties originalProperties) {
        return getConfigSupport().sanitizeConfiguration(originalProperties);
    }

    public static void validateTopicExists(String topic, KafkaTemplate<String, Object> kafkaTemplate, KafkaProperties kafkaProperties) {
        getConfigSupport().validateTopic(topic, kafkaTemplate, kafkaProperties);
    }

    public static Map<String, KafkaResponse.TopicInfo> getConfiguredTopicsInfo(KafkaProperties kafkaProperties) {
        return getConfigSupport().getTopicInfo(kafkaProperties);
    }

    public static boolean isKafkaConnected(KafkaTemplate<String, Object> kafkaTemplate) {
        return getConfigSupport().isConnected(kafkaTemplate);
    }

    public static boolean doesTopicExist(String topic, KafkaProperties properties) {
        return getConfigSupport().validateTopicExists(topic, properties);
    }

    private static KafkaConfigurationSupport getConfigSupport() {
        if (configSupport == null) {
            configSupport = new KafkaConfigurationSupport();
        }
        return configSupport;
    }
}