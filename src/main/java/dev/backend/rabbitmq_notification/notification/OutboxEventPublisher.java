// 미발행 Outbox 이벤트를 RabbitMQ에 발행하고 confirm 후 완료 처리한다.
package dev.backend.rabbitmq_notification.notification;

import dev.backend.rabbitmq_notification.domain.OutboxEvent;
import dev.backend.rabbitmq_notification.repository.OutboxEventRepository;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventPublisher {

	private final OutboxEventRepository outboxEventRepository;
	private final RabbitTemplate rabbitTemplate;

	public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
		this.outboxEventRepository = outboxEventRepository;
		this.rabbitTemplate = rabbitTemplate;
	}

	@Scheduled(fixedDelayString = "${notification.outbox.fixed-delay:1000}")
	public void publishPending() {
		outboxEventRepository.findTop100ByPublishedAtIsNullOrderByIdAsc().forEach(this::publish);
	}

	private void publish(OutboxEvent outboxEvent) {
		try {
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
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("RabbitMQ publish was interrupted", exception);
		} catch (Exception exception) {
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
