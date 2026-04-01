package com.gamepulse.domain.alert;

import com.gamepulse.domain.game.Game;
import jakarta.persistence.*;

@Entity
@Table(name = "price_alerts")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steam_app_id")
    private Game game;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Integer targetPrice;

    private boolean notified = false;

    protected PriceAlert() {}

    public PriceAlert(Game game, String email, Integer targetPrice) {
        this.game = game;
        this.email = email;
        this.targetPrice = targetPrice;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public String getEmail() { return email; }
    public Integer getTargetPrice() { return targetPrice; }
    public boolean isNotified() { return notified; }
    public void markNotified() { this.notified = true; }
}