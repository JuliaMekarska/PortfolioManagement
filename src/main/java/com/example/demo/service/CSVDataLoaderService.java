package com.example.demo.service;

import com.example.demo.entities.Asset;
import com.example.demo.entities.MarketType;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.MarketTypeRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

@Service
public class CSVDataLoaderService {

    private final AssetRepository assetRepository;
    private final MarketTypeRepository marketTypeRepository;

    private MarketType stockType;
    private MarketType cryptoType;
    private MarketType etfType;
    private MarketType commodityType;

    public CSVDataLoaderService(AssetRepository assetRepository, MarketTypeRepository marketTypeRepository) {
        this.assetRepository = assetRepository;
        this.marketTypeRepository = marketTypeRepository;
    }
    @PersistenceContext
    private EntityManager entityManager;

    @PostConstruct
    public void init() {
        try {
/*
            File pythonFile = new File("src/main/python/output/fetch_data.py");
            if (!pythonFile.exists()) {
                System.err.println("Python script not found: " + pythonFile.getAbsolutePath());
            } else {
                ProcessBuilder pb = new ProcessBuilder("python", pythonFile.getAbsolutePath());
                pb.inheritIO();
                Process process = pb.start();
                process.waitFor();
            }
            File pythonFile1 = new File("src/main/python/output/historic_data.py");
            if (!pythonFile.exists()) {
                System.err.println("Python script not found: " + pythonFile1.getAbsolutePath());
            } else {
                ProcessBuilder pb = new ProcessBuilder("python", pythonFile1.getAbsolutePath());
                pb.inheritIO();
                Process process = pb.start();
                process.waitFor(); // wait until CSVs are written
            }

             */

            stockType = ensureType("STOCK");
            cryptoType = ensureType("CRYPTO");
            etfType = ensureType("ETF");
            commodityType = ensureType("COMMODITY");

            loadCsvToDatabase("src/main/resources/data/stocks_data.csv", stockType);
            loadCsvToDatabase("src/main/resources/data/crypto_data.csv", cryptoType);
            loadCsvToDatabase("src/main/resources/data/etf_data.csv", etfType);
            loadCsvToDatabase("src/main/resources/data/commodities_data.csv", commodityType);

        } catch (IOException | CsvValidationException e) {
            System.err.println("Error running Python script or loading CSVs: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public MarketType ensureType(String name) {
        return marketTypeRepository.findByName(name)
                .orElseGet(() -> marketTypeRepository.save(new MarketType(name)));
    }

    public void loadCsvToDatabase(String csvFilePath, MarketType type) throws IOException, CsvValidationException {
        File file = new File(csvFilePath);
        if (!file.exists()) {
            System.err.println("CSV file not found: " + file.getAbsolutePath());
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {

                String[] fields = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (fields.length < 9) continue;

                String date = fields[0].replace("\"", "").trim();
                String open = fields[1].replace("\"", "").trim();
                String high = fields[2].replace("\"", "").trim();
                String low = fields[3].replace("\"", "").trim();
                String close = fields[4].replace("\"", "").trim();
                String volume = fields[5].replace("\"", "").trim();
                String prevClose = fields[6].replace("\"", "").trim();
                String companyName = fields[7].replace("\"", "").trim();
                String ticker = fields[8].replace("\"", "").trim();

                Asset asset = assetRepository.findByTicker(ticker)
                        .orElseGet(Asset::new);

                asset.setTicker(ticker);
                asset.setName(companyName);
                asset.setExchange("");
                asset.setOpenPrice(toBigDecimal(open));
                asset.setHighPrice(toBigDecimal(high));
                asset.setLowPrice(toBigDecimal(low));
                asset.setClosePrice(toBigDecimal(close));
                asset.setVolume(toBigDecimal(volume));
                asset.setPreviousClose(toBigDecimal(prevClose));
                asset.setPercentChange(null);
                asset.setLastUpdated(Instant.now());
                asset.setMarketType(type);

                if (asset.getClosePrice() != null) {
                    asset.setPriceToBuy(asset.getClosePrice().multiply(BigDecimal.valueOf(1.01)));
                    asset.setPriceToSell(asset.getClosePrice().multiply(BigDecimal.valueOf(0.99)));
                }

                assetRepository.save(asset);
            }
        }
    }

    public BigDecimal toBigDecimal(String value) {
        try {
            return (value != null && !value.isBlank()) ? new BigDecimal(value) : null;
        } catch (Exception e) {
            return null;
        }
    }
}