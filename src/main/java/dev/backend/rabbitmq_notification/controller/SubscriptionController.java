// 구독 생성과 취소 HTTP 요청을 처리하는 Controller.
package dev.backend.rabbitmq_notification.controller;

import dev.backend.rabbitmq_notification.dto.ApiResponse;
import dev.backend.rabbitmq_notification.dto.SubscriptionResponse;
import dev.backend.rabbitmq_notification.dto.SubscriptionServiceResult;
import dev.backend.rabbitmq_notification.service.SubscriptionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/{subscriberId}/subscriptions/{creatorId}")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@PostMapping
	public ApiResponse<SubscriptionResponse> subscribe(
			@PathVariable Long subscriberId,
			@PathVariable Long creatorId,
			HttpServletResponse response
	) {
		SubscriptionServiceResult result = subscriptionService.subscribe(subscriberId, creatorId);
		SubscriptionResponse subscriptionResponse = SubscriptionResponse.from(result);
		if (result.created()) {
			response.setStatus(HttpStatus.CREATED.value());
			response.setHeader(HttpHeaders.LOCATION, "/members/" + subscriberId + "/subscriptions/" + creatorId);
		} else {
			response.setStatus(HttpStatus.OK.value());
		}
		return ApiResponse.success(subscriptionResponse);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribe(
			@PathVariable Long subscriberId,
			@PathVariable Long creatorId
	) {
		subscriptionService.unsubscribe(subscriberId, creatorId);
	}
}
