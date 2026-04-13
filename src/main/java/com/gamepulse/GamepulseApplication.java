package com.gamepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@RestController
@EnableJpaRepositories(basePackages = {
        "com.gamepulse.domain"
        // domain 패키지만 JPA 스캔 (infra.es 제외)
})
@EnableElasticsearchRepositories(basePackages = {
        "com.gamepulse.infra.es"
        // ES Repository는 infra.es 패키지만 스캔
})
public class GamepulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamepulseApplication.class, args);
    }

    @GetMapping("/health")
    public String health() {
        return "GamePulse is running";
    }
}
