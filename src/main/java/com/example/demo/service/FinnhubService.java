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
import java.util.Arrays;
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

    private static final List<String> TOP_STOCKS = Arrays.asList(
            "AAPL","MSFT","AMZN","GOOGL","TSLA","META","NVDA","JPM","V","MA",
            "DIS","NFLX","KO","PFE","BAC","ORCL","INTC","CSCO","XOM","CVX"
    );

    public FinnhubService(AssetRepository assetRepository, MarketTypeRepository marketTypeRepository) {
        this.assetRepository = assetRepository;
        this.marketTypeRepository = marketTypeRepository;
    }

    @PostConstruct
    public void init() {
        MarketType stockType = marketTypeRepository.findByName("STOCK")
                .orElseGet(() -> marketTypeRepository.save(new MarketType("STOCK")));

        try {
            seedTopStocks(stockType);
        } catch (Exception e) {
            log.error("Error during initial seeding", e);
        }
    }

    private void seedTopStocks(MarketType marketType) {
        int count = 0;
        for (String sym : TOP_STOCKS) {
            if (count >= assetsPerMarket) break;
            try {
                String name = fetchStockName(sym);
                saveOrUpdateAsset(sym, name, "US", marketType);
                count++;
                Thread.sleep(restSleepMs);
            } catch (Exception ex) {
                log.warn("Failed to seed stock {}: {}", sym, ex.getMessage());
            }
        }
        log.info("Seeded {} predefined stocks", count);
    }

    private String fetchStockName(String symbol) {
        try {
            String url = "https://finnhub.io/api/v1/stock/profile2?symbol=" +
                    URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
                    "&token=" + apiKey;
            String resp = rest.getForObject(url, String.class);
            if (resp == null || resp.isEmpty()) return symbol;
            JsonNode node = mapper.readTree(resp);
            String name = node.path("name").asText(null);
            return name != null ? name : symbol;
        } catch (Exception e) {
            log.debug("Could not fetch profile for {}: {}", symbol, e.getMessage());
            return symbol;
        }
    }

    private void saveOrUpdateAsset(String ticker, String name, String exchange, MarketType marketType) {
        Optional<Asset> opt = assetRepository.findByTicker(ticker);
        if (opt.isPresent()) {
            Asset a = opt.get();
            boolean changed = false;
            if (name != null && !name.equals(a.getName())) { a.setName(name); changed = true; }
            if (exchange != null && !exchange.equals(a.getExchange())) { a.setExchange(exchange); changed = true; }
            if (a.getMarketType() == null || !a.getMarketType().getName().equals(marketType.getName())) {
                a.setMarketType(marketType);
                changed = true;
            }
            if (changed) assetRepository.save(a);
        } else {
            Asset a = new Asset();
            a.setTicker(ticker);
            a.setName(name);
            a.setExchange(exchange);
            a.setMarketType(marketType);
            assetRepository.save(a);
        }
    }

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
                updateSingleStockQuote(asset);
                Thread.sleep(restSleepMs);
            } catch (Exception e) {
                log.warn("Failed to update {}: {}", asset.getTicker(), e.getMessage());
            }
        }
        log.info("Quotes update finished at {}", Instant.now());
    }

    private void updateSingleStockQuote(Asset asset) throws Exception {
        String symbolEncoded = URLEncoder.encode(asset.getTicker(), StandardCharsets.UTF_8);
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbolEncoded + "&token=" + apiKey;
        String response = rest.getForObject(url, String.class);
        if (response == null || response.isEmpty()) return;

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

        if (prevClose != null && current != null && prevClose.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal pct = current.subtract(prevClose)
                    .divide(prevClose, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(6, RoundingMode.HALF_UP);
            asset.setPercentChange(pct);
        } else {
            asset.setPercentChange(null);
        }

        asset.setVolume(null);

        if (current != null) {
            asset.setPriceToBuy(current.multiply(BigDecimal.valueOf(1.001)).setScale(6, RoundingMode.HALF_UP));
            asset.setPriceToSell(current.multiply(BigDecimal.valueOf(0.999)).setScale(6, RoundingMode.HALF_UP));
        } else {
            asset.setPriceToBuy(null);
            asset.setPriceToSell(null);
        }

        if (node.has("t") && node.path("t").canConvertToLong()) {
            asset.setLastUpdated(Instant.ofEpochSecond(node.path("t").asLong()));
        } else {
            asset.setLastUpdated(Instant.now());
        }

        assetRepository.save(asset);
    }

    private BigDecimal parseDecimal(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.isEmpty()) return null;
        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            try {
                double d = Double.parseDouble(text);
                return BigDecimal.valueOf(d);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
