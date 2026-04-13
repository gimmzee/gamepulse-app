package com.gamepulse.infra.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface GameEsRepository
        extends ElasticsearchRepository<GameDocument, Long> {

    List<GameDocument> findByGenreContaining(String genre);
    List<GameDocument> findByTagsContaining(String tag);
}