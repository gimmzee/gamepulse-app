package com.gamepulse.service;

import com.gamepulse.domain.game.Game;
import com.gamepulse.infra.es.GameDocument;
import com.gamepulse.infra.es.GameEsRepository;
import com.gamepulse.domain.game.GameRepository;
import com.gamepulse.infra.steam.SteamApiClient;
import com.gamepulse.infra.kafka.GameEventProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PriceService {

    private final GameRepository gameRepository;
    private final SteamApiClient steamApiClient;
    private final GameEventProducer eventProducer;
    private final GameService gameService;
    private final GameEsRepository gameEsRepository;

    public PriceService(GameRepository gameRepository,
                        SteamApiClient steamApiClient,
                        GameEventProducer eventProducer,
                        GameService gameService, GameEsRepository gameEsRepository) {
        this.gameRepository = gameRepository;
        this.steamApiClient = steamApiClient;
        this.eventProducer = eventProducer;
        this.gameService = gameService;
        this.gameEsRepository = gameEsRepository;
    }

    @Scheduled(fixedRate = 3600000)
    public void collectPrices() {
        List<Game> games = gameRepository.findAll();
        for (Game game : games) {
            try {
                Map<String, Object> data =
                        steamApiClient.getGameDetail(game.getSteamAppId());
                Integer newPrice = extractPrice(data, game.getSteamAppId());
                if (newPrice != null && !newPrice.equals(game.getCurrentPrice())) {
                    gameService.updatePrice(game, newPrice);
                }
            } catch (Exception e) {
                System.err.println("Price fetch failed: "
                        + game.getSteamAppId() + " - " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Integer extractPrice(Map<String, Object> response, Long appId) {
        try {
            Map<String, Object> appData =
                    (Map<String, Object>) response.get(String.valueOf(appId));
            if (appData == null) return null;
            if (!Boolean.TRUE.equals(appData.get("success"))) return null;

            Map<String, Object> data = (Map<String, Object>) appData.get("data");
            if (data == null) return null;

            Map<String, Object> priceOverview =
                    (Map<String, Object>) data.get("price_overview");
            if (priceOverview == null) return 0;

            Object finalPrice = priceOverview.get("final");
            if (finalPrice instanceof Integer) {
                return (Integer) finalPrice / 100;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
// 매일 새벽 2시 실행
    public void collectPopularGames() {
        try {
            // Steam 인기 게임 목록 조회
            Map<String, Object> response = WebClient.builder()
                    .baseUrl("https://api.steampowered.com")
                    .build()
                    .get()
                    .uri("/ISteamChartsService/GetMostPlayedGames/v1/")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return;

            Map<String, Object> steamResponse = (Map<String, Object>) response.get("response");
            if (steamResponse == null) return;

            List<Map<String, Object>> ranks =
                    (List<Map<String, Object>>) steamResponse.get("ranks");
            if (ranks == null) return;

            for (Map<String, Object> rank : ranks) {
                Long appId = Long.valueOf(rank.get("appid").toString());

                // 이미 DB에 있으면 건너뜀
                if (gameRepository.existsById(appId)) continue;

                // Steam API로 게임 상세 정보 조회
                try {
                    Map<String, Object> detail = steamApiClient.getGameDetail(appId);
                    if (detail == null) continue;

                    Map<String, Object> appData =
                            (Map<String, Object>) detail.get(String.valueOf(appId));
                    if (appData == null ||
                            !Boolean.TRUE.equals(appData.get("success"))) continue;

                    Map<String, Object> data =
                            (Map<String, Object>) appData.get("data");
                    if (data == null) continue;

                    // 게임 타입만 수집 (DLC, 번들 제외)
                    String type = (String) data.get("type");
                    if (!"game".equals(type)) continue;

                    String title = (String) data.get("name");
                    if (title == null) continue;

                    // 가격 파싱
                    Integer price = 0;
                    Map<String, Object> priceOverview =
                            (Map<String, Object>) data.get("price_overview");
                    if (priceOverview != null) {
                        Object finalPrice = priceOverview.get("final");
                        if (finalPrice instanceof Integer) {
                            price = (Integer) finalPrice / 100;
                        }
                    }

                    // 썸네일
                    String thumbnail =
                            "https://cdn.akamai.steamstatic.com/steam/apps/"
                                    + appId + "/header.jpg";

                    // 장르 추출 (기존 코드)
                    String genreStr = null;
                    List<Map<String, Object>> genres =
                            (List<Map<String, Object>>) data.get("genres");
                    if (genres != null) {
                        genreStr = genres.stream()
                                .map(g -> (String) g.get("description"))
                                .collect(Collectors.joining(","));
                    }

                    // tags 추출 (추가)
                    String tagsStr = null;
                    List<Map<String, Object>> categories =
                            (List<Map<String, Object>>) data.get("categories");
                    if (categories != null) {
                        tagsStr = categories.stream()
                                .map(c -> (String) c.get("description"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining(","));
                    }

                    // description 추출 (추가)
                    String description = (String) data.get("short_description");

                    // DB 저장
                    Game game = new Game(appId, title, price);
                    game.setThumbnailUrl(thumbnail);
                    game.setGenre(genreStr);
                    game.setTags(tagsStr);
                    game.setDescription(description);
                    // DB 저장
                    gameRepository.save(game);
                    // ES 인덱싱 (동기화)
                    gameEsRepository.save(GameDocument.from(game));
                    // DB에 저장할 때 ES에도 자동으로 인덱싱

                    System.out.println("New game added: " + title);

                    // Steam API 과부하 방지
                    Thread.sleep(200);

                } catch (Exception e) {
                    System.err.println("Failed to fetch game " + appId
                            + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Popular games collection failed: "
                    + e.getMessage());
        }
    }
}