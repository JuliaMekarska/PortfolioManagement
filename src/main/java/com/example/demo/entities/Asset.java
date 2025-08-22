package com.example.demo.entities;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String ticker;

    @ManyToOne
    @JoinColumn(name = "market_type_id")
    private MarketType marketType;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL)
    private List<PortfolioAsset> portfolioAssets;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL)
    private List<Transaction> transactions;

    // gettery/settery
}
