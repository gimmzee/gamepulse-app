package com.gamepulse.infra.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class GameEventConsumer {

    @KafkaListener(topics = "game-price-events", groupId = "gamepulse-group")
    public void consume(String message) {
        System.out.println("Price event received: " + message);
        // TODO: Elasticsearch 통계 적재
        // TODO: 최저가 달성 시 이메일 알림
    }
}