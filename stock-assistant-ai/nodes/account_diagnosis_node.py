import base64
import io
import os
import chromadb
import requests

from dotenv import load_dotenv
from langchain.chat_models import init_chat_model
from langchain_core.messages import HumanMessage
from PIL import Image

from models.graph_state import AccountDiagnosisState
from models.vision_schema import VisionExtractionResult
from services.vision_prompt import VISION_SYSTEM_PROMPT

from chromadb.utils import embedding_functions


load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
llm = init_chat_model("gemini-3.1-flash-lite", model_provider="google_genai", api_key=api_key)
structured_llm = llm.with_structured_output(VisionExtractionResult) #구조화된 LLM 답변

# ChromaDB 컬렉션(DB) 연결
chroma_client = chromadb.PersistentClient(path="./chroma_db")
ko_ef = embedding_functions.SentenceTransformerEmbeddingFunction(model_name="jhgan/ko-sroberta-multitask")
rag_collection = chroma_client.get_collection(name="economic_terms", embedding_function=ko_ef)

# 진단 리포트에서 항상 설명해줄 고정 검색어 (벡터DB에 실제있는 것들만)
# 수익률/리스크 관리는 800선에 대응하는 용어가 없어서 RAG 없이 리포트에서 직접 설명함
DIAGNOSIS_TERMS = [ "PER", "PBR", "HHI"]
SPRING_BOOT_BASE_URL = "http://localhost:8080"

#이미지를 LLM에 인식시키는 노드
def recognize_node(state: AccountDiagnosisState) -> dict:
    print('1. 인식노드 실행')

    img = Image.open(io.BytesIO(state['images_byte'])) #bytes -> PIL 이미지
    image_b64 = image_to_base64(img, fmt='PNG')   #PIL 이미지 -> base64

    message = build_vision_message(VISION_SYSTEM_PROMPT, image_b64, 'image/png')

    result = VisionExtractionResult = structured_llm.invoke([message])

    return {'vision_result': result}

#라우팅 함수(분기) 노드
def reliability_check_node(state: AccountDiagnosisState) -> dict:
    print('2. 신뢰도체크 분기 노드 실행')

    if state['vision_result'].is_reliable:
        result = 'proceed'
    else:
        result = 'retry'

    return {'branch': result}

#TOP20 체크 노드
def top20_check_node(state: AccountDiagnosisState) -> dict:
    print('3. Top20 검증 노드 실행')

    top20_names = fetch_top20_stock_names()
    unmatched_stocks = []

    for holding in state['vision_result'].holdings:
        if holding.stock_name not in top20_names:
            unmatched_stocks.append(holding.stock_name)

    return {'unmatched_stocks': unmatched_stocks}

#RAG 노드
def rag_search_node(state: AccountDiagnosisState) -> dict:
    print('4. RAG검색 노드 실행')

    rag_context = []
    all_terms = rag_collection.get()  # 전체 다 가져오기

    for term in DIAGNOSIS_TERMS:
        #result = rag_collection.query(query_texts=[term + '이 뭐야?'], n_results=1)    -> 유사도검색

        #for doc, meta in zip(result["documents"][0], result["metadatas"][0]):
        for doc, meta in zip(all_terms["documents"], all_terms["metadatas"]):
            if term in meta["term"]:   # term 문자열이 포함되어 있는지로 매칭
                rag_context.append({'doc': doc, 'meta': meta})
                break   # 용어당 1개만

    return {'rag_context': rag_context}

####헬퍼 함수들####
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

#spring에서 MasterStock 테이블의 데이터 받아옴
def fetch_top20_stock_names() -> set[str]:
    response = requests.get(f"{SPRING_BOOT_BASE_URL}/api/stocks/top20")
    response.raise_for_status()
    stocks = response.json()

    return {stock["stockName"] for stock in stocks}