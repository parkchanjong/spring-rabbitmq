// 구독자에게 전달된 비디오 알림 이력을 저장하는 엔티티.
package dev.backend.rabbitmq_notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "notifications",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notifications_event_recipient",
				columnNames = {"event_id", "recipient_id"}
		)
)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false, updatable = false, length = 36)
	private String eventId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private Member recipient;

	@Column(nullable = false, updatable = false)
	private Long videoId;

	@Column(nullable = false, updatable = false)
	private String videoTitle;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Notification() {
	}

	public Long getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public Member getRecipient() {
		return recipient;
	}

	public Long getVideoId() {
		return videoId;
	}

	public String getVideoTitle() {
		return videoTitle;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
