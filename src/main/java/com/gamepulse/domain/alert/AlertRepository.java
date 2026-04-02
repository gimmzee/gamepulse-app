package com.gamepulse.domain.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByGameSteamAppIdAndNotifiedFalse(Long steamAppId);
    List<PriceAlert> findByEmail(String email);
}