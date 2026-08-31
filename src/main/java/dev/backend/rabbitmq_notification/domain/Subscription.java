// 크리에이터를 구독한 회원 관계를 저장하는 도메인 엔티티.
package dev.backend.rabbitmq_notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "subscriptions",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_subscriptions_subscriber_creator",
				columnNames = {"subscriber_id", "creator_id"}
		)
)
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subscriber_id", nullable = false)
	private Member subscriber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator_id", nullable = false)
	private Member creator;

	protected Subscription() {
	}

	private Subscription(Member subscriber, Member creator) {
		this.subscriber = subscriber;
		this.creator = creator;
	}

	public static Subscription create(Member subscriber, Member creator) {
		if (subscriber == creator) {
			throw new IllegalArgumentException("Subscriber and creator must be different");
		}
		return new Subscription(subscriber, creator);
	}

	public Long getId() {
		return id;
	}

	public Member getSubscriber() {
		return subscriber;
	}

	public Member getCreator() {
		return creator;
	}
}
