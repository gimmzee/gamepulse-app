package com.gamepulse.infra.steam;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
public class SteamApiClient {

    private final WebClient webClient;

    public SteamApiClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://store.steampowered.com")
                .build();
    }

    public Map<String, Object> getGameDetail(Long appId) {
        return webClient.get()
                .uri("/api/appdetails?appids={appId}&cc=kr&l=korean", appId)
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/120.0.0.0 Safari/537.36")
                // Steam API가 봇 요청을 차단할 때 브라우저처럼 보이게 함
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}