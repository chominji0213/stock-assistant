# 계좌진단 흐름의 LangGraph 그래프 구성.
# 원래 계획대로 그래프 조립 코드를 별도 모듈로 분리 (test_graph_flow.py는 이걸 가져다 테스트만 함).

from langgraph.graph import StateGraph, START, END
from models.graph_state import AccountDiagnosisState
from nodes.account_diagnosis_node import (
    recognize_node,
    reliability_check_node,
    top20_check_node,
    rag_search_node,
    score_node,
    report_node,
)


def route_branch(state: AccountDiagnosisState) -> str:
    print('조건부엣지에서 쓰는 라우팅함수')

    return state['branch']


def retry_node(state: AccountDiagnosisState) -> dict:
    print('재업로드 요청')

    return {'error': '이미지 신뢰도가 낮아서 다시 재업로드 요청드립니다.'}


def build_account_diagnosis_graph():
    graph = StateGraph(AccountDiagnosisState)

    graph.add_node('recognize', recognize_node)
    graph.add_node('reliability_check', reliability_check_node)
    graph.add_node('top20_check', top20_check_node)
    graph.add_node("rag_search", rag_search_node)
    graph.add_node("score", score_node)
    graph.add_node("report", report_node)
    graph.add_node("retry", retry_node)

    graph.add_edge(START, 'recognize')
    graph.add_edge('recognize', 'reliability_check')
    graph.add_conditional_edges('reliability_check', route_branch, {'proceed': 'top20_check', 'retry': 'retry'})
    graph.add_edge("top20_check", "rag_search")
    graph.add_edge("rag_search", "score")
    graph.add_edge("score", "report")
    graph.add_edge('report', END)
    graph.add_edge('retry', END)

    return graph.compile()


# 앱 전체에서 하나만 만들어서 재사용 (라우터/테스트 스크립트 둘 다 여기서 import)
app = build_account_diagnosis_graph()
