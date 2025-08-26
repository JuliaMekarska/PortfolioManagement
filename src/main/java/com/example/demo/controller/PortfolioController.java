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
}