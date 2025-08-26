package com.example.demo;

import com.example.demo.entities.*;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserTransactionServiceTest {

    private UserRepository userRepository;
    private AssetRepository assetRepository;
    private UserTransactionService transactionService;
    private User user;
    private Asset asset;

    private long idCounter = 1L;
    private Long nextId() {
        return idCounter++;
    }

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        assetRepository = mock(AssetRepository.class);
        transactionService = new UserTransactionService(userRepository, assetRepository);

        user = new User();
        user.setId(1L);
        user.setBalance(BigDecimal.ZERO);

        asset = new Asset();
        asset.setId(1L);
        asset.setTicker("AAPL");
        asset.setClosePrice(new BigDecimal("200"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(assetRepository.findByTicker("AAPL")).thenReturn(Optional.of(asset));
    }

    @Test
    void testAddBuyTransactionAddsAsset() {
        UserTransaction tx = transactionService.addTransaction(
                1L, "AAPL", BigDecimal.ONE, new BigDecimal("150"), TransactionType.BUY, null);

        tx.setId(nextId());

        assertEquals(1, user.getTransactions().size());
        assertEquals(1, user.getPortfolioAssets().size());

        PortfolioAsset pa = user.getPortfolioAssets().get(0);
        assertEquals(new BigDecimal("150"), pa.getPurchasePrice());
        assertEquals(BigDecimal.ONE, pa.getQuantity());
    }

    @Test
    void testAddSellTransactionUpdatesBalance() {
        UserTransaction buyTx = transactionService.addTransaction(
                1L, "AAPL", BigDecimal.TEN, new BigDecimal("100"), TransactionType.BUY, null);
        buyTx.setId(nextId());

        PortfolioAsset pa = user.getPortfolioAssets().get(0);
        pa.setId(10L);

        UserTransaction sellTx = transactionService.addTransaction(
                1L, "AAPL", BigDecimal.TEN, new BigDecimal("120"), TransactionType.SELL, pa.getId());
        sellTx.setId(nextId());

        assertTrue(user.getPortfolioAssets().isEmpty());
        assertEquals(new BigDecimal("1200"), user.getBalance());
    }

    @Test
    void testDeleteBuyTransactionRemovesAsset() {
        UserTransaction buyTx = transactionService.addTransaction(
                1L, "AAPL", BigDecimal.ONE, new BigDecimal("100"), TransactionType.BUY, null);
        buyTx.setId(nextId());

        assertEquals(1, user.getPortfolioAssets().size());

        transactionService.deleteTransaction(1L, buyTx.getId());

        assertTrue(user.getPortfolioAssets().isEmpty());
        assertTrue(user.getTransactions().isEmpty());
    }

    @Test
    void testDeleteSellTransactionUpdatesBalance() {
        UserTransaction buyTx = transactionService.addTransaction(
                1L, "AAPL", BigDecimal.TEN, new BigDecimal("100"), TransactionType.BUY, null);
        buyTx.setId(nextId());

        PortfolioAsset pa = user.getPortfolioAssets().get(0);
        pa.setId(20L);

        UserTransaction sellTx = transactionService.addTransaction(
                1L, "AAPL", BigDecimal.TEN, new BigDecimal("150"), TransactionType.SELL, pa.getId());
        sellTx.setId(nextId());

        assertEquals(new BigDecimal("1500"), user.getBalance());

        transactionService.deleteTransaction(1L, sellTx.getId());

        assertEquals(BigDecimal.ZERO, user.getBalance());

        assertFalse(user.getTransactions().contains(sellTx));
    }

    @Test
    void testCalculateProfitLoss() {
        UserTransaction tx = transactionService.addTransaction(
                1L, "AAPL", BigDecimal.TEN, new BigDecimal("100"), TransactionType.BUY, null);
        tx.setId(nextId());

        BigDecimal profit = transactionService.calculateProfitLoss(1L);

        assertEquals(new BigDecimal("1000.00"), profit);
    }
}
