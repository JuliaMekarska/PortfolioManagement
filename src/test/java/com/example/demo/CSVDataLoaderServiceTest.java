package com.example.demo;
import com.example.demo.entities.Asset;
import com.example.demo.entities.MarketType;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.MarketTypeRepository;
import com.example.demo.service.CSVDataLoaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.FileWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
public class CSVDataLoaderServiceTest {
    private AssetRepository assetRepository;
    private MarketTypeRepository marketTypeRepository;
    private CSVDataLoaderService service;

    @BeforeEach
    void setUp() {
        assetRepository = mock(AssetRepository.class);
        marketTypeRepository = mock(MarketTypeRepository.class);
        service = new CSVDataLoaderService(assetRepository, marketTypeRepository);
    }

    @Test
    void testToBigDecimal_validNumber() {
        BigDecimal result = service.toBigDecimal("123.45");
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void testToBigDecimal_blankValue() {
        BigDecimal result = service.toBigDecimal("   ");
        assertNull(result);
    }

    @Test
    void testLoadCsvToDatabase_savesAssetCorrectly() throws Exception {
        Path tempCsv = Files.createTempFile("stocks_data", ".csv");
        try (FileWriter writer = new FileWriter(tempCsv.toFile())) {
            writer.write("\"Date\",\"Open\",\"High\",\"Low\",\"Close\",\"Volume\",\"PreviousClose\",\"CompanyName\",\"Symbol\"\n");
            writer.write("\"2025-08-22 00:00:00-04:00\",\"226.16\",\"229.08\",\"225.41\",\"227.75\",\"42445300\",\"224.90\",\"Apple Inc.\",\"AAPL\"\n");
        }

        MarketType stockType = new MarketType("STOCK");
        when(assetRepository.findByTicker("AAPL")).thenReturn(Optional.empty());

        service.loadCsvToDatabase(tempCsv.toString(), stockType);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());

        Asset saved = captor.getValue();
        assertEquals("AAPL", saved.getTicker());
        assertEquals("Apple Inc.", saved.getName());
        assertEquals(new BigDecimal("226.16"), saved.getOpenPrice());
        assertEquals(new BigDecimal("229.08"), saved.getHighPrice());
        assertEquals(new BigDecimal("225.41"), saved.getLowPrice());
        assertEquals(new BigDecimal("227.75"), saved.getClosePrice());
        assertEquals(new BigDecimal("42445300"), saved.getVolume());
        assertEquals(new BigDecimal("224.90"), saved.getPreviousClose());
        assertNotNull(saved.getPriceToBuy());
        assertNotNull(saved.getPriceToSell());
    }

    @Test
    void testLoadCsvToDatabase_skipsMalformedRow() throws Exception {
        Path tempCsv = Files.createTempFile("stocks_data", ".csv");
        try (FileWriter writer = new FileWriter(tempCsv.toFile())) {
            writer.write("Date,Open,High,Low,Close,Volume,PreviousClose,CompanyName,Symbol\n");
            writer.write("MALFORMED_ROW_WITH_TOO_FEW_FIELDS\n");
        }

        MarketType stockType = new MarketType("STOCK");

        service.loadCsvToDatabase(tempCsv.toString(), stockType);

        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void testEnsureType_createsNewTypeIfNotFound() {
        when(marketTypeRepository.findByName("STOCK")).thenReturn(Optional.empty());
        when(marketTypeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MarketType type = service.ensureType("STOCK");

        assertEquals("STOCK", type.getName());
        verify(marketTypeRepository).save(any(MarketType.class));
    }

    @Test
    void testEnsureType_returnsExistingType() {
        MarketType existing = new MarketType("STOCK");
        when(marketTypeRepository.findByName("STOCK")).thenReturn(Optional.of(existing));

        MarketType type = service.ensureType("STOCK");

        assertSame(existing, type);
        verify(marketTypeRepository, never()).save(any(MarketType.class));
    }
}
