package com.gamepulse.infra.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameEventProducer {

    private static final String TOPIC = "game-price-events";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public GameEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPriceEvent(Long appId, Integer oldPrice, Integer newPrice) {
        String message = String.format(
                "{\"appId\":%d,\"oldPrice\":%d,\"newPrice\":%d,\"timestamp\":\"%s\"}",
                appId, oldPrice, newPrice, java.time.Instant.now()
        );
        kafkaTemplate.send(TOPIC, String.valueOf(appId), message);
    }
}