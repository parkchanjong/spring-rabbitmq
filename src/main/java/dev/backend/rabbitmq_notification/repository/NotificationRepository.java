// 수신자별 알림 이력의 멱등 저장을 담당하는 Repository.
package dev.backend.rabbitmq_notification.repository;

import dev.backend.rabbitmq_notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Modifying
	@Query(value = """
			INSERT IGNORE INTO notifications (event_id, recipient_id, video_id, video_title, created_at)
			VALUES (:eventId, :recipientId, :videoId, :videoTitle, CURRENT_TIMESTAMP)
			""", nativeQuery = true)
	int insertIfAbsent(
			@Param("eventId") String eventId,
			@Param("recipientId") Long recipientId,
			@Param("videoId") Long videoId,
			@Param("videoTitle") String videoTitle
	);

	long countByEventId(String eventId);
}
