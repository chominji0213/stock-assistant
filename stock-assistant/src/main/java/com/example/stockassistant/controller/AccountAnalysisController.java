package com.example.stockassistant.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.stockassistant.domain.AccountAnalysis;
import com.example.stockassistant.domain.AccountAnalysisItem;
import com.example.stockassistant.domain.Member;
import com.example.stockassistant.repository.AccountAnalysisRepository;
import com.example.stockassistant.repository.MemberRepository;
import com.example.stockassistant.repository.StockMasterRepository;
import com.example.stockassistant.service.AiServiceClient;

import lombok.RequiredArgsConstructor;

// 계좌분석 화면. /account-analysis는 SecurityConfig에서 이미 로그인 필수(anyRequest().authenticated())라,
// 이 컨트롤러에 들어오는 요청은 항상 로그인한 회원의 요청 -> 분석 결과를 ACCOUNT_ANALYSIS/ACCOUNT_ANALYSIS_ITEM에 저장한다.
@Controller
@RequiredArgsConstructor
public class AccountAnalysisController {

	private final AiServiceClient aiServiceClient;
	private final MemberRepository memberRepository;
	private final AccountAnalysisRepository accountAnalysisRepository;
	private final StockMasterRepository stockMasterRepository;

	@GetMapping("/account-analysis")
	public String accountAnalysisPage() {
		return "account-analysis/index";
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/account-analysis")
	public String analyze(@RequestParam("image") MultipartFile image, Model model) {
		if (image.isEmpty()) {
			model.addAttribute("errorMessage", "계좌 캡처 이미지를 선택해주세요.");
			return "account-analysis/index";
		}
		try {
			Map<String, Object> result = aiServiceClient.diagnoseAccount(image);

			boolean isReliable = Boolean.TRUE.equals(result.get("is_reliable"));
			model.addAttribute("isReliable", isReliable);

			if (!isReliable) {
				model.addAttribute("errorMessage", result.get("error"));
				return "account-analysis/index";
			}

			List<Map<String, Object>> holdings = (List<Map<String, Object>>) result.get("holdings");
			Integer diagnosisScore = (Integer) result.get("diagnosis_score");
			String finalReport = (String) result.get("final_report");

			model.addAttribute("confidenceScore", result.get("confidence_score"));
			model.addAttribute("holdings", holdings);
			model.addAttribute("unmatchedStocks", (List<String>) result.get("unmatched_stocks"));
			model.addAttribute("diagnosisScore", diagnosisScore);
			model.addAttribute("scoreBreakdown", (Map<String, Object>) result.get("score_breakdown"));
			model.addAttribute("finalReport", finalReport);

			saveAnalysis(holdings, diagnosisScore, finalReport);
		} catch (Exception e) {
			e.printStackTrace(); // 콘솔에 원인 스택트레이스가 남도록 (디버깅용)
			model.addAttribute("errorMessage", "AI 서비스 호출 실패: " + e.getMessage());
		}
		return "account-analysis/index";
	}

	// 로그인한 회원의 계좌분석 결과를 DB에 저장 (헤더 1건 + 인식된 종목별 상세 여러 건)
	private void saveAnalysis(List<Map<String, Object>> holdings, Integer diagnosisScore, String finalReport) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String loginId = authentication.getName();

		Member member = memberRepository.findByLoginId(loginId).orElse(null);
		if (member == null) {
			return; // 못 찾으면 저장만 스킵 (화면 표시는 이미 끝났으므로 사용자 경험엔 영향 없음)
		}

		AccountAnalysis analysis = new AccountAnalysis();
		analysis.setMember(member);
		analysis.setDiagnosisScore(diagnosisScore);
		analysis.setReportContent(finalReport);

		if (holdings != null) {
			for (Map<String, Object> h : holdings) {
				AccountAnalysisItem item = new AccountAnalysisItem();
				item.setAccountAnalysis(analysis);
				item.setRecognizedStockName((String) h.get("stock_name"));

				Object quantity = h.get("quantity");
				if (quantity instanceof Number) {
					item.setQuantity(((Number) quantity).longValue());
				}

				Object stockCode = h.get("stock_code");
				if (stockCode instanceof String) {
					stockMasterRepository.findById((String) stockCode).ifPresent(item::setStockMaster);
				}

				Object confidence = h.get("confidence");
				if (confidence instanceof Number) {
					// DB 컬럼이 precision=4, scale=3 (0.000~1.000)이라 소수 3자리로 맞춰줌
					item.setConfidence(BigDecimal.valueOf(((Number) confidence).doubleValue())
							.setScale(3, RoundingMode.HALF_UP));
				}

				analysis.getItems().add(item);
			}
		}

		accountAnalysisRepository.save(analysis); // items는 cascade=ALL이라 같이 저장됨
	}
}
