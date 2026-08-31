// 구독 도메인 엔티티의 관계 생성 규칙을 검증하는 테스트.
package dev.backend.rabbitmq_notification.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SubscriptionTest {

	@Test
	void subscriptionConnectsDifferentMembers() {
		Member subscriber = Member.create("subscriber");
		Member creator = Member.create("creator");

		Subscription subscription = Subscription.create(subscriber, creator);

		assertNull(subscription.getId());
		assertSame(subscriber, subscription.getSubscriber());
		assertSame(creator, subscription.getCreator());
	}

	@Test
	void selfSubscriptionIsRejected() {
		Member member = Member.create("member");

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> Subscription.create(member, member)
		);

		assertEquals("Subscriber and creator must be different", exception.getMessage());
	}
}
