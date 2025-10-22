package com.tiger.pocs.kafka;

import com.tiger.pocs.kafka.domain.KafkaMessage;
import com.tiger.pocs.kafka.processor.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class TopicKafkaConsumer {

    private final ClientEventProcessor clientEventProcessor;
    private final AccountEventProcessor accountEventProcessor;
    private final UserEventProcessor userEventProcessor;
    private final LogOffsetEventProcessor logOffsetEventProcessor;
    private final UserAccessEventProcessor userAccessEventProcessor;

    @KafkaListener(topics = "${CLIENTS_TOPIC_NAME}", groupId = "${CLIENTS_TOPIC_GROUP_ID}")
    public void handleClientsMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processMessage(consumerRecord, acknowledgment, "🟦", "clients",
                clientEventProcessor::processClientMessage);
    }

    @KafkaListener(topics = "${ACCOUNTS_TOPIC_NAME}", groupId = "${ACCOUNTS_TOPIC_GROUP_ID}")
    public void handleAccountsMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processMessage(consumerRecord, acknowledgment, "🟪", "accounts",
                accountEventProcessor::processAccountMessage);
    }

    @KafkaListener(topics = "${USERS_TOPIC_NAME}", groupId = "${USERS_TOPIC_GROUP_ID}")
    public void handleUsersMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processMessage(consumerRecord, acknowledgment, "🟩", "users",
                userEventProcessor::processUserMessage);
    }

    @KafkaListener(topics = "${LOG_OFFSET_TOPIC_NAME}", groupId = "${LOG_OFFSET_TOPIC_GROUP_ID}")
    public void handleLogOffsetMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processMessage(consumerRecord, acknowledgment, "🟨", "log offset",
                logOffsetEventProcessor::processLogOffsetMessage);
    }

    @KafkaListener(topics = "${USER_ACCESS_TOPIC_NAME}", groupId = "${USER_ACCESS_TOPIC_GROUP_ID}")
    public void handleUserAccessMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processMessage(consumerRecord, acknowledgment, "🟧", "user access",
                userAccessEventProcessor::processUserAccessMessage);
    }


    private KafkaMessage<String> convertToKafkaMessage(ConsumerRecord<String, String> consumerRecord) {
        Map<String, String> headers = new HashMap<>();
        consumerRecord.headers().forEach(header ->
                headers.put(header.key(), new String(header.value()))
        );

        return KafkaMessage.<String>builder()
                .key(consumerRecord.key())
                .value(consumerRecord.value())
                .topic(consumerRecord.topic())
                .partition(consumerRecord.partition())
                .offset(consumerRecord.offset())
                .timestamp(Instant.ofEpochMilli(consumerRecord.timestamp()))
                .headers(headers)
                .build();
    }



    private void processMessage(
            ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment, String icon,
            String messageType, MessageProcessor processor) {

        log.info("{} [CONSUMER] Received {} message from topic: {}, partition: {}, offset: {}",
                icon, messageType, consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());

        try {
            KafkaMessage<String> message = convertToKafkaMessage(consumerRecord);
            processor.process(message);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("❌ [CONSUMER] Failed to process {} message: {}", messageType, e.getMessage(), e);
            acknowledgment.acknowledge(); // Still acknowledge to avoid infinite retries
        }
    }


    @FunctionalInterface
    private interface MessageProcessor {
        void process(KafkaMessage<String> message);
    }
}