package com.example.demo.entities;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class MarketType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // np. Crypto, Stock

    @OneToMany(mappedBy = "marketType", cascade = CascadeType.ALL)
    private List<Asset> assets;

    // gettery/settery
}
