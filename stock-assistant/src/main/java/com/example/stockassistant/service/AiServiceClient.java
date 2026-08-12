package com.example.stockassistant.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiServiceClient {

	// FastAPI(AI 서비스)는 Vision/RAG/점수계산/리포트 생성 때문에 느릴 수 있어서,
	// DART/KRX 호출용 RestClient(5초 타임아웃)를 공유해서 쓰지 않고,
	// Spring Bean 주입/Qualifier에 의존하지 않도록 여기서 직접 긴 타임아웃 RestClient를 만들어 씀.
	private final RestClient restClient;

	public AiServiceClient() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(180));
		this.restClient = RestClient.builder().requestFactory(factory).build();
	}

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

	// 계좌분석: 업로드받은 이미지를 FastAPI /account-diagnosis로 전달
	// (인식 -> 신뢰도체크 -> Top20검증 -> RAG검색 -> 점수계산 -> 리포트까지 한번에 실행됨)
	@SuppressWarnings("unchecked")
	public Map<String, Object> diagnoseAccount(MultipartFile image) {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		try {
			builder.part("image", image.getResource())
					.filename(image.getOriginalFilename());
		} catch (Exception e) {
			throw new RuntimeException("이미지 전달 준비 실패", e);
		}

		return restClient.post()
				.uri(fastApiBaseUrl + "/account-diagnosis")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(builder.build())
				.retrieve()
				.body(Map.class);
	}
}