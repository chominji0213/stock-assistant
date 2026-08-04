package com.example.stockassistant.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Top20 종목 마스터 테이블
@Entity
@Table(name = "STOCK_MASTER")
@Getter
@Setter
@NoArgsConstructor
public class StockMaster {
	@Id
	@Column(name = "stock_code", length = 20)
	private String stockCode;

	@Column(name = "stock_name", nullable = false, unique = true, length = 100)
	private String stockName;

	@Column(name = "market", length = 20)
	private String market;

	@Column(name = "market_cap_rank")
	private Integer marketCapRank;

	// DART 고유번호 (8자리). 공시 조회(list.json)는 종목코드가 아니라 이 값을 요구함
	@Column(name = "corp_code", length = 8)
	private String corpCode;

	// 법인등록번호 (13자리). 금융위원회 기업재무정보 API(getSummFinaStat_V2)는 corp_code가 아니라 이 값을 요구함
	@Column(name = "crno", length = 13)
	private String crno;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
