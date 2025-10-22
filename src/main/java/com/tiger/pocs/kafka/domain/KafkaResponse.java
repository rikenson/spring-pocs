package com.tiger.pocs.kafka.domain;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

public class KafkaResponse {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishResponse {
        private String status;
        private String message;
        private String topic;
        private String key;
        private Long timestamp;

        public static PublishResponse success(String topic, String key) {
            return PublishResponse.builder()
                    .status("success")
                    .message("Payload published successfully")
                    .topic(topic)
                    .key(key)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        public static PublishResponse error(String topic, String key, String errorMessage) {
            return PublishResponse.builder()
                    .status("error")
                    .message("Failed to publish payload: " + errorMessage)
                    .topic(topic)
                    .key(key)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private boolean kafkaConnected;
        private String bootstrapServers;
        private Map<String, TopicInfo> configuredTopics;
        private Long timestamp;
        private String error;

        public static HealthResponse up(
                boolean kafkaConnected, String bootstrapServers, Map<String, TopicInfo> configuredTopics) {
            return HealthResponse.builder()
                    .status("UP")
                    .kafkaConnected(kafkaConnected)
                    .bootstrapServers(bootstrapServers)
                    .configuredTopics(configuredTopics)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        public static HealthResponse down(String errorMessage) {
            return HealthResponse.builder()
                    .status("DOWN")
                    .error(errorMessage)
                    .timestamp(System.currentTimeMillis())
                    .kafkaConnected(false)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicInfo {
        private String name;
        private String groupId;
        private boolean autoSubscribe;

        public static TopicInfo from(KafkaProperties.TopicConfig topicConfig) {
            return TopicInfo.builder()
                    .name(topicConfig.getName())
                    .groupId(topicConfig.getGroupId())
                    .autoSubscribe(topicConfig.isAutoSubscribe())
                    .build();
        }
    }
}