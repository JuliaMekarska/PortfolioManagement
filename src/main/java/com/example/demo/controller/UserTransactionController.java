package com.example.demo.controller;

import com.example.demo.entities.Asset;
import com.example.demo.entities.PortfolioAsset;
import com.example.demo.entities.User;
import com.example.demo.entities.UserTransaction;
import com.example.demo.entities.TransactionType;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.PortfolioAssetRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserTransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class UserTransactionController {

    private final UserTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;

    public UserTransactionController(UserTransactionRepository transactionRepository,
                                     UserRepository userRepository,
                                     AssetRepository assetRepository,
                                     PortfolioAssetRepository portfolioAssetRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.portfolioAssetRepository = portfolioAssetRepository;
    }

    @PostMapping("/buy")
    public UserTransaction buyAsset(@RequestParam Long userId,
                                    @RequestParam Long assetId,
                                    @RequestParam BigDecimal amount,
                                    @RequestParam BigDecimal price) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        UserTransaction transaction = new UserTransaction();
        transaction.setUser(user);
        transaction.setAsset(asset);
        transaction.setType(TransactionType.BUY);
        transaction.setAmount(amount);
        transaction.setPrice(price);
        transactionRepository.save(transaction);

        PortfolioAsset portfolioAsset = portfolioAssetRepository.findByUser_Id(userId).stream()
                .filter(pa -> pa.getAsset().getId().equals(assetId))
                .findFirst()
                .orElseGet(() -> {
                    PortfolioAsset pa = new PortfolioAsset();
                    pa.setUser(user);
                    pa.setAsset(asset);
                    pa.setQuantity(BigDecimal.ZERO);
                    return pa;
                });

        portfolioAsset.setQuantity(portfolioAsset.getQuantity().add(amount));
        portfolioAssetRepository.save(portfolioAsset);

        return transaction;
    }

    @PostMapping("/sell")
    public UserTransaction sellAsset(@RequestParam Long userId,
                                     @RequestParam Long assetId,
                                     @RequestParam BigDecimal amount,
                                     @RequestParam BigDecimal price) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        PortfolioAsset portfolioAsset = portfolioAssetRepository.findByUser_Id(userId).stream()
                .filter(pa -> pa.getAsset().getId().equals(assetId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Not enough assets to sell"));

        if (portfolioAsset.getQuantity().compareTo(amount) < 0) {
            throw new RuntimeException("Not enough assets to sell");
        }

        UserTransaction transaction = new UserTransaction();
        transaction.setUser(user);
        transaction.setAsset(asset);
        transaction.setType(TransactionType.SELL);
        transaction.setAmount(amount);
        transaction.setPrice(price);
        transactionRepository.save(transaction);

        portfolioAsset.setQuantity(portfolioAsset.getQuantity().subtract(amount));
        if (portfolioAsset.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            portfolioAssetRepository.delete(portfolioAsset);
        } else {
            portfolioAssetRepository.save(portfolioAsset);
        }

        return transaction;
    }

    @GetMapping("/history")
    public List<UserTransaction> getTransactionHistory(@RequestParam Long userId) {
        return transactionRepository.findByUser_Id(userId);
    }
}
