package com.remitm.modules.fx.repository;

import com.remitm.common.enums.DeliveryMethod;
import com.remitm.modules.fx.entity.CorridorFeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorridorFeeRepository extends JpaRepository<CorridorFeeEntity, Long> {

    Optional<CorridorFeeEntity> findByCorridorIdAndDeliveryMethodAndIsActiveTrue(
            Long corridorId, DeliveryMethod deliveryMethod);

    List<CorridorFeeEntity> findByCorridorId(Long corridorId);
}
