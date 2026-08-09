package com.example.stockassistant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.stockassistant.domain.StockMaster;
import com.example.stockassistant.service.StockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StockController {

	private final StockService stockService;
	
	/**
	 * Top20 마스터 종목 전체 조회
	 * @return
	 */
	@GetMapping("/api/stocks/top20")
	public List<StockMaster> getTop20() {
		return stockService.getTop20();
	}
	
	/**
	 * 특정 종목의 최근 공시 목록 조회
	 * @param stockCode
	 * @return
	 */
	@GetMapping("/api/stocks/{stockCode}/disclosure")
	public Object getDisclosure(@PathVariable String stockCode) {
	    return stockService.getDisclosure(stockCode);
	}
	
	/**
	 * 특정 종목의 기준일(basDd) 시세 조회
	 * @param stockCode
	 * @param basDd
	 * @return
	 */
	@GetMapping("/api/stocks/{stockCode}/price")
	public Object getPrice(@PathVariable String stockCode, @RequestParam String basDd) {
	    return stockService.getPrice(stockCode, basDd);
	}
	
	/**
	 * 특정 종목의 요약재무제표 조회
	 * @param stockCode
	 * @param bizYear
	 * @return
	 */
	@GetMapping("/api/stocks/{stockCode}/financial")
	public Object getFinancialInfo(@PathVariable String stockCode, @RequestParam String bizYear) {
	    return stockService.getFinancialInfo(stockCode, bizYear);
	}
}