package com.gamepulse.api;

import com.gamepulse.domain.game.Game;
import com.gamepulse.domain.game.GamePrice;
import com.gamepulse.service.GameService;
import com.gamepulse.service.ItadService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final ItadService itadService;


    public GameController(GameService gameService, ItadService itadService) {
        this.gameService = gameService;
        this.itadService = itadService;
    }

    @GetMapping("/search")
    public List<Game> search(@RequestParam String q) {
        return gameService.search(q);
    }

    @GetMapping("/{appId}")
    public Game getGame(@PathVariable Long appId) {
        return gameService.getGame(appId);
    }

    @GetMapping("/{appId}/prices")
    public List<GamePrice> getPriceHistory(@PathVariable Long appId) {
        return gameService.getPriceHistory(appId);
    }

    @GetMapping("/recommend")
    public List<Game> recommend(@RequestParam List<Long> likedAppIds) {
        return gameService.recommend(likedAppIds);
    }

    @PostMapping("/{appId}/alert")
    public String setAlert(
            @PathVariable Long appId,
            @RequestParam Integer targetPrice,
            @RequestParam String email
    ) {
        gameService.setAlert(appId, targetPrice, email);
        return "Alert registered for " + email;
    }

    // 플랫폼별 현재 가격 비교
    @GetMapping("/{appId}/platform-prices")
    public Map<String, Object> getPlatformPrices(@PathVariable Long appId) {
        return itadService.getPlatformPrices(appId);
    }

    // 가격 개요 (현재 최저가 + 역대 최저가)
    @GetMapping("/{appId}/price-overview")
    public Map getPriceOverview(@PathVariable Long appId) {
        return itadService.getPriceOverview(appId);
    }
}