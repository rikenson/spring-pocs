package com.tiger.pocs.tester;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerConfigTest {

    @Test
    void consumerFactory_shouldContainConfiguredProperties() {
        KafkaConsumerConfig cfg = new KafkaConsumerConfig();

        // inject values comme ferait Spring avec @Value
        ReflectionTestUtils.setField(cfg, "bootstrapAddress", "broker1:9092");
        ReflectionTestUtils.setField(cfg, "groupId", "my-group");

        ConsumerFactory<String, String> factory = cfg.consumerFactory();
        assertNotNull(factory);
        assertTrue(factory instanceof DefaultKafkaConsumerFactory);

        @SuppressWarnings("unchecked")
        Map<String, Object> props = ((DefaultKafkaConsumerFactory<String, String>) factory).getConfigurationProperties();
        assertEquals("broker1:9092", props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("my-group", props.get(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals("earliest", props.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    @Test
    void kafkaListenerContainerFactory_shouldUseConsumerFactory() {
        KafkaConsumerConfig cfg = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(cfg, "bootstrapAddress", "broker1:9092");
        ReflectionTestUtils.setField(cfg, "groupId", "g");

        ConcurrentKafkaListenerContainerFactory<String, String> containerFactory = cfg.kafkaListenerContainerFactory();
        assertNotNull(containerFactory);
        assertNotNull(containerFactory.getConsumerFactory());
        // consumerFactory must produce the same type as consumerFactory() method
        assertTrue(containerFactory.getConsumerFactory() instanceof DefaultKafkaConsumerFactory);
    }
}
