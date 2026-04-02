package com.gamepulse.domain.game;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_prices")
public class GamePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steam_app_id")
    private Game game;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    protected GamePrice() {}

    public GamePrice(Game game, Integer price) {
        this.game = game;
        this.price = price;
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public Integer getPrice() { return price; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}