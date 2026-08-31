// 비디오 생성 후 구독자 알림에 사용하는 RabbitMQ 이벤트.
package dev.backend.rabbitmq_notification.notification;

import java.time.LocalDateTime;

public record VideoCreatedEvent(
		String eventId,
		Long videoId,
		Long creatorId,
		String videoTitle,
		LocalDateTime occurredAt
) {
}
