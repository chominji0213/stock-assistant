package com.example.stockassistant.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.stockassistant.domain.StockMaster;
import com.example.stockassistant.repository.StockMasterRepository;
import com.example.stockassistant.service.AiServiceClient;
import com.example.stockassistant.service.DartApiService;
import com.example.stockassistant.service.KrxApiService;

import lombok.RequiredArgsConstructor;

// DART/KRX 연동이 실제로 되는지, 캐싱이 잘 붙는지 눈으로 확인하기 위한 임시 테스트용 컨트롤러
// (개발 중에만 쓰고, 나중에 지워도 되는 용도)
@RestController
@RequiredArgsConstructor
public class ApiTestController {

	private final DartApiService dartApiService;
	private final KrxApiService krxApiService;
	private final StockMasterRepository stockMasterRepository;
	private final AiServiceClient aiServiceClient;

	// 딱 한 번 실행: DART 고유번호 매핑을 받아서 STOCK_MASTER의 corp_code를 채워넣음
	@GetMapping("/api-test/dart/corp-code-sync")
	public String syncCorpCode() {
		Map<String, String> mapping = dartApiService.fetchCorpCodeMapping();

		int updated = 0;
		for (StockMaster stock : stockMasterRepository.findAll()) {
			String corpCode = mapping.get(stock.getStockCode());
			if (corpCode != null) {
				stock.setCorpCode(corpCode);
				stockMasterRepository.save(stock);
				updated++;
			}
		}
		return "corp_code 업데이트 완료: " + updated + "건";
	}

	// 특정 종목의 최근 공시 목록 조회 (corp_code는 위 sync를 먼저 실행해야 채워져 있음)
	@GetMapping("/api-test/dart/disclosure/{stockCode}")
	public Object getDisclosure(@PathVariable String stockCode) {
		StockMaster stock = stockMasterRepository.findById(stockCode)
				.orElseThrow(() -> new IllegalArgumentException("Top20에 없는 종목코드: " + stockCode));
		if (stock.getCorpCode() == null) {
			return "corp_code가 없습니다. /api-test/dart/corp-code-sync 먼저 호출하세요.";
		}
		return dartApiService.getDisclosureList(stock.getCorpCode());
	}

	// 특정 종목의 특정 기준일 시세 조회 (예: basDd=20260731)
	@GetMapping("/api-test/krx/price/{stockCode}")
	public Object getPrice(@PathVariable String stockCode, @RequestParam String basDd) {
		return krxApiService.getStockPrice(stockCode, basDd);
	}
	
	@GetMapping("/api-test/ai/echo")
	public Object testEcho(@RequestParam String message) {
		return aiServiceClient.echo(message);
	}
}
