package com.tiger.pocs.kafka;

import com.tiger.pocs.kafka.config.KafkaConfigUtils;
import com.tiger.pocs.kafka.domain.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final ApplicationContext applicationContext;

    public void publishPayload(String topicName, Object payload) {
        applicationContext.getBean(KafkaProducer.class).publishPayload(topicName, null, payload);
    }

    public boolean validateAndPublishPayload(String topicName, String key, Object payload) {
        if (!isTopicConfigured(topicName)) {
            logTopicNotConfiguredWarning(topicName);
        }

        if (!validateTopicExists(topicName)) {
            logTopicNotExist(topicName);
            return false;
        }

        sendMessageToTopic(topicName, key, payload);
        return true;
    }

    @Async
    public void publishPayload(String topicName, String key, Object payload) {
        if (!isTopicConfigured(topicName)) {
            logTopicNotConfiguredWarning(topicName);
        }

        if (!validateTopicExists(topicName)) {
            logTopicNotExist(topicName);
            return;
        }

        sendMessageToTopic(topicName, key, payload);
    }

    private boolean isTopicConfigured(String topicName) {
        return kafkaProperties.getTopics().values().stream()
                .anyMatch(topicConfig -> topicName.equals(topicConfig.getName()));
    }

    private boolean validateTopicExists(String topicName) {
        try {
            return KafkaConfigUtils.doesTopicExist(topicName, kafkaProperties);
        } catch (Exception e) {
            logTopicValidationError(topicName, e.getMessage());
            return false;
        }
    }

    private void logTopicNotConfiguredWarning(String topicName) {
        log.info("Publishing to topic '{}' which is not in configured topics list", topicName);
    }

    private void logTopicNotExist(String topicName) {
        log.error("Cannot publish message to topic '{}' - topic does not exist in Kafka cluster. " +
                "Please create the topic first or check your topic configuration.", topicName);
    }

    private void logTopicValidationError(String topicName, String errorMessage) {
        log.warn("Failed to validate existence of topic '{}': {}", topicName, errorMessage);
    }

    private void sendMessageToTopic(String topicName, String key, Object payload) {
        try {
            ProducerRecord<String, Object> messageRecord = createProducerRecord(topicName, key, payload);
            kafkaTemplate.send(messageRecord).whenComplete((sendResult, exception) ->
                    handleSendResult(topicName, key, sendResult, exception));
        } catch (Exception exception) {
            logSendError(topicName, key, exception);
        }
    }

    private ProducerRecord<String, Object> createProducerRecord(String topicName, String key, Object payload) {
        return new ProducerRecord<>(topicName, key, payload);
    }

    private void handleSendResult(
            String topicName, String key, SendResult<String, Object> sendResult, Throwable exception) {

        if (exception == null) {
            logSuccessfulSend(topicName, key, sendResult);
        } else {
            logSendFailure(topicName, key, exception);
        }
    }

    private void logSuccessfulSend(String topicName, String key, SendResult<String, Object> sendResult) {
        log.info("Successfully published payload to topic '{}' with key '{}' at offset '{}'",
                topicName, key, sendResult.getRecordMetadata().offset());
    }

    private void logSendFailure(String topicName, String key, Throwable exception) {
        log.error("Failed to publish payload to topic '{}' with key '{}'", topicName, key, exception);
    }

    private void logSendError(String topicName, String key, Exception exception) {
        log.error("Error publishing payload to topic '{}' with key '{}'", topicName, key, exception);
    }
}