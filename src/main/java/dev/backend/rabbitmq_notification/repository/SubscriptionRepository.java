// 구독 관계 엔티티의 영속화를 담당하는 Repository.
package dev.backend.rabbitmq_notification.repository;

import dev.backend.rabbitmq_notification.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
}
