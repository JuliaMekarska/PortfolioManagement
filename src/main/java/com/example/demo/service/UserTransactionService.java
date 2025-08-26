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
            user.addAsset(pa);
        } else if (type == TransactionType.SELL) {
            BigDecimal remaining = amount;

            if (portfolioAsset.getQuantity().compareTo(remaining) <= 0) {
                remaining = remaining.subtract(portfolioAsset.getQuantity());
                // Dodajemy do balance wartość sprzedanych akcji
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

        // aktualizujemy dane transakcji
        if (newAmount != null) transaction.setAmount(newAmount);
        if (newPrice != null) transaction.setPrice(newPrice);
        if (newType != null) transaction.setType(newType);

        // przeliczamy portfel po zmianie transakcji
        recalculatePortfolio(user);

        userRepository.save(user);
        return transaction;
    }

    /**
     * Przelicza portfel użytkownika na podstawie wszystkich transakcji (każdy zakup = nowy asset)
     */
    private void recalculatePortfolio(User user) {
        user.getPortfolioAssets().clear();

        for (UserTransaction t : user.getTransactions()) {
            if (t.getType() == TransactionType.BUY) {
                PortfolioAsset pa = new PortfolioAsset();
                pa.setUser(user);
                pa.setAsset(t.getAsset());
                pa.setQuantity(t.getAmount());
                user.addAsset(pa);
            } else if (t.getType() == TransactionType.SELL) {
                BigDecimal remaining = t.getAmount();

                // Pobieramy wszystkie portfolioAsset dla danego tickera
                List<PortfolioAsset> assets = user.getPortfolioAssets().stream()
                        .filter(pa -> pa.getAsset().getTicker().equals(t.getAsset().getTicker()))
                        .toList();

                for (PortfolioAsset pa : assets) {
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                    BigDecimal qty = pa.getQuantity();
                    if (qty.compareTo(remaining) <= 0) {
                        // Odejmujemy całość i oznaczamy do usunięcia
                        remaining = remaining.subtract(qty);
                        pa.setQuantity(BigDecimal.ZERO);
                    } else {
                        // Odejmujemy tylko część
                        pa.setQuantity(qty.subtract(remaining));
                        remaining = BigDecimal.ZERO;
                    }
                }

                // Usuwamy wszystkie assety z quantity == 0
                user.getPortfolioAssets().removeIf(pa -> pa.getQuantity().compareTo(BigDecimal.ZERO) == 0);
            }
        }
    }

    public void deleteTransaction(Long userId, Long transactionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserTransaction transaction = user.getTransactions().stream()
                .filter(t -> t.getId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Usuwamy asset z portfela jeśli istnieje i dotyczy tej transakcji
        if (transaction.getType() == TransactionType.BUY) {
            // Szukamy portfolioAsset odpowiadającego tej transakcji (po assetId i ilości)
            PortfolioAsset pa = user.getPortfolioAssets().stream()
                    .filter(p -> p.getAsset().getId().equals(transaction.getAsset().getId())
                            && p.getQuantity().compareTo(transaction.getAmount()) == 0)
                    .findFirst()
                    .orElse(null);

            if (pa != null) {
                user.getPortfolioAssets().remove(pa);
            }
        } else if (transaction.getType() == TransactionType.SELL) {
            // W przypadku SELL zazwyczaj portfel już nie ma assetu, nic do usunięcia
        }

        // Usuwamy transakcję
        user.getTransactions().remove(transaction);

        userRepository.save(user);
    }

    public BigDecimal calculateProfitLoss(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal totalPnL = BigDecimal.ZERO;

        for (UserTransaction t : user.getTransactions()) {
            if (t.getType() == TransactionType.BUY) { // tylko BUY liczymy do PnL
                BigDecimal pnl = t.getAsset().getClosePrice().subtract(t.getPrice())
                        .multiply(t.getAmount());
                totalPnL = totalPnL.add(pnl);
            }
        }

        return totalPnL.setScale(2, RoundingMode.HALF_UP);
    }}