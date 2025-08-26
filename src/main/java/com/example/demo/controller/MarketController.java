package com.example.demo.controller;

import com.example.demo.entities.Asset;
import com.example.demo.entities.MarketType;
import com.example.demo.exception.MarketNotFoundException;
import com.example.demo.exception.SymbolNotFoundException;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.MarketTypeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class MarketController {

    private final AssetRepository assetRepository;
    private final MarketTypeRepository marketTypeRepository;

    public MarketController(AssetRepository assetRepository, MarketTypeRepository marketTypeRepository) {
        this.assetRepository = assetRepository;
        this.marketTypeRepository = marketTypeRepository;
    }

    @GetMapping("/market-types")
    public List<MarketType> getAllMarketTypes() {
        return marketTypeRepository.findAll();
    }

    @GetMapping("/assets")
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    @GetMapping("/assets/market/{marketName}")
    public List<Asset> getAssetsByMarket(@PathVariable String marketName) {
        MarketType market = marketTypeRepository.findByName(marketName)
                .orElseThrow(() -> new MarketNotFoundException(marketName));

        return assetRepository.findByMarketType(market);
    }

    @GetMapping("/assets/{ticker}")
    public Asset getAssetByTicker(@PathVariable String ticker) {
        return assetRepository.findByTicker(ticker)
                .orElseThrow(() -> new SymbolNotFoundException(ticker));
    }
}