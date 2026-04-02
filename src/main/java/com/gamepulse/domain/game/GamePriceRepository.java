package com.gamepulse.domain.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GamePriceRepository extends JpaRepository<GamePrice, Long> {

    List<GamePrice> findByGameSteamAppIdOrderByRecordedAtDesc(Long steamAppId);
}