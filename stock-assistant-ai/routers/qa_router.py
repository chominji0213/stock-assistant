# 일반질의 흐름을 HTTP로 열어주는 라우터.
# Spring Boot(또는 화면)가 질문을 보내면, LangGraph 일반질의 그래프(Tool Calling)를 돌려서 답변을 돌려준다.

from fastapi import APIRouter
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from graph.qa_graph import app as qa_app

router = APIRouter()


class QARequest(BaseModel):
    question: str


@router.post("/qa")
def ask_question(req: QARequest):
    """질문을 받아 일반질의 그래프(Tool Calling: 시세/공시/재무정보/용어사전)를 실행하고 답변을 반환한다."""

    result = qa_app.invoke({"question": req.question})

    return JSONResponse(
        content={
            'question': req.question,
            'tool_calls': result.get('tool_calls') or [],
            'answer': result.get('answer'),
        },
        media_type="application/json; charset=utf-8",
    )
    
