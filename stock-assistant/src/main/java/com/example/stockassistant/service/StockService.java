package com.example.stockassistant.service;

import java.util.List;

import com.example.stockassistant.domain.StockMaster;

public interface StockService {
	/**
	 * Top20 마스터 종목 전체 조회
	 * @return
	 */
	List<StockMaster> getTop20();
}
