package com.example.stockassistant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.stockassistant.domain.StockMaster;
import com.example.stockassistant.repository.StockMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {
	private final StockMasterRepository stockMasterRepository;
	
	/**
	 * Top20 마스터 종목 전체 조회
	 */
	@Override
	public List<StockMaster> getTop20() {
		return stockMasterRepository.findAll();
	}
}
