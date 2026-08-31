// 구독 HTTP 응답 본문을 표현하는 DTO.
package dev.backend.rabbitmq_notification.dto.subscription;

public record SubscriptionResponse(Long id, Long subscriberId, Long creatorId) {

	public static SubscriptionResponse from(SubscriptionServiceResult result) {
		return new SubscriptionResponse(result.id(), result.subscriberId(), result.creatorId());
	}
}
