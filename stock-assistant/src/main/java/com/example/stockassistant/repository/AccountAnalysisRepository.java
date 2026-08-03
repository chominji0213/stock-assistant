package com.example.stockassistant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.stockassistant.domain.AccountAnalysis;

public interface AccountAnalysisRepository extends JpaRepository<AccountAnalysis, Long> {

	/**
	 * 회원의 계좌분석 이력을 최신순으로 조회
	 * @param memberNo
	 * @return
	 */
	List<AccountAnalysis> findByMember_MemberNoOrderByAnalyzedAtDesc(Long memberNo);
}
