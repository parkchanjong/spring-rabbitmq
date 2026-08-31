// RabbitMQ 발행 전 이벤트를 영속화하는 Outbox 엔티티.
package dev.backend.rabbitmq_notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
		name = "outbox_events",
		uniqueConstraints = @UniqueConstraint(name = "uk_outbox_events_event_id", columnNames = "event_id")
)
public class OutboxEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false, updatable = false, length = 36)
	private String eventId;

	@Column(nullable = false, updatable = false)
	private Long videoId;

	@Column(nullable = false, updatable = false)
	private Long creatorId;

	@Column(nullable = false, updatable = false)
	private String videoTitle;

	@Column(nullable = false, updatable = false)
	private LocalDateTime occurredAt;

	private LocalDateTime publishedAt;

	protected OutboxEvent() {
	}

	private OutboxEvent(String eventId, Long videoId, Long creatorId, String videoTitle, LocalDateTime occurredAt) {
		this.eventId = eventId;
		this.videoId = videoId;
		this.creatorId = creatorId;
		this.videoTitle = videoTitle;
		this.occurredAt = occurredAt;
	}

	public static OutboxEvent videoCreated(Video video) {
		return new OutboxEvent(
				UUID.randomUUID().toString(),
				video.getId(),
				video.getMember().getId(),
				video.getTitle(),
				video.getCreatedAt()
		);
	}

	public void markPublished() {
		this.publishedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public Long getVideoId() {
		return videoId;
	}

	public Long getCreatorId() {
		return creatorId;
	}

	public String getVideoTitle() {
		return videoTitle;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}
}
