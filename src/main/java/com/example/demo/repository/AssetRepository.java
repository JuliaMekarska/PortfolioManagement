package com.example.demo.repository;

import com.example.demo.entities.Asset;
import com.example.demo.entities.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByTicker(String ticker);
    List<Asset> findByMarketType(MarketType marketType);

}
