// 비디오 생성 이벤트를 수신해 구독자별 알림 이력을 저장한다.
package dev.backend.rabbitmq_notification.notification;

import dev.backend.rabbitmq_notification.repository.NotificationRepository;
import dev.backend.rabbitmq_notification.repository.SubscriptionRepository;
import java.util.List;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VideoNotificationConsumer {

	private static final Logger log = LoggerFactory.getLogger(VideoNotificationConsumer.class);

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
		try {
			log.debug(
					"비디오 생성 이벤트를 수신했습니다. eventId={}, videoId={}, creatorId={}",
					event.eventId(),
					event.videoId(),
					event.creatorId()
			);
			List<Long> subscriberIds = subscriptionRepository.findSubscriberIdsByCreatorId(event.creatorId());
			int createdCount = 0;
			for (Long subscriberId : subscriberIds) {
				createdCount += notificationRepository.insertIfAbsent(
						event.eventId(),
						subscriberId,
						event.videoId(),
						event.videoTitle()
				);
			}
			log.debug(
					"비디오 알림 저장을 완료했습니다. eventId={}, subscriberCount={}, createdCount={}, duplicateCount={}",
					event.eventId(),
					subscriberIds.size(),
					createdCount,
					subscriberIds.size() - createdCount
			);
		} catch (RuntimeException exception) {
			log.error(
					"비디오 알림 저장에 실패했습니다. eventId={}, videoId={}, creatorId={}",
					event.eventId(),
					event.videoId(),
					event.creatorId(),
					exception
			);
			throw exception;
		}
	}
}
