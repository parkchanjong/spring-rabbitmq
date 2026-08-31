// 구독 서비스의 생성과 취소 동작을 검증하는 단위 테스트.
package dev.backend.rabbitmq_notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.domain.Subscription;
import dev.backend.rabbitmq_notification.dto.SubscriptionServiceResult;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import dev.backend.rabbitmq_notification.repository.SubscriptionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@InjectMocks
	private SubscriptionService subscriptionService;

	@Test
	void subscribeCreatesSubscriptionWhenItDoesNotExist() {
		Member subscriber = member(1L);
		Member creator = member(2L);
		Subscription savedSubscription = subscription(3L, subscriber, creator);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(subscriber));
		when(memberRepository.findById(2L)).thenReturn(Optional.of(creator));
		when(subscriptionRepository.findBySubscriberIdAndCreatorId(1L, 2L)).thenReturn(Optional.empty());
		when(subscriptionRepository.save(any(Subscription.class))).thenReturn(savedSubscription);

		SubscriptionServiceResult result = subscriptionService.subscribe(1L, 2L);

		ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
		verify(subscriptionRepository).save(captor.capture());
		assertEquals(subscriber, captor.getValue().getSubscriber());
		assertEquals(creator, captor.getValue().getCreator());
		assertEquals(new SubscriptionServiceResult(3L, 1L, 2L, true), result);
	}

	@Test
	void subscribeReturnsExistingSubscriptionWithoutSavingAgain() {
		Member subscriber = member(1L);
		Member creator = member(2L);
		Subscription subscription = subscription(3L, subscriber, creator);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(subscriber));
		when(memberRepository.findById(2L)).thenReturn(Optional.of(creator));
		when(subscriptionRepository.findBySubscriberIdAndCreatorId(1L, 2L)).thenReturn(Optional.of(subscription));

		SubscriptionServiceResult result = subscriptionService.subscribe(1L, 2L);

		verify(subscriptionRepository, never()).save(any(Subscription.class));
		assertEquals(new SubscriptionServiceResult(3L, 1L, 2L, false), result);
	}

	@Test
	void unsubscribeDeletesExistingSubscription() {
		Member subscriber = mock(Member.class);
		Member creator = mock(Member.class);
		Subscription subscription = Subscription.create(subscriber, creator);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(subscriber));
		when(memberRepository.findById(2L)).thenReturn(Optional.of(creator));
		when(subscriptionRepository.findBySubscriberIdAndCreatorId(1L, 2L)).thenReturn(Optional.of(subscription));

		subscriptionService.unsubscribe(1L, 2L);

		verify(subscriptionRepository).delete(subscription);
	}

	@Test
	void unsubscribeIgnoresMissingSubscription() {
		Member subscriber = mock(Member.class);
		Member creator = mock(Member.class);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(subscriber));
		when(memberRepository.findById(2L)).thenReturn(Optional.of(creator));
		when(subscriptionRepository.findBySubscriberIdAndCreatorId(1L, 2L)).thenReturn(Optional.empty());

		subscriptionService.unsubscribe(1L, 2L);

		verify(subscriptionRepository, never()).delete(any(Subscription.class));
	}

	@Test
	void subscribeRejectsSelfSubscriptionBeforeRepositoryAccess() {
		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> subscriptionService.subscribe(1L, 1L)
		);

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
		verify(memberRepository, never()).findById(any());
	}

	@Test
	void subscribeRejectsMissingMember() {
		when(memberRepository.findById(1L)).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> subscriptionService.subscribe(1L, 2L)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	private Member member(Long id) {
		Member member = mock(Member.class);
		when(member.getId()).thenReturn(id);
		return member;
	}

	private Subscription subscription(Long id, Member subscriber, Member creator) {
		Subscription subscription = mock(Subscription.class);
		when(subscription.getId()).thenReturn(id);
		when(subscription.getSubscriber()).thenReturn(subscriber);
		when(subscription.getCreator()).thenReturn(creator);
		return subscription;
	}
}
