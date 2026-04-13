package com.gamepulse.infra.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "games")
public class GameDocument {

    @Id
    private Long steamAppId;
    // ES용 ID. spring data annotation의 @Id 사용

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String genre;
    // Keyword = 정확한 매칭. "Action,RPG" 전체를 하나의 값으로

    @Field(type = FieldType.Text, analyzer = "standard")
    private String tags;
    // Text = 분석 후 인덱싱. "Open World" → "open", "world" 각각 인덱싱

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    // 생성자
    public GameDocument() {}

    // Game Entity에서 변환하는 정적 팩토리 메서드
    public static GameDocument from(com.gamepulse.domain.game.Game game) {
        GameDocument doc = new GameDocument();
        doc.steamAppId = game.getSteamAppId();
        doc.title = game.getTitle();
        doc.description = game.getDescription();
        doc.genre = game.getGenre();
        doc.tags = game.getTags();
        doc.thumbnailUrl = game.getThumbnailUrl();
        return doc;
    }

    // Getter/Setter
    public Long getSteamAppId() { return steamAppId; }
    public void setSteamAppId(Long steamAppId) { this.steamAppId = steamAppId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}