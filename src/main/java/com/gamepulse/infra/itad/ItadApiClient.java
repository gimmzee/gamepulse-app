package com.gamepulse.infra.itad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Component
public class ItadApiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String country;

    public ItadApiClient(
            @Value("${itad.api.base-url}") String baseUrl,
            @Value("${itad.api.key}") String apiKey,
            @Value("${itad.api.country}") String country
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.country = country;
    }

    // Steam appId로 ITAD game ID 조회
    public String lookupGameId(Long steamAppId) {
        Map response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/lookup/v1")
                        .queryParam("appid", steamAppId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) return null;
        Boolean found = (Boolean) response.get("found");
        if (!Boolean.TRUE.equals(found)) return null;

        Map game = (Map) response.get("game");
        return game != null ? (String) game.get("id") : null;
    }

    // 여러 플랫폼 현재 가격 조회
    public List<Map> getPrices(List<String> itadIds) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/prices/v3")
                        .queryParam("key", apiKey)
                        .queryParam("country", country)
                        .build())
                .bodyValue(itadIds)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();
    }

    // 역대 최저가 조회
    public List<Map> getHistoryLow(List<String> itadIds) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/historylow/v1")
                        .queryParam("key", apiKey)
                        .queryParam("country", country)
                        .build())
                .bodyValue(itadIds)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();
    }

    // 가격 개요 (현재 최저가 + 역대 최저가 한 번에)
    public Map getPriceOverview(List<String> itadIds) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/overview/v2")
                        .queryParam("key", apiKey)
                        .queryParam("country", country)
                        .build())
                .bodyValue(itadIds)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    // 제목으로 게임 검색
    public List<Map> searchGames(String title) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/search/v1")
                        .queryParam("title", title)
                        .queryParam("key", apiKey)
                        .queryParam("results", 10)
                        .build())
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();
    }
    // 플랫폼별 가격 이력 (기본 최근 3개월)
    public List<Map> getGamePriceHistory(String itadId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/history/v2")
                        .queryParam("id", itadId)
                        .queryParam("key", apiKey)
                        .queryParam("country", country)
                        .build())
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();
    }
}