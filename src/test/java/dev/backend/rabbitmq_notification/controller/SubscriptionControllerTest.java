// 구독 컨트롤러의 HTTP 요청과 응답을 검증하는 슬라이스 테스트.
package dev.backend.rabbitmq_notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.backend.rabbitmq_notification.dto.SubscriptionServiceResult;
import dev.backend.rabbitmq_notification.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SubscriptionService subscriptionService;

	@Test
	void subscribeReturnsOkForNewSubscription() throws Exception {
		when(subscriptionService.subscribe(1L, 2L)).thenReturn(new SubscriptionServiceResult(3L, 1L, 2L, true));

		mockMvc.perform(post("/members/{subscriberId}/subscriptions/{creatorId}", 1L, 2L))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(jsonPath("$.data.id").value(3))
				.andExpect(jsonPath("$.data.subscriberId").value(1))
				.andExpect(jsonPath("$.data.creatorId").value(2));

		verify(subscriptionService).subscribe(1L, 2L);
	}

	@Test
	void subscribeReturnsOkForExistingSubscription() throws Exception {
		when(subscriptionService.subscribe(1L, 2L)).thenReturn(new SubscriptionServiceResult(3L, 1L, 2L, false));

		mockMvc.perform(post("/members/{subscriberId}/subscriptions/{creatorId}", 1L, 2L))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(jsonPath("$.data.id").value(3));
	}

	@Test
	void unsubscribeReturnsOk() throws Exception {
		mockMvc.perform(delete("/members/{subscriberId}/subscriptions/{creatorId}", 1L, 2L))
				.andExpect(status().isOk());

		verify(subscriptionService).unsubscribe(1L, 2L);
	}

	@Test
	void subscribeReturnsBadRequestFromService() throws Exception {
		when(subscriptionService.subscribe(1L, 1L)).thenThrow(
				new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscriber and creator must be different")
		);

		mockMvc.perform(post("/members/{subscriberId}/subscriptions/{creatorId}", 1L, 1L))
				.andExpect(status().isBadRequest());
	}
}
