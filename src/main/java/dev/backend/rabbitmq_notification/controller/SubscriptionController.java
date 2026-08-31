// 구독 생성과 취소 HTTP 요청을 처리하는 Controller.
package dev.backend.rabbitmq_notification.controller;

import dev.backend.rabbitmq_notification.dto.subscription.SubscriptionResponse;
import dev.backend.rabbitmq_notification.dto.subscription.SubscriptionServiceResult;
import dev.backend.rabbitmq_notification.service.SubscriptionService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/{subscriberId}/subscriptions/{creatorId}")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@PostMapping
	public ResponseEntity<SubscriptionResponse> subscribe(
			@PathVariable Long subscriberId,
			@PathVariable Long creatorId
	) {
		SubscriptionServiceResult result = subscriptionService.subscribe(subscriberId, creatorId);
		SubscriptionResponse response = SubscriptionResponse.from(result);
		if (result.created()) {
			return ResponseEntity.created(URI.create("/members/" + subscriberId + "/subscriptions/" + creatorId))
					.body(response);
		}
		return ResponseEntity.ok(response);
	}

	@DeleteMapping
	public ResponseEntity<Void> unsubscribe(
			@PathVariable Long subscriberId,
			@PathVariable Long creatorId
	) {
		subscriptionService.unsubscribe(subscriberId, creatorId);
		return ResponseEntity.noContent().build();
	}
}
