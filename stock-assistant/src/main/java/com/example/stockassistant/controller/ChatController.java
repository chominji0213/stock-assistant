package com.example.stockassistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.stockassistant.service.AiServiceClient;

import lombok.RequiredArgsConstructor;

// 일반질의 화면. 데모 단계라 DB 저장 없이 FastAPI 호출 결과만 그대로 보여줌.
@Controller
@RequiredArgsConstructor
public class ChatController {

	private final AiServiceClient aiServiceClient;

	@GetMapping("/general-chat")
	public String chatPage() {
		return "chat/index";
	}

	@PostMapping("/general-chat")
	public String ask(@RequestParam(required = false) String question, @RequestParam(value = "images", required = false) MultipartFile[] images, Model model) {
		// 이미지만 첨부하고 질문을 안 쓴 경우, 기본 프롬프트로 대체 (빈 질문으로 넘어가면 에러가 나서)
		if (question == null || question.isBlank()) {
			question = "이 이미지를 요약해줘";
		}
		model.addAttribute("question", question);
		try {
			model.addAttribute("answer", aiServiceClient.qa(question, images));
		} catch (Exception e) {
			e.printStackTrace(); // 콘솔에 원인 스택트레이스가 남도록 (디버깅용)
			model.addAttribute("errorMessage", "AI 서비스 호출 실패: " + e.getMessage());
		}
		return "chat/index";
	}
}
