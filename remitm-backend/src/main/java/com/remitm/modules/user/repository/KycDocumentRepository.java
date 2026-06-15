package com.remitm.modules.user.repository;

import com.remitm.common.enums.KycDocumentStatus;
import com.remitm.modules.user.entity.KycDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocumentEntity, Long> {

    List<KycDocumentEntity> findByUserId(Long userId);

    List<KycDocumentEntity> findByUserIdAndStatus(Long userId, KycDocumentStatus status);

    long countByUserId(Long userId);
}
