import os
import requests

from dotenv import load_dotenv
from langchain.chat_models import init_chat_model
from langchain_core.tools import tool
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings

from models.graph_state import QAState

from services.qa_prompt import QA_SYSTEM_PROMPT

from datetime import date, timedelta

from rich import print as rprint

load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
llm = init_chat_model("gemini-3.1-flash-lite", model_provider="google_genai", api_key=api_key)

SPRING_BOOT_BASE_URL = "http://localhost:8080"

ko_embeddings = HuggingFaceEmbeddings(model_name="jhgan/ko-sroberta-multitask")
vectors = Chroma(
    collection_name='economic_terms',
    embedding_function=ko_embeddings,
    persist_directory="./chroma_db",
)
retriever = vectors.as_retriever(search_kwargs={'k': 3})

@tool
def search_glossary(query: str) -> str:
    """경제/금융 용어를 벡터DB에서 검색해서 설명과 출처를 반환한다.'"""
    docs = retriever.invoke(query)

    if not docs:
        return '관련 용어를 찾지 못했습니다.'

    results = []
    for d in docs:
        term = d.metadata.get('term')
        source = d.metadata.get('source')
        results.append(f"{term} (출처: {source}): {d.page_content}")

    return '\n'.join(results)

@tool
def get_stock_price(stock_name: str) -> str:
    """종목의 최근 시세를 조회한다."""
    stock_map = fetch_top20_stock_map()
    stock_code = find_stock_code(stock_name, stock_map)

    if stock_code is None:
        return f"'{stock_name}'은 Top20 종목이 아니라서 시세 조회를 지원하지 않습니다."

    price, bas_dd = find_latest_price(stock_code)

    if price is None:
        return f"'{stock_name}'의 최근 시세를 찾지 못했습니다."

    return f"{bas_dd} 기준 {stock_name} 시세: {price}"

@tool
def get_disclosure(stock_name: str) -> str:
    """종목의 최근 공시를 조회한다."""
    stock_map = fetch_top20_stock_map()
    stock_code = find_stock_code(stock_name, stock_map)

    if stock_code is None:
        return f"'{stock_name}'은 Top20 종목이 아니라서 공시 조회를 지원하지 않습니다."

    response = requests.get(f"{SPRING_BOOT_BASE_URL}/api/stocks/{stock_code}/disclosure")
    response.raise_for_status()

    return str(response.json())

@tool
def get_financial_info(stock_name: str, biz_year: str = None) -> str:
    """종목의 요약재무제표(매출액/영업이익/순이익/자산/부채 등)를 조회한다. biz_year(사업연도)는 생략 가능."""
    stock_map = fetch_top20_stock_map()
    stock_code = find_stock_code(stock_name, stock_map)

    if stock_code is None:
        return f"'{stock_name}'은 Top20 종목이 아니라서 재무정보 조회를 지원하지 않습니다."

    data, year = find_financial_info(stock_code, biz_year)

    if data is None:
        return f"'{stock_name}'의 재무정보를 찾지 못했습니다."

    return f"{year}년 기준 {stock_name} 요약재무제표: {data['response']['body']['items']['item']}"

#도구리스트
tools = [search_glossary, get_stock_price, get_disclosure, get_financial_info]
tools_by_name = {t.name: t for t in tools}
llm_with_tools = llm.bind_tools(tools)

#일반질의 노드
def qa_node(state: QAState) -> dict:
    print('1. 질의응답 노드 실행')

    question = state['question']
    today_str = f"오늘 날짜는 {date.today().isoformat()}입니다."
    ai_message = llm_with_tools.invoke([HumanMessage(content=f"{today_str}\n{question}")])
    #rprint(ai_message)

    tool_results = []
    for call in ai_message.tool_calls:
        tool_fn = tools_by_name[call['name']]
        result = tool_fn.invoke(call['args'])
        tool_results.append({'name': call['name'], 'args': call['args'], 'result': result})

    return {'tool_calls': ai_message.tool_calls, 'tool_results': tool_results}

#답변생성 노드
def answer_node(state: QAState) -> dict:
    print('2. 답변생성 노드 실행')

    context_text = build_answer_context(state)

    messages = [
        SystemMessage(content=QA_SYSTEM_PROMPT),   
        HumanMessage(content=context_text) 
    ]

    result = llm.invoke(messages)   #LLM 호출

    return {'answer': extract_report_text(result.content)} 



####헬퍼함수####
#tool_result를 답변생성 프롬프트용 텍스트로 정리
def build_answer_context(state: QAState) -> str:
    lines = []

    lines.append(f"[질문] {state['question']}")
    lines.append('[도구 실행 결과]')

    for result in state.get('tool_results') or []:
        lines.append(f"- {result['name']}: {result['result']}")

    return "\n".join(lines)

#Gemini 응답 content에서 텍스트만 뽑아내는 함수
def extract_report_text(content) -> str:
    if isinstance(content, str):
        return content

    texts = []
    for block in content:
        if isinstance(block, dict) and block.get('type') == 'text':
            texts.append(block['text'])

    return "\n".join(texts)

#종목명 -> 종목코드 매핑 (Top20 목록에서 가져옴)
def fetch_top20_stock_map() -> dict[str, str]:
    response = requests.get(f"{SPRING_BOOT_BASE_URL}/api/stocks/top20")
    response.raise_for_status()
    stocks = response.json()

    stock_map = {}
    for s in stocks:
        stock_map[s['stockName']] = s['stockCode']

    return stock_map

def find_latest_price(stock_code: str, max_days_back: int = 10):
    for days_back in range(max_days_back):
        bas_dd = (date.today() - timedelta(days=days_back)).strftime("%Y%m%d")
        response = requests.get(
            f"{SPRING_BOOT_BASE_URL}/api/stocks/{stock_code}/price",
            params={"basDd": bas_dd},
        )
        response.raise_for_status()

        if not response.text.strip():   # 응답 본문이 비어있으면 그 날은 시세 없음 -> 하루 더 거슬러가기
            continue

        price = response.json()

        if price is not None:
            return price, bas_dd

    return None, None

def find_financial_info(stock_code: str, biz_year: str | None, max_years_back: int = 3):
    start_year = int(biz_year) if biz_year else date.today().year - 1

    for offset in range(max_years_back):
        year = str(start_year - offset)
        response = requests.get(
            f"{SPRING_BOOT_BASE_URL}/api/stocks/{stock_code}/financial",
            params={"bizYear": year},
        )
        print("status:", response.status_code)
        print("body:", response.text[:500])
        response.raise_for_status()

        if not response.text.strip():
            continue

        data = response.json()
        total_count = data.get('response', {}).get('body', {}).get('totalCount', 0)

        if total_count > 0:
            return data, year

    return None, None

#정확한 이름이 없으면 포함관계로 한번 더 찾기 (예: '하이닉스' -> 'SK하이닉스')
def find_stock_code(stock_name: str, stock_map: dict[str, str]) -> str | None:
    if stock_name in stock_map:
        return stock_map[stock_name]

    for name, code in stock_map.items():
        if stock_name in name:
            return code

    return None
