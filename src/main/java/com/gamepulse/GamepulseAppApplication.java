package com.gamepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@RestController
public class GamepulseAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamepulseAppApplication.class, args);
    }

    @GetMapping("/health")
    public String health() {
        return "GamePulse is running";
    }
}
