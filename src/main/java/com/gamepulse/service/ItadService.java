package com.gamepulse.service;

import com.gamepulse.infra.itad.ItadApiClient;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ItadService {

    private final ItadApiClient itadApiClient;

    public ItadService(ItadApiClient itadApiClient) {
        this.itadApiClient = itadApiClient;
    }

    // Steam appId로 플랫폼별 현재 가격 조회
    public Map<String, Object> getPlatformPrices(Long steamAppId) {
        String itadId = itadApiClient.lookupGameId(steamAppId);
        if (itadId == null) return Map.of("error", "Game not found on ITAD");

        List<Map> prices = itadApiClient.getPrices(List.of(itadId));
        List<Map> historyLow = itadApiClient.getHistoryLow(List.of(itadId));

        Map<String, Object> result = new HashMap<>();
        result.put("itadId", itadId);
        result.put("prices", prices);
        result.put("historyLow", historyLow);
        return result;
    }

    // 가격 개요 (현재 최저가 + 역대 최저가)
    public Map getPriceOverview(Long steamAppId) {
        String itadId = itadApiClient.lookupGameId(steamAppId);
        if (itadId == null) return Map.of("error", "Game not found on ITAD");

        return itadApiClient.getPriceOverview(List.of(itadId));
    }

    public List<Map> getGamePriceHistory(Long steamAppId) {
        String itadId = itadApiClient.lookupGameId(steamAppId);
        if (itadId == null) return List.of();
        return itadApiClient.getGamePriceHistory(itadId);
    }
}