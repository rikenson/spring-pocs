package com.tiger.pocs.kafka;

import com.tiger.pocs.kafka.config.KafkaConfigUtils;
import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.domain.KafkaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/kafka")
@RequiredArgsConstructor
public class KafkaController {

    private final KafkaProducer kafkaProducer;
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping
    public ResponseEntity<KafkaResponse.PublishResponse> publishPayload(
            @RequestParam String topic, @RequestParam(required = false) String key, @RequestBody Object payload) {

        try {
            boolean published = kafkaProducer.validateAndPublishPayload(topic, key, payload);
            
            if (!published) {
                log.warn("Failed to publish payload to topic '{}' - topic does not exist", topic);
                return ResponseEntity.badRequest()
                        .body(KafkaResponse.PublishResponse.error(topic, key, 
                            "Topic '" + topic + "' does not exist in Kafka cluster. Please create the topic first."));
            }
            
            log.info("Successfully accepted payload for publishing to topic '{}' via REST endpoint", topic);
            return ResponseEntity.ok(KafkaResponse.PublishResponse.success(topic, key));

        } catch (Exception exception) {
            log.error("Failed to accept payload for topic '{}' via REST endpoint", topic, exception);
            return ResponseEntity.internalServerError()
                    .body(KafkaResponse.PublishResponse.error(topic, key, exception.getMessage()));
        }
    }

    @GetMapping("/configuration")
    public ResponseEntity<KafkaProperties> getKafkaConfiguration() {
        return ResponseEntity.ok(KafkaConfigUtils.createSanitizedConfiguration(kafkaProperties));
    }

    @GetMapping("/health")
    public ResponseEntity<KafkaResponse.HealthResponse> getKafkaHealth() {
        try {
            boolean kafkaConnected = KafkaConfigUtils.isKafkaConnected(kafkaTemplate);
            Map<String, KafkaResponse.TopicInfo> configuredTopics = KafkaConfigUtils.getConfiguredTopicsInfo(kafkaProperties);

            KafkaResponse.HealthResponse response = KafkaResponse.HealthResponse.up(
                    kafkaConnected, kafkaProperties.getBootstrapServers(), configuredTopics);

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            log.error("Kafka health check failed", exception);
            return ResponseEntity.internalServerError()
                    .body(KafkaResponse.HealthResponse.down(exception.getMessage()));
        }
    }


}