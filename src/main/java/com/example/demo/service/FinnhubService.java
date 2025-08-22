package com.example.demo.service;

import com.example.demo.entities.Asset;
import com.example.demo.entities.MarketType;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.MarketTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class FinnhubService {

    private static final Logger log = LoggerFactory.getLogger(FinnhubService.class);

    private final AssetRepository assetRepository;
    private final MarketTypeRepository marketTypeRepository;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${finnhub.api.key}")
    private String apiKey;

    @Value("${app.assets.per.market:20}")
    private int assetsPerMarket;

    @Value("${app.rest.sleep.ms:200}")
    private long restSleepMs;

    public FinnhubService(AssetRepository assetRepository, MarketTypeRepository marketTypeRepository) {
        this.assetRepository = assetRepository;
        this.marketTypeRepository = marketTypeRepository;
    }

    @PostConstruct
    public void init() {
        // Ensure MarketType rows exist
        MarketType stockType = marketTypeRepository.findByName("STOCK")
                .orElseGet(() -> marketTypeRepository.save(new MarketType("STOCK")));
       // MarketType etfType = marketTypeRepository.findByName("ETF")
               // .orElseGet(() -> marketTypeRepository.save(new MarketType("ETF")));
        MarketType cryptoType = marketTypeRepository.findByName("CRYPTO")
                .orElseGet(() -> marketTypeRepository.save(new MarketType("CRYPTO")));

        try {
            seedStocks(stockType);
           // seedEtfs(etfType);
            seedCryptos(cryptoType);
        } catch (Exception e) {
            log.error("Error during initial seeding of symbols", e);
        }
    }

    // Seed top N stocks (US exchange)
    private void seedStocks(MarketType marketType) throws Exception {
        String url = "https://finnhub.io/api/v1/stock/symbol?exchange=US&token=" + apiKey;
        String response = rest.getForObject(url, String.class);
        JsonNode arr = mapper.readTree(response);

        int count = 0;
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                if (count >= assetsPerMarket) break;
                String symbol = node.path("symbol").asText(null);
                String description = node.path("description").asText(null);
                String exchange = node.path("exchange").asText("US");
                if (symbol == null || symbol.isEmpty()) continue;
                saveOrUpdateAsset(symbol, description, exchange, marketType);
                count++;
            }
            log.info("Seeded {} stocks", count);
        }
    }

    // Seed ETFs
    /*
    private void seedEtfs(MarketType marketType) throws Exception {
        String url = "https://finnhub.io/api/v1/etf/list?token=" + apiKey;
        String response = rest.getForObject(url, String.class);
        JsonNode arr = mapper.readTree(response);

        int count = 0;
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                if (count >= assetsPerMarket) break;
                String symbol = node.path("symbol").asText(null);
                String name = node.path("name").asText(null);
                String exchange = node.path("exchange").asText("UNKNOWN");
                if (symbol == null || symbol.isEmpty()) continue;
                saveOrUpdateAsset(symbol, name, exchange, marketType);
                count++;
            }
            log.info("Seeded {} ETFs", count);
        }
    }
  */
    // Seed crypto symbols from a given exchange (BINANCE used here)
    private void seedCryptos(MarketType marketType) throws Exception {
        String exchange = "BINANCE";
        String url = "https://finnhub.io/api/v1/crypto/symbol?exchange=" +
                URLEncoder.encode(exchange, StandardCharsets.UTF_8) +
                "&token=" + apiKey;
        String response = rest.getForObject(url, String.class);
        JsonNode arr = mapper.readTree(response);

        int count = 0;
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                if (count >= assetsPerMarket) break;
                String symbol = node.path("symbol").asText(null); // e.g. BINANCE:BTCUSDT
                String description = node.path("description").asText(null);
                if (symbol == null || symbol.isEmpty()) continue;
                saveOrUpdateAsset(symbol, description, exchange, marketType);
                count++;
            }
            log.info("Seeded {} cryptos", count);
        }
    }

    private void saveOrUpdateAsset(String ticker, String name, String exchange, MarketType marketType) {
        Optional<Asset> opt = assetRepository.findByTicker(ticker);
        if (opt.isPresent()) {
            Asset a = opt.get();
            boolean changed = false;
            if (name != null && !name.equals(a.getName())) { a.setName(name); changed = true; }
            if (exchange != null && !exchange.equals(a.getExchange())) { a.setExchange(exchange); changed = true; }
            if (changed) {
                assetRepository.save(a);
            }
        } else {
            Asset a = new Asset();
            a.setTicker(ticker);
            a.setName(name);
            a.setExchange(exchange);
            a.setMarketType(marketType);
            assetRepository.save(a);
        }
    }

    /**
     * Scheduled update: updates quotes for all assets in DB.
     * Runs every app.polling.interval.ms by default (configured in application.properties).
     */
    @Scheduled(fixedDelayString = "${app.polling.interval.ms:120000}")
    public void updateAllQuotes() {
        List<Asset> assets = assetRepository.findAll();
        if (assets.isEmpty()) {
            log.info("No assets in DB - skipping update");
            return;
        }

        log.info("Starting quotes update for {} assets at {}", assets.size(), Instant.now());
        for (Asset asset : assets) {
            try {
                updateSingleAssetQuote(asset);
                Thread.sleep(restSleepMs); // avoid burst
            } catch (Exception e) {
                log.warn("Failed to update {}: {}", asset.getTicker(), e.getMessage());
            }
        }
        log.info("Quotes update finished at {}", Instant.now());
    }

    // Public method so controller can trigger update manually
    public void triggerUpdateNow() {
        updateAllQuotes();
    }

    private void updateSingleAssetQuote(Asset asset) throws Exception {
        String symbolEncoded = URLEncoder.encode(asset.getTicker(), StandardCharsets.UTF_8);
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbolEncoded + "&token=" + apiKey;
        String response = rest.getForObject(url, String.class);
        if (response == null || response.isEmpty()) {
            log.debug("Empty response for symbol {}", asset.getTicker());
            return;
        }
        JsonNode node = mapper.readTree(response);

        BigDecimal current = parseDecimal(node.path("c").asText(null));
        BigDecimal high = parseDecimal(node.path("h").asText(null));
        BigDecimal low = parseDecimal(node.path("l").asText(null));
        BigDecimal open = parseDecimal(node.path("o").asText(null));
        BigDecimal prevClose = parseDecimal(node.path("pc").asText(null));

        asset.setOpenPrice(open);
        asset.setHighPrice(high);
        asset.setLowPrice(low);
        asset.setClosePrice(current);
        asset.setPreviousClose(prevClose);

        if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) != 0 && current != null) {
            BigDecimal pct = current.subtract(prevClose)
                    .divide(prevClose, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(6, RoundingMode.HALF_UP);
            asset.setPercentChange(pct);
        } else {
            asset.setPercentChange(null);
        }

        // volume not provided by quote endpoint for every symbol; leave null or fetch candles if needed
        asset.setVolume(null);

        // Simulated spread (Option 2)
        if (current != null) {
            asset.setPriceToBuy(current.multiply(BigDecimal.valueOf(1.001)).setScale(6, RoundingMode.HALF_UP));
            asset.setPriceToSell(current.multiply(BigDecimal.valueOf(0.999)).setScale(6, RoundingMode.HALF_UP));
        } else {
            asset.setPriceToBuy(null);
            asset.setPriceToSell(null);
        }

        asset.setLastUpdated(Instant.now());
        assetRepository.save(asset);
    }

    // Safe BigDecimal parser - returns null on bad input
    private BigDecimal parseDecimal(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.isEmpty()) return null;
        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            // sometimes Finnhub returns 0 or non-numeric - handle gracefully
            try {
                // try parsing as double fallback
                double d = Double.parseDouble(text);
                return BigDecimal.valueOf(d);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
