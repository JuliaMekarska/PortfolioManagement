package com.example.demo.controller;

import com.example.demo.entities.UserTransaction;
import com.example.demo.entities.TransactionType;
import com.example.demo.service.UserTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
public class UserTransactionController {

    private final UserTransactionService transactionService;

    public UserTransactionController(UserTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Controller works!";
    }

    @PostMapping("/buy")
    public UserTransaction buyTransaction(@RequestParam Long userId,
                                          @RequestParam String ticker,
                                          @RequestParam BigDecimal amount,
                                          @RequestParam BigDecimal price) {
        // Kupno zawsze po tickerze, assetId = null
        return transactionService.addTransaction(userId, ticker, amount, price, TransactionType.BUY, null);
    }

    @PostMapping("/sell")
    public UserTransaction sellTransaction(@RequestParam Long userId,
                                           @RequestParam Long assetId,
                                           @RequestParam BigDecimal amount,
                                           @RequestParam BigDecimal price) {
        // Sprzedaż po assetId
        return transactionService.addTransaction(userId, null, amount, price, TransactionType.SELL, assetId);
    }

    @PutMapping("/{transactionId}/edit")
    public UserTransaction editTransaction(
            @RequestParam Long userId,
            @PathVariable Long transactionId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) TransactionType type
    ) {
        return transactionService.updateTransaction(userId, transactionId, amount, price, type);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<String> deleteTransaction(
            @RequestParam Long userId,
            @PathVariable Long transactionId) {

        transactionService.deleteTransaction(userId, transactionId);
        return ResponseEntity.ok("Transaction deleted successfully");
    }
}
