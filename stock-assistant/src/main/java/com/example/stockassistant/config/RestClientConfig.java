package com.example.stockassistant.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// DART/KRX/FastAPI 같은 외부 API 호출에 쓰는 RestClient 빈 등록
// RestTemplate은 스프링 공식문서에서 유지보수 모드로 안내하고 있어서,
// 신규 프로젝트 기준으로 권장되는 RestClient로 씀
//
// requestFactory를 SimpleClientHttpRequestFactory(HttpURLConnection 기반)로 명시한 이유:
// 기본값(JDK HttpClient 기반)에서 POST body가 제대로 안 실려가는 문제가 있어서
// 더 오래되고 검증된 방식으로 바꿔서 테스트하기 위함
//
// 타임아웃을 넣어둔 이유: 외부 API(DART/KRX 등)가 응답을 안 주고 계속 붙잡고 있으면
// 우리 서버 스레드도 같이 멈춰버리니까, 일정 시간 지나면 포기하고 에러를 내도록 함
@Configuration
public class RestClientConfig {

	@Bean
	public RestClient restClient() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(3)); // 연결 자체가 3초 넘게 안 되면 포기
		factory.setReadTimeout(Duration.ofSeconds(5));    // 응답을 5초 넘게 기다리면 포기

		return RestClient.builder()
				.requestFactory(factory)
				.build();
	}
}
