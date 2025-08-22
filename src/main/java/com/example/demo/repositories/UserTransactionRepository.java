package com.example.demo.repositories;

import com.example.demo.entities.UserTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {
    List<UserTransaction> findByUser_Id(Long userId);

    List<UserTransaction> findByUser_IdAndAsset_Id(Long userId, Long assetId);
}
