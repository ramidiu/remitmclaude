package com.remitm.modules.fx.repository;

import com.remitm.modules.fx.entity.CorridorDeliveryMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorridorDeliveryMethodRepository extends JpaRepository<CorridorDeliveryMethodEntity, Long> {

    List<CorridorDeliveryMethodEntity> findByCorridorIdAndIsActiveTrue(Long corridorId);

    /** Eager-load the corridor so the routing screen can read its currencies without a lazy hit. */
    @Query("SELECT dm FROM CorridorDeliveryMethodEntity dm JOIN FETCH dm.corridor")
    List<CorridorDeliveryMethodEntity> findAllWithCorridor();
}
