# 일반질의 흐름의 LangGraph 그래프 구성.
# 원래 계획대로 그래프 조립 코드를 별도 모듈로 분리 (test_qa_flow.py는 이걸 가져다 테스트만 함).

from langgraph.graph import StateGraph, START, END
from models.graph_state import QAState
from nodes.qa_node import qa_node, answer_node


def build_qa_graph():
    graph = StateGraph(QAState)

    graph.add_node('qa', qa_node)
    graph.add_node('answer', answer_node)

    graph.add_edge(START, 'qa')
    graph.add_edge('qa', 'answer')
    graph.add_edge('answer', END)

    return graph.compile()


# 앱 전체에서 하나만 만들어서 재사용 (라우터/테스트 스크립트 둘 다 여기서 import)
app = build_qa_graph()
