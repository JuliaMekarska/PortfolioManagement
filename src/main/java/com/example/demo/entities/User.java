package com.example.demo.entities;

import jakarta.persistence.*;
import jakarta.transaction.Transaction;

import java.util.List;

@Entity
@Table(name = "users") // "user" to reserved word w SQL
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PortfolioAsset> portfolioAssets;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserTransaction> transactions;

    // gettery/settery
}
