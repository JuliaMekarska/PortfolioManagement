package com.example.demo.service;

import com.example.demo.entities.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PortfolioService {

    private final UserRepository userRepo;
    private final UserTransactionRepository txRepo;
    private final PortfolioAssetRepository portfolioRepo;
    private final AssetRepository assetRepo;

    public PortfolioService(UserRepository userRepo, UserTransactionRepository txRepo,
                            PortfolioAssetRepository portfolioRepo, AssetRepository assetRepo) {
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        this.portfolioRepo = portfolioRepo;
        this.assetRepo = assetRepo;
    }

    public BigDecimal getBalance(Long userId) {
        return userRepo.findById(userId).orElseThrow().getBalance();
    }

    public void deposit(Long userId, BigDecimal amount) {
        User user = userRepo.findById(userId).orElseThrow();
        user.setBalance(user.getBalance().add(amount));

        UserTransaction tx = new UserTransaction();
        tx.setUser(user);
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(amount);
        tx.setTimestamp(Instant.now());
        txRepo.save(tx);
        userRepo.save(user);
    }

    // 🔹 wypłata
    public void withdraw(Long userId, BigDecimal amount) {
        User user = userRepo.findById(userId).orElseThrow();
        if (user.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Not enough funds");
        }
        user.setBalance(user.getBalance().subtract(amount));

        UserTransaction tx = new UserTransaction();
        tx.setUser(user);
        tx.setType(TransactionType.WITHDRAW);
        tx.setAmount(amount);
        tx.setTimestamp(Instant.now());
        txRepo.save(tx);
        userRepo.save(user);
    }

    // 🔹 historia transakcji
    public List<UserTransaction> getTransactions(Long userId) {
        return userRepo.findById(userId).orElseThrow().getTransactions();
    }

    // 🔹 lista assetów w portfelu
    public List<PortfolioAsset> getPortfolio(Long userId) {
        return userRepo.findById(userId).orElseThrow().getPortfolioAssets();
    }

    // 🔹 wyliczanie zysku/straty: (currentPrice - averageBuyPrice) * quantity
    public BigDecimal calculateProfitLoss(Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        BigDecimal totalPnL = BigDecimal.ZERO;

        for (PortfolioAsset pa : user.getPortfolioAssets()) {
            Asset asset = pa.getAsset();
            BigDecimal currentPrice = asset.getClosePrice();
            BigDecimal avgBuyPrice = BigDecimal.valueOf(100); // 👈 na razie przykładowa logika (potem możemy liczyć średnią z BUY transakcji)
            BigDecimal diff = currentPrice.subtract(avgBuyPrice);
            totalPnL = totalPnL.add(diff.multiply(pa.getQuantity()));
        }
        return totalPnL;
    }
}
