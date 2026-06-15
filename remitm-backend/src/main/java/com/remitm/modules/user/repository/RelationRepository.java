package com.remitm.modules.user.repository;

import com.remitm.modules.user.entity.Relation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelationRepository extends JpaRepository<Relation, Long> {

    List<Relation> findByIsActive(Boolean isActive);
}
