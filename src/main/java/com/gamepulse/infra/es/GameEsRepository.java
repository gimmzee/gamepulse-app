package com.gamepulse.infra.es;

import com.gamepulse.domain.game.Game;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface GameEsRepository
        extends ElasticsearchRepository<Game, Long> {

    List<Game> findByGenreContaining(String genre);
    List<Game> findByTagsContaining(String tag);
}