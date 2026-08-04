"""
한국은행 경제금융용어 800선 PDF -> RAG용 청크(JSONL) 변환 스크립트
- 용어 단위로 청크 분리 (제목 폰트 크기/굵기로 경계 판단)
- 출처/기준일 메타데이터 부착, 연관검색어 분리
실행: venv 활성화 후 `python scripts_build_rag_docs.py`
출력: data/rag_docs/economic_terms.jsonl (ChromaDB 임베딩 작업에서 그대로 읽어 쓰면 됨)
"""
import json
import os
import re

import pdfplumber

PDF_PATH = os.path.join(os.path.dirname(__file__), "..", "docs", "2026_경제금융용어 800선.pdf")
OUT_DIR = os.path.join(os.path.dirname(__file__), "data", "rag_docs")
OUT_PATH = os.path.join(OUT_DIR, "economic_terms.jsonl")
START_PAGE, END_PAGE = 20, 427
SOURCE_NAME = "한국은행 경제금융용어 800선"
AS_OF = "2026"


def is_header_font(w):
    # 폰트 굵기/크기만 확인 (위치는 여기서 안 봄 - 줄 단위 판별에서 첫 단어 위치만 따로 확인)
    return w["size"] >= 13.5 and ("Bold" in w["fontname"] or "EB" in w["fontname"])


def is_noise_word(w):
    if w["top"] < 50 and w["size"] <= 9:
        return True
    if w["text"] == "I" and w["size"] < 12:
        return True
    if len(w["text"]) == 1 and w["text"] in "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ":
        return True
    if w["text"].isdigit() and w["x0"] > 400:
        return True
    return False


def group_by_line(words, tolerance=3):
    # 같은 줄이어도 폰트가 바뀌면(한글<->영문) 세로 위치(top)가 미묘하게(1px 이하) 달라질 수 있어서,
    # round()로 정수 그리드에 스냅하면 애매한 값이 다른 줄로 튕겨나가는 문제가 있었음(예: "PER"만 분리됨).
    # 그래서 정수 반올림 대신, top 기준으로 정렬한 뒤 값이 가까운(tolerance 이내) 단어들끼리 묶는 방식으로 변경.
    if not words:
        return []
    sorted_words = sorted(words, key=lambda w: w["top"])
    lines = []
    current_line = [sorted_words[0]]
    current_top = sorted_words[0]["top"]
    for w in sorted_words[1:]:
        if abs(w["top"] - current_top) <= tolerance:
            current_line.append(w)
        else:
            # 줄이 끝났으니 왼쪽->오른쪽 읽는 순서로 정렬해서 확정
            lines.append(sorted(current_line, key=lambda w: w["x0"]))
            current_line = [w]
            current_top = w["top"]
    lines.append(sorted(current_line, key=lambda w: w["x0"]))
    return lines


def parse_terms(pdf_path):
    terms = []
    current_term, current_lines = None, []
    with pdfplumber.open(pdf_path) as pdf:
        for pi in range(START_PAGE, min(END_PAGE, len(pdf.pages))):
            words = [w for w in pdf.pages[pi].extract_words(extra_attrs=["fontname", "size"]) if not is_noise_word(w)]
            grouped_lines = group_by_line(words)
            for line_words in grouped_lines:
                line_text = " ".join(w["text"] for w in line_words)
                # 제목 판별: 줄의 첫 단어가 왼쪽 여백(x0<95)에서 시작하고, 줄 전체가 제목 폰트여야 함
                # (영문 약어가 섞인 제목은 폰트가 바뀌면서 단어가 쪼개져 두 번째 단어부터 x0가 커지므로,
                #  x0 체크는 첫 단어에만 적용하고 나머지는 폰트만 확인)
                if line_words and line_words[0]["x0"] < 95 and all(is_header_font(w) for w in line_words):
                    if current_term:
                        terms.append((current_term, " ".join(current_lines).strip()))
                    current_term, current_lines = line_text.strip(), []
                else:
                    current_lines.append(line_text)
        if current_term:
            terms.append((current_term, " ".join(current_lines).strip()))
    return terms


def split_related(body):
    # "연관검색어 A, B, C" 꼬리 부분을 본문과 분리
    # 레이아웃상 드물게 "A, B, C 연관검색어"처럼 순서가 뒤바뀌어 추출되는 경우가 있어 양쪽 다 확인
    idx = body.find("연관검색어")
    if idx == -1:
        return body, []
    before = body[:idx].strip()
    after = body[idx + len("연관검색어"):].strip()
    candidate = after if after else before
    if not candidate or len(candidate) > 120:
        # 관련어 후보로 보기엔 너무 길다 = 문장 중간에 우연히 낀 경우 -> 태그만 제거
        return (before + " " + after).strip(), []
    related = [t.strip() for t in re.split(r"[,，]", candidate) if t.strip()]
    main = before if after else body[:idx].strip()
    return main, related


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    raw_terms = parse_terms(PDF_PATH)
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        for term, body in raw_terms:
            main_body, related = split_related(body)
            doc = {
                "term": term,
                "definition": main_body,
                "related_terms": related,
                "source": SOURCE_NAME,
                "as_of_date": AS_OF,
            }
            f.write(json.dumps(doc, ensure_ascii=False) + "\n")
    print(f"완료: {len(raw_terms)}개 용어 -> {OUT_PATH}")


if __name__ == "__main__":
    main()
