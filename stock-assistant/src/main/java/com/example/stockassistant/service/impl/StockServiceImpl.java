package com.example.stockassistant.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.stockassistant.domain.StockMaster;
import com.example.stockassistant.repository.StockMasterRepository;
import com.example.stockassistant.service.DartApiService;
import com.example.stockassistant.service.FinancialApiService;
import com.example.stockassistant.service.KrxApiService;
import com.example.stockassistant.service.StockService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {
	private final StockMasterRepository stockMasterRepository;
    private final DartApiService dartApiService;
    private final KrxApiService krxApiService;
    private final FinancialApiService financialApiService;
	
	/**
	 * Top20 마스터 종목 전체 조회
	 */
	@Override
	public List<StockMaster> getTop20() {
		return stockMasterRepository.findAll();
	}
	
	/**
	 * 특정 종목의 기준일 시세 조회
	 */
	@Override
	public Object getPrice(String stockCode, String basDd) {
		return krxApiService.getStockPrice(stockCode, basDd);
	}
	
	/**
	 * 특정 종목의 최근 공시 목록 조회
	 */
	@Override
	public Object getDisclosure(String stockCode) {
		StockMaster stock = stockMasterRepository.findById(stockCode)
				.orElseThrow(() -> new IllegalArgumentException("Top20에 없는 종목코드: " + stockCode));
		if(stock.getCorpCode() == null) {
			return Map.of("error", "corp_code가 없습니다. corp-code-sync 먼저 실행 필요합니다.");
		}
		
		return dartApiService.getDisclosureList(stock.getCorpCode());
	}

	/**
	 * 특정 종목의 요약재무제표 조회
	 */
	@Override
	public Object getFinancialInfo(String stockCode, String bizYear) {
	    StockMaster stock = stockMasterRepository.findById(stockCode)
	            .orElseThrow(() -> new IllegalArgumentException("Top20에 없는 종목코드: " + stockCode));
	    if (stock.getCrno() == null) {
	        return Map.of("error", "crno가 없습니다. crno-sync 먼저 실행 필요합니다.");
	    }

	    return financialApiService.getSummaryFinancialStatement(stock.getCrno(), bizYear);
	}
}
