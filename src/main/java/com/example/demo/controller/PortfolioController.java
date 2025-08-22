package com.example.demo.controller;

import com.example.demo.entities.PortfolioAsset;
import com.example.demo.entities.UserTransaction;
import com.example.demo.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping("/{userId}/balance")
    public BigDecimal getBalance(@PathVariable Long userId) {
        return service.getBalance(userId);
    }

    @PostMapping("/{userId}/deposit")
    public void deposit(@PathVariable Long userId, @RequestParam BigDecimal amount) {
        service.deposit(userId, amount);
    }

    @PostMapping("/{userId}/withdraw")
    public void withdraw(@PathVariable Long userId, @RequestParam BigDecimal amount) {
        service.withdraw(userId, amount);
    }

    @GetMapping("/{userId}/transactions")
    public List<UserTransaction> getTransactions(@PathVariable Long userId) {
        return service.getTransactions(userId);
    }

    @GetMapping("/{userId}/assets")
    public List<PortfolioAsset> getPortfolio(@PathVariable Long userId) {
        return service.getPortfolio(userId);
    }

    @GetMapping("/{userId}/profit-loss")
    public BigDecimal getProfitLoss(@PathVariable Long userId) {
        return service.calculateProfitLoss(userId);
    }
}
