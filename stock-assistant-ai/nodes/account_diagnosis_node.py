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

from langchain_core.messages import HumanMessage, SystemMessage
from services.report_prompt import REPORT_SYSTEM_PROMPT


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
        #for doc, meta in zip(result["documents"][0], result["metadatas"][0]):
        for doc, meta in zip(all_terms["documents"], all_terms["metadatas"]):
            if term in meta["term"]:   # term 문자열이 포함되어 있는지로 매칭
                rag_context.append({'doc': doc, 'meta': meta})
                break   # 용어당 1개만

    return {'rag_context': rag_context}

#점수계산 노드
def score_node(state: AccountDiagnosisState) -> dict:
    print('5. 점수 계산 노드')

    holdings = state['vision_result'].holdings
    unmatched = set(state.get('unmatched_stocks') or [])    #TOP20에 없는 종목명 리스트

    if not holdings:    #인식된 종목이 없다면
        return {'diagnosis_score': 0, 'score_breakdown': {}}

    breakdown = {
        '수익률 건전성' : calc_profit_score(holdings),
        '집중도/분산' : calc_concentration_score(holdings),
        'TOP20 우량주 비중' : calc_top20_score(holdings, unmatched),
        '밸류에이션 적정성' : calc_valuation_score_stub(),
        '리스크 관리' : calc_risk_score(holdings)
    }

    total = 0
    for v in breakdown.values():
        total += v

    return {'diagnosis_score' : round(total), 'score_breakdown': breakdown}

#답변 생성 노드(위의 노드의 결과들을 종합해서 LLM에게 보낸다.)
def report_node(state: AccountDiagnosisState) -> dict:
    print('6. 답변 생성 노드')
    context_text = build_report_context(state)

    messages = [
        SystemMessage(content=REPORT_SYSTEM_PROMPT),
        HumanMessage(content=context_text)
    ]

    result = llm.invoke(messages)   #정해진 스키마(structured_llm) 그대로가 아니라 자유로운 문장을 받아야하므로 llm으로 호출

    return {'final_report': extract_report_text(result.content)}

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

#수익률 건전성(평균 수익률 + 손실종목비율)
def calc_profit_score(holdings) -> float:
    #평균 수익률 점수
    rates = []
    for h in holdings:
        if h.profit_loss_rate is not None:  #profit_loss_rate -> 수익률
            rates.append(h.profit_loss_rate)
    if not rates:
        return 10.0 #데이터가 없다면 중간값으로

    total = 0
    for r in rates:
        total += r
    avg_return = total / len(rates)
    avg_score = max(0, min(10, 5+avg_return / 2))

    #손실 종목 비율 점수
    loss_count = 0
    for r in rates:
        if r < 0:
            loss_count += 1
    loss_ratio = loss_count / len(rates)
    loss_score = (1 - loss_ratio) * 10  #손실종목이 하나도없으면 10점, 전부 손실이면 0점

    return avg_score + loss_score

#종목 집중도/분산 — HHI(HHI 공식대로 점수산출)
def calc_concentration_score(holdings) -> float:
    amounts = []
    for h in holdings:
        if h.eval_amount is not None:
            amounts.append(h.eval_amount)

    total = 0
    for a in amounts:
        total += a

    if total == 0:
        return 10.0

    hhi = 0
    for a in amounts:
        weight = a / total
        hhi += weight ** 2
    hhi *= 10000

    return 20 * (1 - hhi / 10000)   #한종목이 몰려있을수록 감점, 여러종목에 고르게 나눠져있을수록 만점

#Top20 우량주 비중
def calc_top20_score(holdings, unmatched: set[str]) -> float:
    total = len(holdings)

    matched = 0
    for h in holdings:
        if h.stock_name not in unmatched:
            matched += 1

    return 20 * (matched / total)   #보유종목중에 TOP 20에 있는 종목이 몇%인지 산출

#밸류에이션 적정성(PER/PBR)
def calc_valuation_score_stub() -> float:
    # TODO: 재무정보 API(DART) 연동 후 PER/PBR 기반으로 교체
    return 10.0

#리스크 관리 (과도한 손실 종목 비율, -20 정도면 심한 손실로 가정)
def calc_risk_score(holdings, threshold: float = -20.0) -> float:
    rates = []
    for h in holdings:
        if h.profit_loss_rate is not None:
            rates.append(h.profit_loss_rate)

    if not rates:   #손실종목비율이 없으면 10점
        return 10.0

    severe_count = 0
    for r in rates:
        if r <= threshold:
            severe_count += 1
    severe_ratio = severe_count / len(rates)

    return 20 * (1 - severe_ratio) 

#점수계산/RAG/보유종목 데이터를 리포트에 보낼 텍스트로 정리
#LLM에게 한 번에 넘겨줄 수 있는 하나의 텍스트 블록(문자열)으로 합쳐주는 함수
def build_report_context(state:AccountDiagnosisState) -> str:
    lines = []

    lines.append('[보유종목]')
    for h in state['vision_result'].holdings:
        lines.append(f"- {h.stock_name}: 수량 {h.quantity}, 평가금액 {h.eval_amount}, 수익률 {h.profit_loss_rate}%")

    lines.append('TOP20 미매칭 종목')
    unmatched = state.get('unmatched_stocks') or []
    if unmatched:
        for name in unmatched:
            lines.append(f"- {name}")
    else:
        lines.append('없음')

    lines.append('[진단점수]')
    lines.append(f"- 총점: {state.get('diagnosis_score')}")
    breakdown = state.get('score_breakdown') or {}
    for key, value in breakdown.items():
        lines.append(f"- {key}: {value}")

    lines.append('[RAG 용어 설명]')
    rag_context = state.get('rag_context') or []
    for item in rag_context:
        term = item['meta']['term']
        source = item['meta']['source']
        lines.append(f"- {term} (출처: {source}): {item['doc']}")

    return '\n'.join(lines)

#Gemini 응답 content에서 텍스트만 뽑아내는 함수
def extract_report_text(content) -> str:
    if isinstance(content, str):
        return content

    texts = []
    for block in content:
        if isinstance(block, dict) and block.get('type') == 'text':
            texts.append(block['text'])

    return "\n".join(texts)