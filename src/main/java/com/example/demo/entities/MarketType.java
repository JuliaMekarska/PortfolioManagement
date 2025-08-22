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

    public MarketType(String stock) {
    }

    public MarketType() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    // gettery/settery
}
