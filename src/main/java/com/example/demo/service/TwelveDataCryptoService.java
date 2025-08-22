package com.example.demo.service;

import com.example.demo.entities.Asset;
import com.example.demo.repository.AssetRepository;
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

    @Value("${twelvedata.apikey}")
    private String apiKey;

    private final AssetRepository assetRepository;
    private final RestTemplate restTemplate;

    private final List<String> cryptoSymbols = List.of(
            "BTC/USD", "ETH/USD", "BNB/USD", "XRP/USD",
            "ADA/USD", "SOL/USD", "DOGE/USD", "DOT/USD"
    );

    public TwelveDataCryptoService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedRate = 90000) // every 1.5 minutes
    public void updateCryptos() {
        String symbolsParam = String.join(",", cryptoSymbols);
        String url = "https://api.twelvedata.com/quote?symbol=" + symbolsParam + "&apikey=" + apiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            cryptoSymbols.forEach(symbol -> {
                Map<String, Object> data = (Map<String, Object>) response.get(symbol);
                if (data != null) {
                    Asset asset = assetRepository.findByTicker(symbol).orElse(new Asset());
                    asset.setTicker(symbol);
                    asset.setName(symbol.split("/")[0]);
                    asset.setOpenPrice(new BigDecimal((String)data.get("open")));
                    asset.setHighPrice(new BigDecimal((String)data.get("high")));
                    asset.setLowPrice(new BigDecimal((String)data.get("low")));
                    asset.setClosePrice(new BigDecimal((String)data.get("close")));
                    asset.setVolume(new BigDecimal((String)data.get("volume")));
                    asset.setPreviousClose(new BigDecimal((String)data.get("previous_close")));
                    asset.setPercentChange(new BigDecimal((String)data.get("percent_change")));
                    asset.setLastUpdated(Instant.now());
                    assetRepository.save(asset);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}