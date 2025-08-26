package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import jakarta.transaction.Transaction;

import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private BigDecimal balance = BigDecimal.ZERO;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id") // foreign key w UserTransaction
    private List<UserTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PortfolioAsset> assets = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public List<UserTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<UserTransaction> transactions) { this.transactions = transactions; }

    public void addTransaction(UserTransaction transaction) {
        this.transactions.add(transaction);
    }

    public List<PortfolioAsset>  getPortfolioAssets() { return assets; }
    public void setAssets(List<PortfolioAsset> assets) { this.assets = assets; }

    public void addAsset(PortfolioAsset asset) {
        this.assets.add(asset);
    }

}
