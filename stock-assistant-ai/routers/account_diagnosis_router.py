# 계좌진단 흐름을 HTTP로 열어주는 라우터.
# Spring Boot(또는 화면)가 계좌 캡처 이미지를 업로드하면, LangGraph 계좌진단 그래프를 돌려서 결과를 돌려준다.

from fastapi import APIRouter, File, UploadFile
from fastapi.responses import JSONResponse

from graph.account_diagnosis_graph import app as account_diagnosis_app

router = APIRouter()


@router.post("/account-diagnosis")
async def diagnose_account(image: UploadFile = File(...)):
    """계좌 캡처 이미지를 업로드받아 계좌진단 그래프(인식->신뢰도체크->Top20검증->RAG검색->점수계산->리포트)를 실행한다."""

    image_bytes = await image.read()
    result = account_diagnosis_app.invoke({"images_byte": image_bytes}) #그래프 실행

    # 신뢰도가 낮아서 재업로드가 필요한 경우 (retry 분기를 탄 경우)
    if result.get('error'):
        return JSONResponse(
            content={
                'is_reliable': False,
                'error': result['error'],
            },
            media_type="application/json; charset=utf-8",
        )

    #최종답을 JSON으로 넘김
    return JSONResponse(
        content={
            'is_reliable': True,
            'holdings': [h.model_dump() for h in result['vision_result'].holdings],
            'confidence_score': result['vision_result'].confidence_score,
            'unmatched_stocks': result.get('unmatched_stocks') or [],
            'diagnosis_score': result.get('diagnosis_score'),
            'score_breakdown': result.get('score_breakdown'),
            'final_report': result.get('final_report'),
        },
        media_type="application/json; charset=utf-8",
    )
