package com.gamepulse.infra.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class GameEventConsumer {

    @KafkaListener(topics = "game-price-events", groupId = "gamepulse-group")
    public void consume(String message) {
        System.out.println("Price event received: " + message);
        // 추후 Elasticsearch 적재 및 알림 발송 구현
    }
}