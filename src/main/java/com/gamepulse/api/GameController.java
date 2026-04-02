package com.gamepulse.api;

import com.gamepulse.domain.game.Game;
import com.gamepulse.domain.game.GamePrice;
import com.gamepulse.service.GameService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
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
}