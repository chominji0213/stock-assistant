import base64
import os
from typing import Optional

from dotenv import load_dotenv
from fastapi import FastAPI, File, Form, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from langchain.chat_models import init_chat_model
from langchain_core.messages import HumanMessage

from models.vision_schema import VisionExtractionResult
from services.vision_prompt import VISION_SYSTEM_PROMPT

load_dotenv()

app = FastAPI(title="AI 주식 어시스턴트")

# 데모용 프론트(정적 HTML, file://로 열거나 별도 포트)에서 바로 fetch 할 수 있도록 전체 허용.
# 나중에 Spring Boot를 통해서만 호출하게 되면 좁혀도 됨.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

api_key = os.getenv("GEMINI_API_KEY")

# 텍스트 질의응답용 LLM. (아직 RAG/Tool Calling 안 붙인 순수 LLM 호출 - "일반질의" 데모용)
chat_llm = init_chat_model("gemini-3.1-flash-lite", model_provider="google_genai", api_key=api_key)

# 계좌 캡처 이미지 인식용 LLM. VisionExtractionResult 스키마로 구조화된 응답 강제.
vision_llm = chat_llm.with_structured_output(VisionExtractionResult)


class EchoRequest(BaseModel):
    message: str


@app.get("/health")
def health_check():
    """서버가 잘 떠 있는지 확인하는 용도의 기본 라우트"""
    return {"status": "ok"}


@app.post("/echo")
def echo(req: EchoRequest):
    return {"received": req.message}


@app.post("/chat")
async def chat(message: str = Form(...), image: Optional[UploadFile] = File(None)):
    """일반질의. 뉴스 캡처 같은 이미지가 같이 올라오면 그것도 같이 보고 답변한다.
    (아직 RAG/Tool Calling 붙이기 전이라 Gemini한테 그냥 물어보고 답만 받아온다.)"""
    if image is not None:
        image_bytes = await image.read()
        image_b64 = base64.b64encode(image_bytes).decode("utf-8")
        content_type = image.content_type or "image/png"

        message_with_image = HumanMessage(
            content=[
                {"type": "text", "text": message},
                {"type": "image_url", "image_url": f"data:{content_type};base64,{image_b64}"},
            ]
        )
        response = chat_llm.invoke([message_with_image])
    else:
        response = chat_llm.invoke(message)

    return {"answer": response.content}


@app.post("/vision/analyze")
async def analyze_account_image(image: UploadFile = File(...)):
    """계좌 캡처 이미지를 업로드받아 보유종목 + 신뢰도 점수를 반환한다."""
    image_bytes = await image.read()
    image_b64 = base64.b64encode(image_bytes).decode("utf-8")

    content_type = image.content_type or "image/png"

    message = HumanMessage(
        content=[
            {"type": "text", "text": VISION_SYSTEM_PROMPT},
            {"type": "image_url", "image_url": f"data:{content_type};base64,{image_b64}"},
        ]
    )

    result: VisionExtractionResult = vision_llm.invoke([message])
    return result.model_dump()
