package com.gamepulse.service;

import com.gamepulse.domain.game.Game;
import com.gamepulse.domain.game.GameRepository;
import com.gamepulse.infra.steam.SteamApiClient;
import com.gamepulse.infra.kafka.GameEventProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class PriceService {

    private final GameRepository gameRepository;
    private final SteamApiClient steamApiClient;
    private final GameEventProducer eventProducer;
    private final GameService gameService;

    public PriceService(GameRepository gameRepository,
                        SteamApiClient steamApiClient,
                        GameEventProducer eventProducer,
                        GameService gameService) {
        this.gameRepository = gameRepository;
        this.steamApiClient = steamApiClient;
        this.eventProducer = eventProducer;
        this.gameService = gameService;
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
}