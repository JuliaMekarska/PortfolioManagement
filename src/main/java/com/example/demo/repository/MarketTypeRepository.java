package com.example.demo.repository;

import com.example.demo.entities.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MarketTypeRepository extends JpaRepository<MarketType, Long> {
    Optional<MarketType> findByName(String name);
}
