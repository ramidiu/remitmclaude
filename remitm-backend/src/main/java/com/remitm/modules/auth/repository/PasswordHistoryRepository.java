package com.remitm.modules.auth.repository;

import com.remitm.modules.auth.entity.PasswordHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, Long> {

    List<PasswordHistoryEntity> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<PasswordHistoryEntity> findByUserId(Long userId);
}
