# 계좌 캡처 이미지를 Gemini Vision으로 인식할 때 사용하는 출력 스키마.
# "인식 노드"가 이 형태로 결과를 받고, "신뢰도체크 분기 노드"가 confidence_score/is_reliable을 보고
# 다음 단계(Top20 검증 → RAG검색 → 점수계산)로 보낼지, 사용자에게 재업로드를 요청할지 결정한다.

from pydantic import BaseModel, Field


class HoldingItem(BaseModel):
    """계좌 캡처 이미지에서 인식된 보유 종목 한 줄"""

    stock_name: str = Field(description="화면에 표시된 종목명 (예: 삼성전자)")
    stock_code: str | None = Field(default=None, description="종목코드를 읽을 수 있으면 채움, 없으면 null")
    quantity: float | None = Field(default=None, description="보유 수량")
    avg_price: float | None = Field(default=None, description="평균 매입가")
    current_price: float | None = Field(default=None, description="현재가")
    eval_amount: float | None = Field(default=None, description="평가금액")
    profit_loss_rate: float | None = Field(default=None, description="수익률(%), 예: -3.2")
    confidence: float = Field(ge=0.0, le=1.0, description="이 종목 한 줄에 대한 인식 신뢰도 (0.0~1.0)")


class VisionExtractionResult(BaseModel):
    """Gemini Vision 인식 노드의 최종 출력"""

    holdings: list[HoldingItem] = Field(description="인식된 보유 종목 목록. 하나도 못 읽었으면 빈 리스트")
    confidence_score: int = Field(ge=0, le=100, description="인식 신뢰도 점수 0~100")
    confidence_reason: str = Field(description="왜 이 점수를 줬는지 한두 문장 설명")
    issues: list[str] = Field(default_factory=list, description="판독이 애매했던 부분 목록 (없으면 빈 리스트)")
    is_reliable: bool = Field(description="confidence_score가 임계값(70) 이상이면 true")


# 신뢰도체크 분기 노드에서 쓰는 임계값
CONFIDENCE_THRESHOLD = 70
