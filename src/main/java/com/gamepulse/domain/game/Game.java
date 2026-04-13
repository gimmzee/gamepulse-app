package com.gamepulse.domain.game;

import jakarta.persistence.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
// ES는 GameDocument가 담당
public class Game {

    @Id
    private Long steamAppId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String genre;
    private String thumbnailUrl;

    @Column(nullable = false)
    private Integer currentPrice;

    private Integer lowestPrice;
    private LocalDateTime lowestPriceAt;
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String tags;

    protected Game() {}

    public Game(Long steamAppId, String title, Integer currentPrice) {
        this.steamAppId = steamAppId;
        this.title = title;
        this.currentPrice = currentPrice;
        this.lowestPrice = currentPrice;
        this.lowestPriceAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getSteamAppId() { return steamAppId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getGenre() { return genre; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Integer getCurrentPrice() { return currentPrice; }
    public Integer getLowestPrice() { return lowestPrice; }
    public LocalDateTime getLowestPriceAt() { return lowestPriceAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getTags() { return tags; }

    public void updatePrice(Integer newPrice) {
        this.currentPrice = newPrice;
        this.updatedAt = LocalDateTime.now();
        if (lowestPrice == null || newPrice < lowestPrice) {
            this.lowestPrice = newPrice;
            this.lowestPriceAt = LocalDateTime.now();
        }
    }

    public void setDescription(String description) { this.description = description; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setTags(String tags) { this.tags = tags; }
}