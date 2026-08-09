package com.example.stockassistant.service;

import java.util.List;

import com.example.stockassistant.domain.StockMaster;

public interface StockService {
	/**
	 * Top20 마스터 종목 전체 조회
	 * @return
	 */
	List<StockMaster> getTop20();
	
	/**
     * 특정 종목의 기준일 시세 조회
     * @param stockCode 종목코드
     * @param basDd 기준일(YYYYMMDD)
     * @return KRX 시세 정보
     */
	Object getPrice(String stockCode, String basDd);
	
	/**
     * 특정 종목의 최근 공시 목록 조회
     * @param stockCode 종목코드
     * @return DART 공시 목록
     */
    Object getDisclosure(String stockCode);
    
    /**
     * 특정 종목의 요약재무제표 조회
     * @param stockCode 종목코드
     * @param bizYear 사업연도
     * @return 요약재무제표
     */
    Object getFinancialInfo(String stockCode, String bizYear);
}
