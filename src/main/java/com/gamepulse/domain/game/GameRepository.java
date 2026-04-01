package com.gamepulse.domain.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByTitleContainingIgnoreCase(String title);
    List<Game> findByCurrentPriceLessThanEqual(Integer maxPrice);
}