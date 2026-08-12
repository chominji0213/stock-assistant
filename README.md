# 주식 분석 비서 (AI 주식 어시스턴트)

멀티모달 · RAG 기반 AI 주식 어시스턴트. 계좌 캡처 이미지나 질문을 입력하면 AI가 DART · KRX 최신 데이터와 연결해 분석해주는 서비스입니다.

## 구성 (모노레포)

- `stock-assistant/` — 메인 웹 서비스 (Java, Spring Boot, Thymeleaf, Spring Security, Oracle, Redis)
- `stock-assistant-ai/` — AI 처리 전용 서비스 (Python, FastAPI, LangGraph, Gemini, ChromaDB)

두 서비스는 REST API로 통신합니다 (`stock-assistant` → `stock-assistant-ai`).

## 실행 전 준비

### stock-assistant (Spring Boot)

1. `src/main/resources/application.properties.example`를 복사해 `application.properties`로 만들고, DB 계정 · DART API 키 · KRX 인증키를 채워 넣습니다.
2. Oracle DB와 Redis가 로컬에서 실행 중이어야 합니다.

### stock-assistant-ai (FastAPI)

1. `.env.example`을 복사해 `.env`로 만들고, `GEMINI_API_KEY`를 채워 넣습니다.
2. `pip install -r requirements.txt`

## 주요 기능

- 일반질의: 텍스트 · 이미지(여러 장 첨부 가능) 기반 질의응답 (RAG, Tool Calling). 이미지만 첨부한 경우 기본 질문("요약해줘")으로 처리.
- 계좌분석: 계좌 캡처 이미지 인식 → 신뢰도 판단 → Top20 우량주 매칭 → 0~100점 진단 → 리포트에 용어 설명 출처·기준일 표기
- 로그인한 사용자는 계좌분석 결과(진단점수, 종목별 인식 신뢰도, 리포트)가 DB에 저장됩니다.
