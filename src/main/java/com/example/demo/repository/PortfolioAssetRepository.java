package com.example.demo.repository;

import com.example.demo.entities.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    List<PortfolioAsset> findByUser_Id(Long userId);
}