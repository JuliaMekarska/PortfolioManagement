package com.example.demo.controller;

import com.example.demo.entities.*;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.PortfolioAssetRepository;
import com.example.demo.service.UserTransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final UserRepository userRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;
    private final UserTransactionService transactionService;

    public PortfolioController(UserRepository userRepository,
                               PortfolioAssetRepository portfolioAssetRepository,
                               UserTransactionService transactionService) {
        this.userRepository = userRepository;
        this.portfolioAssetRepository = portfolioAssetRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/{userId}")
    public Map<String, Object> getPortfolio(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PortfolioAsset> assets = portfolioAssetRepository.findByUser_Id(userId);
        List<UserTransaction> transactions = transactionService.getTransactionsForUser(userId);
        BigDecimal profitLoss = transactionService.calculateProfitLoss(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user.getUsername());
        result.put("balance", user.getBalance());
        result.put("profitLoss", profitLoss);
        result.put("assets", assets);
        result.put("transactions", transactions);
        return result;
    }

    @GetMapping("/{userId}/portfolio-distribution")
    public Map<String, BigDecimal> getPortfolioDistribution(@PathVariable Long userId) {
        List<UserTransaction> transactions = transactionService.getTransactionsForUser(userId);

        Map<String, BigDecimal> holdings = new HashMap<>();
        Map<String, MarketType> assetMarkets = new HashMap<>();

        for (UserTransaction t : transactions) {
            String ticker = t.getAsset().getTicker();
            BigDecimal amount = t.getAmount();
            MarketType market = t.getAsset().getMarketType();
            assetMarkets.put(ticker, market);

            holdings.putIfAbsent(ticker, BigDecimal.ZERO);

            if (t.getType() == TransactionType.BUY) {
                holdings.put(ticker, holdings.get(ticker).add(amount));
            } else if (t.getType() == TransactionType.SELL) {
                holdings.put(ticker, holdings.get(ticker).subtract(amount));
            }
        }

        Map<String, BigDecimal> marketValues = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : holdings.entrySet()) {
            String ticker = entry.getKey();
            BigDecimal amount = entry.getValue();
            BigDecimal price = transactionService.getCurrentPrice(ticker);
            BigDecimal value = amount.multiply(price);

            totalValue = totalValue.add(value);

            String marketName = assetMarkets.get(ticker).getName();
            marketValues.putIfAbsent(marketName, BigDecimal.ZERO);
            marketValues.put(marketName, marketValues.get(marketName).add(value));
        }

        Map<String, BigDecimal> marketPercentages = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : marketValues.entrySet()) {
            BigDecimal percent = entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalValue, 2, RoundingMode.HALF_UP);
            marketPercentages.put(entry.getKey(), percent);
        }

        return marketPercentages;
    }
}
