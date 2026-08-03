import os
from dotenv import load_dotenv
from langchain.chat_models import init_chat_model

load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise SystemExit(".env에 GEMINI_API_KEY가 없습니다. 먼저 설정해주세요.")

# 모델 초기화
llm = init_chat_model("gemini-3.1-flash-lite", model_provider="google_genai", api_key=api_key)

response = llm.invoke("안녕, 잘 작동하는지 확인 중이야. 한 문장으로 답해줘.")
print("응답:", response.content)
