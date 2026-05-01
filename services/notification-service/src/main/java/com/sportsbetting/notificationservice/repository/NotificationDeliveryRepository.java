package com.sportsbetting.notificationservice.repository;

import com.sportsbetting.notificationservice.model.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, String> {
}
