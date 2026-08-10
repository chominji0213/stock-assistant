package com.example.stockassistant.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiServiceClient {

	private final RestClient restClient;

	@Value("${fastapi.base-url}")
	private String fastApiBaseUrl;

	public Map<String, Object> echo(String message) {
		String jsonBody = "{\"message\": \"" + message + "\"}";
		
		return restClient.post()
				.uri(fastApiBaseUrl + "/echo")
				.contentType(MediaType.APPLICATION_JSON)
				.body(jsonBody)
				.retrieve()
				.body(Map.class);
	}

	// 일반질의: FastAPI /qa 호출 (Tool Calling — 시세/공시/재무정보/용어사전)
	// 뉴스 캡처 같은 이미지가 여러 장 같이 첨부될 수 있어서 images는 없어도/여러 개여도 됨
	@SuppressWarnings("unchecked")
	public String qa(String question, MultipartFile[] images) {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("question", question);
		if (images != null) {
			for (MultipartFile image : images) {
				if (image != null && !image.isEmpty()) {
					try {
						builder.part("images", image.getResource())
								.filename(image.getOriginalFilename());
					} catch (Exception e) {
						throw new RuntimeException("이미지 전달 준비 실패", e);
					}
				}
			}
		}

		Map<String, Object> response = restClient.post()
				.uri(fastApiBaseUrl + "/qa")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(builder.build())
				.retrieve()
				.body(Map.class);
		return response == null ? null : (String) response.get("answer");
	}

	// 계좌분석: 업로드받은 이미지를 그대로 FastAPI /vision/analyze로 멀티파트 전달
	@SuppressWarnings("unchecked")
	public Map<String, Object> analyzeAccountImage(MultipartFile image) {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		try {
			builder.part("image", image.getResource())
					.filename(image.getOriginalFilename());
		} catch (Exception e) {
			throw new RuntimeException("이미지 전달 준비 실패", e);
		}

		return restClient.post()
				.uri(fastApiBaseUrl + "/vision/analyze")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(builder.build())
				.retrieve()
				.body(Map.class);
	}
}