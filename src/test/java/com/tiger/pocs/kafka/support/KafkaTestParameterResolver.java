package com.tiger.pocs.kafka.support;

import com.tiger.pocs.kafka.domain.KafkaProperties;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

public class KafkaTestParameterResolver implements ParameterResolver {

    private final Map<Class<?>, Supplier<Object>> parameterCreators = Map.of(
            KafkaProperties.class, this::createTestKafkaProperties,
            KafkaTemplate.class, this::createMockKafkaTemplate,
            ApplicationContext.class, this::createMockApplicationContext
    );

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterCreators.containsKey(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return parameterCreators.getOrDefault(parameterType, this::throwUnsupportedType).get();
    }

    private Object throwUnsupportedType() {
        throw new IllegalArgumentException("Unsupported parameter type");
    }

    private KafkaProperties createTestKafkaProperties() {
        var properties = new KafkaProperties();
        properties.setBootstrapServers("localhost:9093"); // Test value for unit tests 
        properties.setDefaultGroupId("test-group");

        properties.setSecurity(createConsumerSecurity());
        properties.setProducer(createProducerWithSecurity());
        properties.setConsumer(createConsumerSettings());
        properties.setTopics(createTestTopics());

        return properties;
    }

    private KafkaProperties.Security createConsumerSecurity() {
        var security = new KafkaProperties.Security();
        security.setEnabled(true);
        security.setProtocol("SASL_PLAINTEXT");
        security.setSaslMechanism("PLAIN");
        security.setApiKey("test-key");
        security.setApiSecret("test-secret");
        return security;
    }

    private KafkaProperties.Producer createProducerWithSecurity() {
        var producer = new KafkaProperties.Producer();
        producer.setSecurity(createProducerSecurity());
        return producer;
    }

    private KafkaProperties.Security createProducerSecurity() {
        var security = new KafkaProperties.Security();
        security.setEnabled(true);
        security.setProtocol("SASL_PLAINTEXT");
        security.setSaslMechanism("PLAIN");
        security.setApiKey("producer-key");
        security.setApiSecret("producer-secret");
        return security;
    }

    private KafkaProperties.Consumer createConsumerSettings() {
        var consumer = new KafkaProperties.Consumer();
        consumer.setKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        consumer.setValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        consumer.setAutoOffsetReset("earliest");
        consumer.setEnableAutoCommit(false);
        consumer.setSessionTimeout(Duration.ofSeconds(30));
        consumer.setHeartbeatInterval(Duration.ofSeconds(3));
        consumer.setMaxPollRecords(500);
        consumer.setRequestTimeout(30000);
        return consumer;
    }

    private Map<String, KafkaProperties.TopicConfig> createTestTopics() {
        return Map.of(
                "clientsTopic", createTopicConfig("clientsTopic", "test-clients-group"),
                "accountsTopic", createTopicConfig("accountsTopic", "test-accounts-group")
        );
    }

    private KafkaProperties.TopicConfig createTopicConfig(String name, String groupId) {
        var topic = new KafkaProperties.TopicConfig();
        topic.setName(name);
        topic.setGroupId(groupId);
        topic.setAutoSubscribe(true);
        return topic;
    }

    private KafkaTemplate<String, Object> createMockKafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    private ApplicationContext createMockApplicationContext() {
        return Mockito.mock(ApplicationContext.class);
    }
}