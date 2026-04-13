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

    public List<Map> searchGames(String keyword) {
        return itadApiClient.searchGames(keyword);
    }

    public List<Map<String, Object>> getCurrentDeals() {
        // ITAD /deals/v2 API 호출
        // country=KR, sort=-cut (할인율 높은 순)
        List<Map> deals = itadApiClient.getDeals();
        // 응답을 프론트가 쓰기 쉬운 형태로 변환
        return deals.stream()
                .limit(20)
                .map(deal -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("title", deal.get("title"));
                    result.put("assets", deal.get("assets"));

                    Map dealInfo = (Map) deal.get("deal");
                    if (dealInfo != null) {
                        Map price = (Map) dealInfo.get("price");
                        Map regular = (Map) dealInfo.get("regular");
                        result.put("currentPrice", price != null ? price.get("amount") : 0);
                        result.put("regularPrice", regular != null ? regular.get("amount") : 0);
                        result.put("cut", dealInfo.get("cut"));
                        result.put("shopName", ((Map)dealInfo.get("shop")).get("name"));
                        result.put("url", dealInfo.get("url"));
                    }
                    return result;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}