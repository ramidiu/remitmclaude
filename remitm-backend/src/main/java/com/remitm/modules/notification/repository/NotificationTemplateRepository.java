package com.remitm.modules.notification.repository;

import com.remitm.common.enums.NotificationChannel;
import com.remitm.modules.notification.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

    Optional<NotificationTemplateEntity> findByTemplateCodeAndChannelAndLanguageAndIsActiveTrue(
            String templateCode, NotificationChannel channel, String language);

    Optional<NotificationTemplateEntity> findByTemplateCodeAndChannelAndIsActiveTrue(
            String templateCode, NotificationChannel channel);

    List<NotificationTemplateEntity> findAllByIsActiveTrue();

    Optional<NotificationTemplateEntity> findFirstByTemplateCodeOrderByIdAsc(String templateCode);
}
