from typing import TypedDict, Optional, Literal
from models.vision_schema import VisionExtractionResult

class AccountDiagnosisState(TypedDict):
    """계좌 진단 흐름: 인식 -> 신뢰도체크 -> 분기 -> Top20검증 -> RAG검색 -> 점수계산/답변생성"""
    
    #입력
    images_byte: bytes

    #인식노드(vision_schema에서 구조화된 LLM에서 받은값 담는 곳) 
    vision_result: Optional[VisionExtractionResult] #Optional: 값이 들어올수도있고, None일수도 있음

    # 신뢰도체크 분기 노드가 정하는 다음 경로
    branch: Optional[Literal["proceed", "retry"]]   #진행 OR 재시도

    # Top20 검증 노드 출력(Top20에 없는 종목명 목록)
    unmatched_stocks: Optional[list[str]]   

    # RAG검색 노드 출력 (용어 설명 등 근거자료)
    rag_context: Optional[list[dict]]

    # 점수계산 노드 출력
    diagnosis_score: Optional[int]          # 0~100
    score_breakdown: Optional[dict]

    # 최종 출력
    final_report: Optional[str]
    error: Optional[str]                    # 실패 시 사용자에게 보여줄 메시지

class QAState(TypedDict):
    """일반질의 흐름: 질문(+이미지) -> Tool Calling(시세/공시/재무정보/용어사전) -> 답변생성"""

    #질문
    question: str

    # 뉴스 캡처 등 첨부 이미지 (선택, 여러 장 가능) - 각 원소: {"base64": ..., "mime": ...}
    images: Optional[list[dict]]

    # LLM이 선택한 도구+파라미터
    tool_calls: Optional[list[dict]]

    # 각 도구 호출 결과
    tool_results: Optional[list[dict]]

    #최종답
    answer: Optional[str]