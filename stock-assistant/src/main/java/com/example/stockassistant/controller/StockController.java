package com.example.stockassistant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.stockassistant.domain.StockMaster;
import com.example.stockassistant.service.StockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StockController {

	private final StockService stockService;

	@GetMapping("/api/stocks/top20")
	public List<StockMaster> getTop20() {
		return stockService.getTop20();
	}
}