package com.gamepulse.service;

import com.gamepulse.infra.itad.ItadApiClient;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

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

    public List<Map> searchGames(String keyword) {
        return itadApiClient.searchGames(keyword);
    }

    public List<Map<String, Object>> getCurrentDeals() {
        List<Map> deals = itadApiClient.getDeals();
        return deals.stream()
                .map(deal -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("title", deal.get("title"));

                    // 썸네일
                    Map assets = (Map) deal.get("assets");
                    if (assets != null) {
                        result.put("thumbnail", assets.get("banner300"));
                    }

                    // 가격 정보
                    Map dealInfo = (Map) deal.get("deal");
                    if (dealInfo != null) {
                        Map price = (Map) dealInfo.get("price");
                        Map regular = (Map) dealInfo.get("regular");
                        Map historyLow = (Map) dealInfo.get("historyLow");

                        result.put("currentPrice",
                                price != null ? price.get("amount") : 0);
                        result.put("regularPrice",
                                regular != null ? regular.get("amount") : 0);
                        result.put("cut", dealInfo.get("cut"));
                        result.put("shopName",
                                ((Map) dealInfo.get("shop")).get("name"));
                        result.put("url", dealInfo.get("url"));
                        result.put("historyLow",
                                historyLow != null ? historyLow.get("amount") : null);
                    }
                    return result;
                })
                .collect(Collectors.toList());
    }
}