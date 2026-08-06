import base64
import io
import os

from dotenv import load_dotenv
from langchain.chat_models import init_chat_model
from langchain_core.messages import HumanMessage
from PIL import Image

from models.graph_state import AccountDiagnosisState
from models.vision_schema import VisionExtractionResult
from services.vision_prompt import VISION_SYSTEM_PROMPT

load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
llm = init_chat_model("gemini-3.1-flash-lite", model_provider="google_genai", api_key=api_key)
structured_llm = llm.with_structured_output(VisionExtractionResult) #구조화된 LLM 답변

def recognize_node(state: AccountDiagnosisState) -> dict:
    print('1. 인식노드 실행')

    img = Image.open(io.BytesIO(state['images_byte'])) #bytes -> PIL 이미지
    image_b64 = image_to_base64(img, fmt='PNG')   #PIL 이미지 -> base64

    message = build_vision_message(VISION_SYSTEM_PROMPT, image_b64, 'image/png')

    result = VisionExtractionResul = structured_llm.invoke([message])

    return {'vision_result': result}

def reliability_check_node(state: AccountDiagnosisState) -> dict:
    print('2. 신뢰도체크 분기 노드 실행')

    if state['vision_result'].is_reliable:
        result = 'proceed'
    else:
        result = 'retry'

    return {'branch': result}


#이미지(바이너리 파일)을 문자열로 변환
def image_to_base64(img: Image.Image, fmt="PNG") -> str:
    buf = io.BytesIO()
    img.save(buf, format=fmt)                                 # 이미지 -> 바이트

    return base64.b64encode(buf.getvalue()).decode("utf-8")   # 바이트 -> base64 문자열

#텍스트 + base64 이미지(raw)를 하나의 사용자 메시지로 묶음
def build_vision_message(text: str, image_b64: str, media_type: str = "image/png") -> HumanMessage:

    return HumanMessage(content=[
        {"type": "text", "text": text},
        {"type": "image_url", "image_url": f"data:{media_type};base64,{image_b64}"},
    ])
    