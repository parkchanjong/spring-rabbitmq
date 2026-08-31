// 비디오 생성 이벤트를 수신해 구독자별 알림 이력을 저장한다.
package dev.backend.rabbitmq_notification.notification;

import dev.backend.rabbitmq_notification.repository.NotificationRepository;
import dev.backend.rabbitmq_notification.repository.SubscriptionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoNotificationConsumer {

	private final SubscriptionRepository subscriptionRepository;
	private final NotificationRepository notificationRepository;

	public VideoNotificationConsumer(
			SubscriptionRepository subscriptionRepository,
			NotificationRepository notificationRepository
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.notificationRepository = notificationRepository;
	}

	@RabbitListener(
			queues = NotificationRabbitConfig.VIDEO_NOTIFICATION_QUEUE,
			containerFactory = "notificationRabbitListenerContainerFactory"
	)
	@Transactional
	public void consume(VideoCreatedEvent event) {
		subscriptionRepository.findSubscriberIdsByCreatorId(event.creatorId())
				.forEach(subscriberId -> notificationRepository.insertIfAbsent(
						event.eventId(),
						subscriberId,
						event.videoId(),
						event.videoTitle()
				));
	}
}
