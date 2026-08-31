// 구독 서비스 처리 결과를 전달하는 DTO.
package dev.backend.rabbitmq_notification.dto;

public record SubscriptionServiceResult(Long id, Long subscriberId, Long creatorId, boolean created) {
}
