package com.example.demo.controller;

import com.example.demo.entities.Asset;
import com.example.demo.repository.AssetRepository;
import com.example.demo.service.FinnhubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assets")
public class MarketController {

    private AssetRepository assetRepository;
    private FinnhubService finnhubService;

    public void AssetController(AssetRepository assetRepository, FinnhubService finnhubService) {
        this.assetRepository = assetRepository;
        this.finnhubService = finnhubService;
    }

    public MarketController(AssetRepository assetRepository, FinnhubService finnhubService) {
        this.assetRepository = assetRepository;
        this.finnhubService = finnhubService;
    }

    // Get all assets
    @GetMapping
    public ResponseEntity<List<Asset>> getAll() {
        return ResponseEntity.ok(assetRepository.findAll());
    }

    // Get asset by ticker
    @GetMapping("/{ticker}")
    public ResponseEntity<Asset> getByTicker(@PathVariable String ticker) {
        Optional<Asset> opt = assetRepository.findByTicker(ticker);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Trigger a manual update for all assets (useful for testing)

}