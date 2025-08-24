package com.example.demo.service;

import com.example.demo.entities.TransactionType;
import com.example.demo.entities.UserTransaction;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.UserTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserTransactionService {

    private final UserTransactionRepository transactionRepository;
    private final AssetRepository assetRepository;

    public UserTransactionService(UserTransactionRepository transactionRepository,
                                  AssetRepository assetRepository) {
        this.transactionRepository = transactionRepository;
        this.assetRepository = assetRepository;
    }

    public List<UserTransaction> getTransactionsForUser(Long userId) {
        return transactionRepository.findByUser_Id(userId);
    }

    public BigDecimal calculateProfitLoss(Long userId) {
        List<UserTransaction> transactions = getTransactionsForUser(userId);

        List<UserTransaction> buys = transactions.stream()
                .filter(t -> t.getType() == TransactionType.BUY)
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .toList();

        List<UserTransaction> sells = transactions.stream()
                .filter(t -> t.getType() == TransactionType.SELL)
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .toList();

        BigDecimal profitLoss = BigDecimal.ZERO;

        for (UserTransaction sell : sells) {
            BigDecimal sellAmount = sell.getAmount();
            BigDecimal sellPrice = sell.getPrice();

            for (UserTransaction buy : buys) {
                if (buy.getAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal matchedAmount = buy.getAmount().min(sellAmount);
                profitLoss = profitLoss.add(matchedAmount.multiply(sellPrice.subtract(buy.getPrice())));

                buy.setAmount(buy.getAmount().subtract(matchedAmount));
                sellAmount = sellAmount.subtract(matchedAmount);

                if (sellAmount.compareTo(BigDecimal.ZERO) == 0) break;
            }
        }

        buys.forEach(t -> t.setAmount(
                transactions.stream()
                        .filter(orig -> orig.getId().equals(t.getId()))
                        .findFirst()
                        .get()
                        .getAmount()
        ));

        return profitLoss;
    }

    public BigDecimal getCurrentPrice(String ticker) {
        return assetRepository.findByTicker(ticker)
                .orElseThrow(() -> new RuntimeException("Asset not found"))
                .getPriceToBuy();
    }
}
