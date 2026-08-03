package com.example.stockassistant.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.stockassistant.service.AiServiceClient;

import lombok.RequiredArgsConstructor;

// 계좌분석 화면. 데모 단계라 ACCOUNT_ANALYSIS/ACCOUNT_ANALYSIS_ITEM 저장 없이 FastAPI 호출 결과만 그대로 보여줌.
// (DB 적재는 "계좌 진단 점수 계산 로직 설계+구현" 태스크에서 붙일 예정)
@Controller
@RequiredArgsConstructor
public class AccountAnalysisController {

	private final AiServiceClient aiServiceClient;

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
			Map<String, Object> result = aiServiceClient.analyzeAccountImage(image);
			model.addAttribute("confidenceScore", result.get("confidence_score"));
			model.addAttribute("isReliable", result.get("is_reliable"));
			model.addAttribute("confidenceReason", result.get("confidence_reason"));
			model.addAttribute("issues", (List<String>) result.get("issues"));
			model.addAttribute("holdings", (List<Map<String, Object>>) result.get("holdings"));
		} catch (Exception e) {
			model.addAttribute("errorMessage", "AI 서비스 호출 실패: " + e.getMessage());
		}
		return "account-analysis/index";
	}
}
