# 일반질의 흐름을 HTTP로 열어주는 라우터.
# Spring Boot(또는 화면)가 질문(+ 선택적으로 뉴스 캡처 등 이미지 여러 장)을 보내면,
# LangGraph 일반질의 그래프(Tool Calling)를 돌려서 답변을 돌려준다.

import base64
from typing import List

from fastapi import APIRouter, File, Form, UploadFile
from fastapi.responses import JSONResponse

from graph.qa_graph import app as qa_app

router = APIRouter()


@router.post("/qa")
async def ask_question(question: str = Form(...), images: List[UploadFile] = File(default=[])):
    """질문을 받아 일반질의 그래프(Tool Calling: 시세/공시/재무정보/용어사전)를 실행하고 답변을 반환한다.
    이미지가 (여러 장) 같이 첨부되면(예: 뉴스 기사 캡처) 질문+답변 생성 단계에서 같이 참고한다."""

    initial_state = {"question": question}
    print(f"[qa_router] question={question!r}, 받은 파일 파트 개수={len(images)}")

    image_list = []
    for image in images:
        if not image.filename:
            continue
        image_bytes = await image.read()
        image_list.append({
            "base64": base64.b64encode(image_bytes).decode("utf-8"),
            "mime": image.content_type or "image/png",
        })

    if image_list:
        initial_state["images"] = image_list

    result = qa_app.invoke(initial_state)

    return JSONResponse(
        content={
            'question': question,
            'tool_calls': result.get('tool_calls') or [],
            'answer': result.get('answer'),
        },
        media_type="application/json; charset=utf-8",
    )
