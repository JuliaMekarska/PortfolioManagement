package com.example.demo.entities;

import jakarta.persistence.*;
import jakarta.transaction.Transaction;

import java.util.List;

@Entity
@Table(name = "users") // "user" to reserved word w SQL
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String username;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    public List<PortfolioAsset> portfolioAssets;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    public List<UserTransaction> transactions;

    public User(String username) {
        this.username = username;
    }

    // gettery/settery
}
