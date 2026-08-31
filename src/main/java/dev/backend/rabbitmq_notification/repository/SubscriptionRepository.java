// 구독 관계 엔티티의 영속화를 담당하는 Repository.
package dev.backend.rabbitmq_notification.repository;

import dev.backend.rabbitmq_notification.domain.Subscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);

	long countBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);

	@Query("select subscription.subscriber.id from Subscription subscription where subscription.creator.id = :creatorId")
	List<Long> findSubscriberIdsByCreatorId(Long creatorId);
}
