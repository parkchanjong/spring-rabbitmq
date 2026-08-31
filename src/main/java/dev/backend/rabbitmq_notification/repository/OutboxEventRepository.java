// 미발행 알림 이벤트의 저장과 조회를 담당하는 Repository.
package dev.backend.rabbitmq_notification.repository;

import dev.backend.rabbitmq_notification.domain.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();
}
