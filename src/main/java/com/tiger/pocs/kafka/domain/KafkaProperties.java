package com.tiger.pocs.kafka.domain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Data
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    
    private String bootstrapServers;
    private String defaultGroupId;
    private Security security = new Security();
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    private Map<String, TopicConfig> topics = new HashMap<>();
    
    @Data
    public static class Security {
        private boolean enabled;
        private String protocol;
        private String saslMechanism;
        private String apiKey;
        private String apiSecret;
        private String truststoreLocation;
        private String truststorePassword;
        private String endpointIdentificationAlgorithm;
        private boolean skipHostnameVerification;
        private String truststoreType;
        private boolean checkHostname;
    }
    
    @Data
    public static class Consumer {
        private String keyDeserializer;
        private String valueDeserializer;
        private String autoOffsetReset;
        private boolean enableAutoCommit;
        private Duration sessionTimeout;
        private Duration heartbeatInterval;
        private int maxPollRecords;
        private Duration maxPollInterval;
        private Duration reconnectBackoff;
        private Duration retryBackoff;
        private int requestTimeout;
    }
    
    @Data
    public static class Producer {
        private Security security = new Security();
    }
    
    @Data
    public static class TopicConfig {
        private String name;
        private String groupId;
        private boolean autoSubscribe;
    }
}