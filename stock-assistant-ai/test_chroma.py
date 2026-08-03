import chromadb

client = chromadb.PersistentClient(path="./chroma_db")  # 로컬 폴더에 저장
collection = client.get_or_create_collection(name="test")

collection.add(
    documents=["삼성전자는 반도체 회사입니다", "SK하이닉스는 메모리 반도체를 만듭니다"],
    ids=["doc1", "doc2"]
)

results = collection.query(query_texts=["반도체 회사가 어디야?"], n_results=1)
print(results)