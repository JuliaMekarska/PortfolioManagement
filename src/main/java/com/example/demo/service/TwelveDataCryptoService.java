package com.example.demo.service;

import com.example.demo.entities.Asset;
import com.example.demo.entities.MarketType;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.MarketTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class TwelveDataCryptoService {
    private static final Logger log = LoggerFactory.getLogger(TwelveDataCryptoService.class);

    private final AssetRepository assetRepository;
    private final MarketTypeRepository marketTypeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${twelvedata.apikey}")
    private String API_KEY;

    private static final String BASE_URL = "https://api.twelvedata.com";

    // only 4 popular cryptos
    private static final List<String> CRYPTOS = List.of("BTC/USD", "ETH/USD", "BNB/USD", "XRP/USD");
    private final MarketType cryptoType;

    public TwelveDataCryptoService(AssetRepository assetRepository, MarketTypeRepository marketTypeRepository) {
        this.assetRepository = assetRepository;
        this.marketTypeRepository = marketTypeRepository;

        // Hardcode market type "CRYPTO"
        this.cryptoType = marketTypeRepository.findByName("CRYPTO")
                .orElseGet(() -> marketTypeRepository.save(new MarketType("CRYPTO")));
    }

    @Scheduled(fixedRate = 90000) // 1.5 minutes
    public void fetchCryptoData() {
        for (String symbol : CRYPTOS) {
            Map<String, Object> quoteResponse = fetchQuote(symbol);
            if (quoteResponse != null) {
                Asset asset = saveQuote(symbol, quoteResponse);
                fetchAndUpdatePrice(asset);
            }
        }
    }

    private Map<String, Object> fetchQuote(String symbol) {
        String url = BASE_URL + "/quote?symbol=" + symbol + "&apikey=" + API_KEY;
        log.info("Fetching quote URL: {}", url);
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("symbol") != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Error fetching quote for {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    private Asset saveQuote(String symbol, Map<String, Object> response) {
        Asset asset = assetRepository.findByTicker(symbol)
                .orElseGet(() -> {
                    Asset newAsset = new Asset();
                    newAsset.setTicker(symbol);
                    newAsset.setName((String) response.get("name"));
                    newAsset.setExchange((String) response.get("exchange"));
                    newAsset.setMarketType(cryptoType); // assign market type here
                    return newAsset;
                });

        asset.setOpenPrice(toBigDecimal(response.get("open")));
        asset.setHighPrice(toBigDecimal(response.get("high")));
        asset.setLowPrice(toBigDecimal(response.get("low")));
        asset.setClosePrice(toBigDecimal(response.get("close")));
        asset.setVolume(toBigDecimal(response.get("volume")));
        asset.setPreviousClose(toBigDecimal(response.get("previous_close")));
        asset.setPercentChange(toBigDecimal(response.get("percent_change")));
        asset.setLastUpdated(Instant.now());
        asset.setMarketType(cryptoType); // ensure market type is set even if asset existed

        return assetRepository.save(asset);
    }

    private void fetchAndUpdatePrice(Asset asset) {
        String symbol = asset.getTicker();
        String url = BASE_URL + "/price?symbol=" + symbol + "&apikey=" + API_KEY;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("price") != null) {
                BigDecimal current = new BigDecimal(response.get("price").toString());
                asset.setPriceToBuy(current.multiply(BigDecimal.valueOf(0.99))); // -1%
                asset.setPriceToSell(current.multiply(BigDecimal.valueOf(1.01))); // +1%
                asset.setLastUpdated(Instant.now());
                assetRepository.save(asset);
            }
        } catch (Exception e) {
            log.error("Error fetching price for {}: {}", symbol, e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        try {
            return value != null ? new BigDecimal(value.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
