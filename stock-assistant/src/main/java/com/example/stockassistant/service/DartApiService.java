package com.example.stockassistant.service;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import lombok.RequiredArgsConstructor;

// DART(전자공시) API 호출 서비스
@Service
@RequiredArgsConstructor
public class DartApiService {

	private final RestClient restClient;

	@Value("${dart.api.key}")
	private String dartApiKey;

	// DART는 종목코드 대신 corp_code(고유번호)로 조회해야 함
	// -> 전체 회사 매핑표(zip)를 받아서 종목코드->corp_code로 변환. 한 번만 실행하면 됨.
	public Map<String, String> fetchCorpCodeMapping() {
		String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + dartApiKey;
		byte[] zipBytes = restClient.get().uri(url).retrieve().body(byte[].class);

		Map<String, String> stockCodeToCorpCode = new HashMap<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (!entry.getName().endsWith(".xml")) {
					continue;
				}
				// zip 스트림을 파서에 바로 넘기면 스트림이 닫혀버려서 바이트로 먼저 읽음
				byte[] xmlBytes = zis.readAllBytes();

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));

				NodeList list = doc.getElementsByTagName("list");
				for (int i = 0; i < list.getLength(); i++) {
					Element el = (Element) list.item(i);
					String corpCode = getTagValue(el, "corp_code");
					String stockCode = getTagValue(el, "stock_code");
					if (stockCode != null && !stockCode.isBlank()) {
						stockCodeToCorpCode.put(stockCode, corpCode);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("DART 고유번호 매핑 파싱 실패", e);
		}
		return stockCodeToCorpCode;
	}

	private String getTagValue(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
			return null;
		}
		return nodes.item(0).getTextContent().trim();
	}

	// 기업개황 조회 - 법인등록번호(jurir_no) 추출용. corp_code 필요 (corp-code-sync 먼저 실행돼 있어야 함)
	// 금융위원회 기업재무정보 API가 요구하는 crno가 바로 이 jurir_no임 (DART corp_code와는 별개 값)
	@SuppressWarnings("unchecked")
	public String getCorporateRegNo(String corpCode) {
		String url = "https://opendart.fss.or.kr/api/company.json"
				+ "?crtfc_key=" + dartApiKey
				+ "&corp_code=" + corpCode;
		Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
		if (body == null || body.get("jurir_no") == null) {
			return null;
		}
		String jurirNo = body.get("jurir_no").toString().trim();
		return jurirNo.isBlank() ? null : jurirNo;
	}

	// 특정 회사 공시 목록 조회 (최근 1년, 최신순 10건)
	// 날짜 범위 안 넣으면 오늘 하루만 검색돼서 주말엔 결과 0건 나옴 -> 1년치로 넉넉하게
	// disclosure 캐시로 30분 저장
	@Cacheable(value = "disclosure", key = "#corpCode")
	public Map<String, Object> getDisclosureList(String corpCode) {
		String endDe = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
		String bgnDe = java.time.LocalDate.now().minusYears(1)
				.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

		String url = "https://opendart.fss.or.kr/api/list.json"
				+ "?crtfc_key=" + dartApiKey
				+ "&corp_code=" + corpCode
				+ "&bgn_de=" + bgnDe
				+ "&end_de=" + endDe
				+ "&page_no=1&page_count=10";
		return restClient.get().uri(url).retrieve().body(Map.class);
	}
}
