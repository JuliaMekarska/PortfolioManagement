package com.example.demo.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private BigDecimal balance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserTransaction> transactions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PortfolioAsset> portfolioAssets;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public List<UserTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<UserTransaction> transactions) { this.transactions = transactions; }

    public List<PortfolioAsset> getPortfolioAssets() { return portfolioAssets; }
    public void setPortfolioAssets(List<PortfolioAsset> portfolioAssets) { this.portfolioAssets = portfolioAssets; }
}
