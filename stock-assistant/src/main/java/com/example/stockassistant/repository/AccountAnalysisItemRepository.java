package com.example.stockassistant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stockassistant.domain.AccountAnalysisItem;

public interface AccountAnalysisItemRepository extends JpaRepository<AccountAnalysisItem, Long> {

	/**
	 * 특정 계좌분석 이력에 속한 종목 상세 목록 조회
	 * @param analysisNo
	 * @return
	 */
	List<AccountAnalysisItem> findByAccountAnalysis_AnalysisNo(Long analysisNo);
}
