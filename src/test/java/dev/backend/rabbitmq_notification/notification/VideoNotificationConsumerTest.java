// 비디오 알림 Consumer의 구독자별 이력 저장을 검증하는 단위 테스트.
package dev.backend.rabbitmq_notification.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.backend.rabbitmq_notification.repository.NotificationRepository;
import dev.backend.rabbitmq_notification.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoNotificationConsumerTest {

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private VideoNotificationConsumer consumer;

	@Test
	void consumeStoresNotificationForEachCurrentSubscriber() {
		VideoCreatedEvent event = new VideoCreatedEvent(
				"event-id",
				10L,
				20L,
				"RabbitMQ 알림",
				LocalDateTime.of(2026, 8, 31, 12, 0)
		);
		when(subscriptionRepository.findSubscriberIdsByCreatorId(20L)).thenReturn(List.of(1L, 2L));

		consumer.consume(event);

		verify(notificationRepository).insertIfAbsent("event-id", 1L, 10L, "RabbitMQ 알림");
		verify(notificationRepository).insertIfAbsent("event-id", 2L, 10L, "RabbitMQ 알림");
	}
}
