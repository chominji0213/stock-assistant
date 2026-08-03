package com.example.stockassistant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stockassistant.domain.StockMaster;

public interface StockMasterRepository extends JpaRepository<StockMaster, String> {

	/**
	 * 종목명으로 Top20 마스터 조회 (인식된 종목명 매칭용)
	 * @param stockName
	 * @return
	 */
	Optional<StockMaster> findByStockName(String stockName);

	/**
	 * Top20 종목인지 여부 확인
	 * @param stockName
	 * @return
	 */
	boolean existsByStockName(String stockName);
}
