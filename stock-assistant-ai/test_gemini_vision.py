# Gemini Vision 신뢰도 프롬프트 테스트용 스크립트.
# 사용법:
#   1) 계좌 캡처 이미지(스크린샷) 파일을 이 프로젝트 폴더에 samples/account_sample.png 로 넣기
#      (실제 계좌 캡처가 없으면 임의의 표/숫자가 담긴 이미지로 우선 테스트해도 됨)
#   2) venv 활성화 후: python test_gemini_vision.py [이미지경로]
#      이미지경로를 안 주면 samples/account_sample.png 를 기본값으로 사용

import base64
import os
import sys

from dotenv import load_dotenv
from langchain.chat_models import init_chat_model
from langchain_core.messages import HumanMessage

from models.vision_schema import VisionExtractionResult
from services.vision_prompt import VISION_SYSTEM_PROMPT

load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise SystemExit(".env에 GEMINI_API_KEY가 없습니다. 먼저 설정해주세요.")

image_path = sys.argv[1] if len(sys.argv) > 1 else "samples/account_sample.png"
if not os.path.exists(image_path):
    raise SystemExit(
        f"이미지 파일을 찾을 수 없습니다: {image_path}\n"
        "테스트할 계좌 캡처 이미지(또는 표가 담긴 임의의 이미지)를 준비해서 경로를 인자로 넘겨주세요.\n"
        "예: python test_gemini_vision.py samples/account_sample.png"
    )

with open(image_path, "rb") as f:
    image_b64 = base64.b64encode(f.read()).decode("utf-8")

ext = os.path.splitext(image_path)[1].lstrip(".").lower() or "png"
mime = "jpeg" if ext in ("jpg", "jpeg") else ext

llm = init_chat_model("gemini-3.1-flash-lite", model_provider="google_genai", api_key=api_key)
structured_llm = llm.with_structured_output(VisionExtractionResult)

message = HumanMessage(
    content=[
        {"type": "text", "text": VISION_SYSTEM_PROMPT},
        {"type": "image_url", "image_url": f"data:image/{mime};base64,{image_b64}"},
    ]
)

result: VisionExtractionResult = structured_llm.invoke([message])

print("=== 인식된 보유 종목 ===")
for h in result.holdings:
    print(f"- {h.stock_name} ({h.stock_code}) 수량:{h.quantity} 평가금액:{h.eval_amount} 수익률:{h.profit_loss_rate}")

print("\n=== 신뢰도 ===")
print(f"confidence_score: {result.confidence_score}")
print(f"is_reliable: {result.is_reliable}")
print(f"confidence_reason: {result.confidence_reason}")
print(f"issues: {result.issues}")
