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

	// 일반질의: FastAPI /chat 호출
	// 뉴스 캡처 같은 이미지가 같이 첨부될 수 있어서 image는 없어도 됨 (null 허용)
	@SuppressWarnings("unchecked")
	public String chat(String message, MultipartFile image) {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("message", message);
		if (image != null && !image.isEmpty()) {
			try {
				builder.part("image", image.getResource())
						.filename(image.getOriginalFilename());
			} catch (Exception e) {
				throw new RuntimeException("이미지 전달 준비 실패", e);
			}
		}

		Map<String, Object> response = restClient.post()
				.uri(fastApiBaseUrl + "/chat")
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