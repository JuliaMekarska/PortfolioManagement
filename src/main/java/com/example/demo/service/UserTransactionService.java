package com.example.demo.service;

import com.example.demo.entities.*;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserTransactionService {

    private final UserRepository userRepository;
    private final AssetRepository assetRepository;

    public UserTransactionService(UserRepository userRepository,
                                  AssetRepository assetRepository) {
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
    }

    public List<UserTransaction> getTransactionsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getTransactions();
    }

    public UserTransaction addTransaction(Long userId, String assetTicker, BigDecimal amount, BigDecimal price, TransactionType type, Long assetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Asset asset = null;
        PortfolioAsset portfolioAsset = null;

        if (type == TransactionType.BUY) {
            asset = assetRepository.findByTicker(assetTicker)
                    .orElseThrow(() -> new RuntimeException("Asset not found"));
        } else if (type == TransactionType.SELL) {
            portfolioAsset = user.getPortfolioAssets().stream()
                    .filter(pa -> pa.getId().equals(assetId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("PortfolioAsset not found"));
            asset = portfolioAsset.getAsset();
        }

        UserTransaction transaction = new UserTransaction();
        transaction.setAsset(asset);
        transaction.setAmount(amount);
        transaction.setPrice(price);
        transaction.setType(type);
        user.addTransaction(transaction);

        if (type == TransactionType.BUY) {
            PortfolioAsset pa = new PortfolioAsset();
            pa.setUser(user);
            pa.setAsset(asset);
            pa.setQuantity(amount);
            pa.setPurchasePrice(price);
            user.addAsset(pa);
        } else if (type == TransactionType.SELL) {
            BigDecimal remaining = amount;

            if (portfolioAsset.getQuantity().compareTo(remaining) <= 0) {
                remaining = remaining.subtract(portfolioAsset.getQuantity());
                user.setBalance(user.getBalance().add(portfolioAsset.getQuantity().multiply(price)));
                user.getPortfolioAssets().remove(portfolioAsset);
            } else {
                portfolioAsset.setQuantity(portfolioAsset.getQuantity().subtract(remaining));
                user.setBalance(user.getBalance().add(remaining.multiply(price)));
            }
        }

        userRepository.save(user);
        return transaction;
    }

    public UserTransaction updateTransaction(Long userId, Long transactionId,
                                             BigDecimal newAmount,
                                             BigDecimal newPrice,
                                             TransactionType newType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserTransaction transaction = user.getTransactions().stream()
                .filter(t -> t.getId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        TransactionType oldType = transaction.getType();
        BigDecimal oldAmount = transaction.getAmount();
        BigDecimal oldPrice = transaction.getPrice();
        Asset asset = transaction.getAsset();

        if (newAmount != null) transaction.setAmount(newAmount);
        if (newPrice != null) transaction.setPrice(newPrice);
        if (newType != null) transaction.setType(newType);

        if (oldType == TransactionType.BUY && transaction.getType() == TransactionType.SELL) {
            PortfolioAsset pa = user.getPortfolioAssets().stream()
                    .filter(p -> p.getAsset().getId().equals(asset.getId()) && p.getQuantity().compareTo(oldAmount) == 0)
                    .findFirst()
                    .orElse(null);

            if (pa != null) {
                user.getPortfolioAssets().remove(pa);
            }

            user.setBalance(user.getBalance().add(transaction.getAmount().multiply(transaction.getPrice())));
        }

        userRepository.save(user);
        return transaction;
    }


    public void deleteTransaction(Long userId, Long transactionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserTransaction transaction = user.getTransactions().stream()
                .filter(t -> t.getId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getType() == TransactionType.BUY) {
            PortfolioAsset pa = user.getPortfolioAssets().stream()
                    .filter(p -> p.getAsset().getId().equals(transaction.getAsset().getId())
                            && p.getQuantity().compareTo(transaction.getAmount()) == 0)
                    .findFirst()
                    .orElse(null);

            if (pa != null) {
                user.getPortfolioAssets().remove(pa);
            }
        } else if (transaction.getType() == TransactionType.SELL) {
            BigDecimal totalSellValue = transaction.getPrice().multiply(transaction.getAmount());
            user.setBalance(user.getBalance().subtract(totalSellValue));
        }

        user.getTransactions().remove(transaction);

        userRepository.save(user);
    }

    public BigDecimal calculateProfitLoss(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal totalProfit = BigDecimal.ZERO;

        for (PortfolioAsset pa : user.getPortfolioAssets()) {
            BigDecimal assetProfit = pa.getAsset().getClosePrice()
                    .subtract(pa.getPurchasePrice())
                    .multiply(pa.getQuantity());

            pa.setProfitAsset(assetProfit.setScale(2, RoundingMode.HALF_UP));

            totalProfit = totalProfit.add(assetProfit);
        }

        return totalProfit.setScale(2, RoundingMode.HALF_UP);
    }


}