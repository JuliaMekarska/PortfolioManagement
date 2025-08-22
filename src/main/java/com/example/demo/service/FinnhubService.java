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

    // how many assets per market (kept for compatibility; we use predefined lists)
    @Value("${app.assets.per.market:20}")
    private int assetsPerMarket;

    @Value("${app.rest.sleep.ms:200}")
    private long restSleepMs;

    // --- Predefined top 20 stocks and top 20 cryptos (BINANCE: pair format)
    private static final List<String> TOP_STOCKS = Arrays.asList(
            "AAPL","MSFT","AMZN","GOOGL","TSLA","META","NVDA","JPM","V","MA",
            "DIS","NFLX","KO","PFE","BAC","ORCL","INTC","CSCO","XOM","CVX"
    );

    private static final List<String> TOP_CRYPTOS = Arrays.asList(
            "BINANCE:BTCUSDT","BINANCE:ETHUSDT","BINANCE:BNBUSDT","BINANCE:XRPUSDT","BINANCE:ADAUSDT",
            "BINANCE:SOLUSDT","BINANCE:DOGEUSDT","BINANCE:DOTUSDT","BINANCE:LTCUSDT","BINANCE:MATICUSDT",
            "BINANCE:SHIBUSDT","BINANCE:TRXUSDT","BINANCE:AVAXUSDT","BINANCE:LINKUSDT","BINANCE:UNIUSDT",
            "BINANCE:BCHUSDT","BINANCE:ETCUSDT","BINANCE:FILUSDT","BINANCE:AAVEUSDT","BINANCE:LUNCUSDT"
    );

    public FinnhubService(AssetRepository assetRepository, MarketTypeRepository marketTypeRepository) {
        this.assetRepository = assetRepository;
        this.marketTypeRepository = marketTypeRepository;
    }

    @PostConstruct
    public void init() {
        // Ensure MarketType rows exist
        MarketType stockType = marketTypeRepository.findByName("STOCK")
                .orElseGet(() -> marketTypeRepository.save(new MarketType("STOCK")));
        MarketType cryptoType = marketTypeRepository.findByName("CRYPTO")
                .orElseGet(() -> marketTypeRepository.save(new MarketType("CRYPTO")));

        // Seed the predefined lists
        try {
            seedTopStocks(stockType);
            seedTopCryptos(cryptoType);
        } catch (Exception e) {
            log.error("Error during initial seeding", e);
        }
    }

    // Seed using predefined TOP_STOCKS and fetch company name via /stock/profile2
    private void seedTopStocks(MarketType marketType) {
        int count = 0;
        for (String sym : TOP_STOCKS) {
            if (count >= assetsPerMarket) break; // honor config if smaller
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

    // Get company name via /stock/profile2?symbol={symbol}
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

    // Seed crypto list, but verify that candles or quote exist first
    // --- new seedTopCryptos implementation (uses /crypto/symbol to get canonical symbols)
    private void seedTopCryptos(MarketType marketType) {
        final String exchange = "BINANCE"; // you can change to COINBASE, KRAKEN, etc.
        try {
            String url = "https://finnhub.io/api/v1/crypto/symbol?exchange=" +
                    URLEncoder.encode(exchange, StandardCharsets.UTF_8) +
                    "&token=" + apiKey;

            String resp = rest.getForObject(url, String.class);
            if (resp == null || resp.isEmpty()) {
                log.warn("Empty /crypto/symbol response for exchange {}", exchange);
                return;
            }

            JsonNode arr = mapper.readTree(resp);
            if (!arr.isArray()) {
                log.warn("/crypto/symbol response not an array for exchange {}: {}", exchange, resp);
                return;
            }

            int count = 0;
            for (JsonNode node : arr) {
                if (count >= assetsPerMarket) break;

                // Finnhub crypto symbol objects typically contain: symbol, displaySymbol, description
                String symbol = node.path("symbol").asText(null);           // use this when calling /crypto/candle
                String display = node.path("displaySymbol").asText(null);  // nicer name for UI
                String desc = node.path("description").asText(null);

                if (symbol == null || symbol.isEmpty()) continue;

                // verify candle data exists (quick check)
                boolean ok = cryptoHasData(symbol);
                if (!ok) {
                    log.debug("Skipping crypto {} (no recent candles)", symbol);
                    continue;
                }

                String humanName = (display != null && !display.isBlank()) ? display : (desc != null ? desc : symbol);
                saveOrUpdateAsset(symbol, humanName, extractExchange(symbol), marketType);
                count++;
                Thread.sleep(restSleepMs);
            }
            log.info("Seeded {} cryptos (exchange={})", count, exchange);

        } catch (Exception e) {
            log.error("Error seeding cryptos", e);
        }
    }


    // check crypto candle availability for the last 5 minutes
    // Check candle availability for a symbol returned by /crypto/symbol
    private boolean cryptoHasData(String symbol) {
        try {
            long now = Instant.now().getEpochSecond();
            long from = now - 300; // last 5 minutes
            String url = "https://finnhub.io/api/v1/crypto/candle?symbol=" +
                    URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
                    "&resolution=1&from=" + from + "&to=" + now + "&token=" + apiKey;

            String resp = rest.getForObject(url, String.class);
            if (resp == null || resp.isEmpty()) return false;

            JsonNode node = mapper.readTree(resp);
            String status = node.path("s").asText(null);
            return "ok".equalsIgnoreCase(status) && node.has("t") && node.path("t").size() > 0;
        } catch (Exception e) {
            log.debug("cryptoHasData failed for {}: {}", symbol, e.getMessage());
            return false;
        }
    }


    private String extractExchange(String ticker) {
        if (ticker == null) return "UNKNOWN";
        int idx = ticker.indexOf(':');
        if (idx > 0) return ticker.substring(0, idx);
        return "UNKNOWN";
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
                if (asset.getMarketType() != null && "CRYPTO".equalsIgnoreCase(asset.getMarketType().getName())) {
                    updateSingleCryptoQuote(asset);
                } else {
                    updateSingleStockQuote(asset);
                }
                Thread.sleep(restSleepMs); // avoid burst
            } catch (Exception e) {
                log.warn("Failed to update {}: {}", asset.getTicker(), e.getMessage());
            }
        }
        log.info("Quotes update finished at {}", Instant.now());
    }

    // Update stocks using /quote
    private void updateSingleStockQuote(Asset asset) throws Exception {
        String symbolEncoded = URLEncoder.encode(asset.getTicker(), StandardCharsets.UTF_8);
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbolEncoded + "&token=" + apiKey;
        String response = rest.getForObject(url, String.class);
        if (response == null || response.isEmpty()) {
            log.debug("Empty quote response for {}", asset.getTicker());
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

    // Update crypto using /crypto/candle for latest candle; fallback to /quote
    private void updateSingleCryptoQuote(Asset asset) throws Exception {
        long now = Instant.now().getEpochSecond();
        long from = now - 300; // last 5 minutes
        String symbolEncoded = URLEncoder.encode(asset.getTicker(), StandardCharsets.UTF_8);
        String url = "https://finnhub.io/api/v1/crypto/candle?symbol=" + symbolEncoded +
                "&resolution=1&from=" + from + "&to=" + now + "&token=" + apiKey;
        String response = rest.getForObject(url, String.class);
        if (response == null || response.isEmpty()) {
            log.debug("Empty crypto candle response for {}", asset.getTicker());
            updateSingleStockQuote(asset); // fallback to quote
            return;
        }
        JsonNode node = mapper.readTree(response);
        String status = node.path("s").asText(null);
        if ("ok".equalsIgnoreCase(status) && node.has("t") && node.path("t").size() > 0) {
            JsonNode ts = node.path("t");
            JsonNode opens = node.path("o");
            JsonNode highs = node.path("h");
            JsonNode lows = node.path("l");
            JsonNode closes = node.path("c");
            JsonNode volumes = node.path("v");
            int idx = ts.size() - 1; // latest candle
            long tsSec = ts.get(idx).asLong();
            BigDecimal o = parseDecimal(opens.get(idx).asText());
            BigDecimal h = parseDecimal(highs.get(idx).asText());
            BigDecimal l = parseDecimal(lows.get(idx).asText());
            BigDecimal c = parseDecimal(closes.get(idx).asText());
            BigDecimal v = parseDecimal(volumes.get(idx).asText());

            asset.setOpenPrice(o);
            asset.setHighPrice(h);
            asset.setLowPrice(l);
            asset.setClosePrice(c);
            asset.setVolume(v);

            // no reliable previous_close in candle response; compute percentChange if you have prevClose stored
            BigDecimal prev = asset.getPreviousClose();
            if (prev != null && prev.compareTo(BigDecimal.ZERO) != 0 && c != null) {
                BigDecimal pct = c.subtract(prev).divide(prev, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP);
                asset.setPercentChange(pct);
            } else {
                asset.setPercentChange(null);
            }

            if (c != null) {
                asset.setPriceToBuy(c.multiply(BigDecimal.valueOf(1.001)).setScale(6, RoundingMode.HALF_UP));
                asset.setPriceToSell(c.multiply(BigDecimal.valueOf(0.999)).setScale(6, RoundingMode.HALF_UP));
            } else {
                asset.setPriceToBuy(null);
                asset.setPriceToSell(null);
            }

            asset.setLastUpdated(Instant.ofEpochSecond(tsSec));
            assetRepository.save(asset);
            log.debug("Crypto candle updated for {} @ {}", asset.getTicker(), Instant.ofEpochSecond(tsSec));
            return;
        }

        // fallback if candles not available
        log.debug("Crypto candles not available for {}, falling back to /quote", asset.getTicker());
        updateSingleStockQuote(asset);
    }

    // Safe BigDecimal parser - returns null on bad input
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