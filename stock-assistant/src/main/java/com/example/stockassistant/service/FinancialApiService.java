package com.example.stockassistant.service;

import java.util.List;
import java.util.Map;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

// 공공데이터포털 - 금융위원회 기업 재무정보 API 호출 서비스
@Service
@RequiredArgsConstructor
public class FinancialApiService {

    private final RestClient restClient;

    @Value("${finance.api.key}")
    private String financeApiKey;

    private static final String BASE_URL =
            "https://apis.data.go.kr/1160100/service/GetFinaStatInfoService_V2/getSummFinaStat_V2";

    // 요약재무제표 조회 (crno + bizYear 기준). 연도별로 캐시.
    @SuppressWarnings("unchecked")
    @Cacheable(value = "financialInfo", key = "#crno + '_' + #bizYear")
    public Map<String, Object> getSummaryFinancialStatement(String crno, String bizYear) {
        String url = BASE_URL
                + "?serviceKey=" + financeApiKey
                + "&crno=" + crno
                + "&bizYear=" + bizYear
                + "&resultType=json";

        Map<String, Object> body = restClient.get().uri(URI.create(url)).retrieve().body(Map.class);
        return body;
    }
}