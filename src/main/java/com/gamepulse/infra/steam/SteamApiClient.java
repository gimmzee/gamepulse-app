package com.gamepulse.infra.steam;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
public class SteamApiClient {

    private final WebClient webClient;

    public SteamApiClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://store.steampowered.com")
                .build();
    }

    public Map<String, Object> getGameDetail(Long appId) {
        return webClient.get()
                .uri("/api/appdetails?appids={appId}&cc=kr&l=korean", appId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}