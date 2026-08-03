package com.example.stockassistant.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

// KRX(한국거래소) API 호출 서비스
// stk_bydd_trd는 KRX 승인 전이라 미검증 상태. 승인나면 실제로 호출해서 확인 필요.
@Service
@RequiredArgsConstructor
public class KrxApiService {

	private final RestClient restClient;

	@Value("${krx.auth.key}")
	private String krxAuthKey;

	private static final String BASE_URL = "http://data-dbg.krx.co.kr/svc/apis/sto/stk_bydd_trd";

	// 기준일(basDd) 코스피 전종목 시세 전체 조회 (시가총액 MKTCAP 포함)
	// KRX는 인증키를 헤더(AUTH_KEY)로 보내야 함
	// 같은 날짜는 같은 데이터라 하루 단위로 stockPrice 캐시에 저장
	@SuppressWarnings("unchecked")
	@Cacheable(value = "stockPrice", key = "#basDd")
	public List<Map<String, Object>> getDailyTradeAll(String basDd) {
		String url = BASE_URL + "?basDd=" + basDd;

		Map<String, Object> body = restClient.get()
				.uri(url)
				.header("AUTH_KEY", krxAuthKey)
				.retrieve()
				.body(Map.class);

		if (body == null || body.get("OutBlock_1") == null) {
			return List.of();
		}
		return (List<Map<String, Object>>) body.get("OutBlock_1");
	}

	// 전종목 리스트에서 원하는 종목코드 하나만 필터링
	public Map<String, Object> getStockPrice(String stockCode, String basDd) {
		return getDailyTradeAll(basDd).stream()
				.filter(row -> stockCode.equals(row.get("ISU_CD")))
				.findFirst()
				.orElse(null);
	}
}
