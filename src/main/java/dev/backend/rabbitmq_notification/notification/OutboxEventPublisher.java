// 미발행 Outbox 이벤트를 RabbitMQ에 발행하고 confirm 후 완료 처리한다.
package dev.backend.rabbitmq_notification.notification;

import dev.backend.rabbitmq_notification.domain.OutboxEvent;
import dev.backend.rabbitmq_notification.repository.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OutboxEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

	private final OutboxEventRepository outboxEventRepository;
	private final RabbitTemplate rabbitTemplate;

	public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
		this.outboxEventRepository = outboxEventRepository;
		this.rabbitTemplate = rabbitTemplate;
	}

	@Scheduled(fixedDelayString = "${notification.outbox.fixed-delay:1000}")
	public void publishPending() {
		List<OutboxEvent> pendingEvents = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();
		if (!pendingEvents.isEmpty()) {
			log.info("Outbox 발행 대상 이벤트를 조회했습니다. count={}", pendingEvents.size());
		}
		pendingEvents.forEach(this::publish);
	}

	private void publish(OutboxEvent outboxEvent) {
		try {
			log.debug(
					"Outbox 이벤트 발행을 시작합니다. eventId={}, videoId={}, creatorId={}",
					outboxEvent.getEventId(),
					outboxEvent.getVideoId(),
					outboxEvent.getCreatorId()
			);
			CorrelationData correlationData = new CorrelationData(outboxEvent.getEventId());
			rabbitTemplate.convertAndSend(
					NotificationRabbitConfig.VIDEO_EVENT_EXCHANGE,
					NotificationRabbitConfig.VIDEO_CREATED_ROUTING_KEY,
					toEvent(outboxEvent),
					message -> {
						message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
						return message;
					},
					correlationData
			);
			CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
			if (!confirm.ack()) {
				throw new IllegalStateException("RabbitMQ publish was not confirmed: " + confirm.reason());
			}
			outboxEvent.markPublished();
			outboxEventRepository.save(outboxEvent);
			log.debug(
					"Outbox 이벤트 발행을 완료했습니다. eventId={}, videoId={}, creatorId={}",
					outboxEvent.getEventId(),
					outboxEvent.getVideoId(),
					outboxEvent.getCreatorId()
			);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.error("Outbox 이벤트 발행이 인터럽트되었습니다. eventId={}", outboxEvent.getEventId(), exception);
			throw new IllegalStateException("RabbitMQ publish was interrupted", exception);
		} catch (Exception exception) {
			log.error("Outbox 이벤트 발행에 실패했습니다. eventId={}", outboxEvent.getEventId(), exception);
			throw new IllegalStateException("RabbitMQ publish failed", exception);
		}
	}

	private VideoCreatedEvent toEvent(OutboxEvent outboxEvent) {
		return new VideoCreatedEvent(
				outboxEvent.getEventId(),
				outboxEvent.getVideoId(),
				outboxEvent.getCreatorId(),
				outboxEvent.getVideoTitle(),
				outboxEvent.getOccurredAt()
		);
	}
}
