package com.example.stockassistant.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 계좌분석 이력 (헤더)
@Entity
@Table(name = "ACCOUNT_ANALYSIS")
@Getter
@Setter
@NoArgsConstructor
public class AccountAnalysis {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "analysis_no")
	private Long analysisNo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_no", nullable = false)
	private Member member;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "diagnosis_score")
	private Integer diagnosisScore;

	// Gemini LLM이 생성한 리포트 본문
	@Lob
	@Column(name = "report_content")
	private String reportContent;

	@Column(name = "analyzed_at", nullable = false)
	private LocalDateTime analyzedAt = LocalDateTime.now();

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	// 계좌분석 1건에 인식된 종목이 여러 개일 수 있어 상세 테이블과 1:N
	@OneToMany(mappedBy = "accountAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AccountAnalysisItem> items = new ArrayList<>();
}
