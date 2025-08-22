package com.example.demo.repositories;

import com.example.demo.entities.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    // portfel konkretnego usera
    List<PortfolioAsset> findByUser_Id(Long userId);
}
