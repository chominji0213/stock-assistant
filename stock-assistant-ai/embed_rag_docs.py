import json
import chromadb
from chromadb.utils import embedding_functions

#ChromaDB 생성
client = chromadb.PersistentClient(path="./chroma_db")

#허깅페이스에서 한국어에 특화된 임베딩 모델을 가져옴
ko_ef = embedding_functions.SentenceTransformerEmbeddingFunction(model_name="jhgan/ko-sroberta-multitask")

# 기존 컬렉션 삭제 후 새 임베딩 함수로 재생성
try:
    client.delete_collection(name="economic_terms")
except Exception:
    pass

#economic_terms 컬렉션(테이블) 생성
collection = client.create_collection(name="economic_terms", metadata={"hnsw:space": "cosine"}, embedding_function=ko_ef)

#PDF 파싱에서 만들어둔 JSONL 파일에서 한줄씩 읽어가지고 리스트로 변환
with open("data/rag_docs/economic_terms.jsonl", "r", encoding="utf-8") as f:
    lines = [json.loads(line) for line in f]

#실제 검색될 텍스트(documents), 부가정보(metadatas), 각 항목의 고유 id
documents, metadatas, ids = [], [], []


for i, item in enumerate(lines):    
    # 각 항목을 구분할 id
    ids.append(f"term_{i}")
    
    # "용어: 설명" 형태로 합쳐서 임베딩할 텍스트로 사용
    documents.append(f"{item['term']}: {item['definition']}")

    # 검색 결과에 같이 붙여줄 메타데이터
    metadatas.append({
        "term": item["term"],
        "source": item["source"],
        "as_of_date": item["as_of_date"],
        "related_terms": ", ".join(item["related_terms"]),
    })

#컬렉션에 적재
collection.add(documents=documents, metadatas=metadatas, ids=ids)
print(f"적재 완료: {collection.count()}개")

#테스트 쿼리
results = collection.query(query_texts=["PER이 뭐야?"], n_results=5)
for doc, meta in zip(results["documents"][0], results["metadatas"][0]):
    print("-" * 40)
    print(doc[:100])
    print("출처:", meta["source"], "/ 기준일:", meta["as_of_date"])
