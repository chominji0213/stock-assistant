package com.example.stockassistant.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 계좌분석에서 인식된 개별 종목 (상세) - 예: 삼성전자, SK하이닉스 등 1건당 여러 행
@Entity
@Table(name = "ACCOUNT_ANALYSIS_ITEM")
@Getter
@Setter
@NoArgsConstructor
public class AccountAnalysisItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_no")
	private Long itemNo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "analysis_no", nullable = false)
	private AccountAnalysis accountAnalysis;

	// Top20 매칭에 성공한 경우에만 채워짐
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stock_code")
	private StockMaster stockMaster;

	// Gemini Vision이 인식한 원본 종목명 텍스트 (Top20 매칭 실패해도 기록됨)
	@Column(name = "recognized_stock_name", nullable = false, length = 100)
	private String recognizedStockName;

	// 신뢰도 0.000 ~ 1.000
	@Column(name = "confidence", precision = 4, scale = 3)
	private BigDecimal confidence;

	// 보유 수량
	@Column(name = "quantity")
	private Long quantity;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
