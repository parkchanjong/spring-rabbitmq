// 회원 간 구독 생성과 취소 유스케이스를 처리하는 서비스.
package dev.backend.rabbitmq_notification.service;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.domain.Subscription;
import dev.backend.rabbitmq_notification.dto.subscription.SubscriptionServiceResult;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import dev.backend.rabbitmq_notification.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SubscriptionService {

	private final MemberRepository memberRepository;
	private final SubscriptionRepository subscriptionRepository;

	public SubscriptionService(MemberRepository memberRepository, SubscriptionRepository subscriptionRepository) {
		this.memberRepository = memberRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Transactional
	public SubscriptionServiceResult subscribe(Long subscriberId, Long creatorId) {
		validateDifferentMembers(subscriberId, creatorId);
		Member subscriber = findMember(subscriberId);
		Member creator = findMember(creatorId);

		return subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId)
				.map(subscription -> toResult(subscription, false))
				.orElseGet(() -> toResult(subscriptionRepository.save(Subscription.create(subscriber, creator)), true));
	}

	@Transactional
	public void unsubscribe(Long subscriberId, Long creatorId) {
		validateDifferentMembers(subscriberId, creatorId);
		findMember(subscriberId);
		findMember(creatorId);
		subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId)
				.ifPresent(subscriptionRepository::delete);
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
	}

	private void validateDifferentMembers(Long subscriberId, Long creatorId) {
		if (subscriberId.equals(creatorId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscriber and creator must be different");
		}
	}

	private SubscriptionServiceResult toResult(Subscription subscription, boolean created) {
		return new SubscriptionServiceResult(
				subscription.getId(),
				subscription.getSubscriber().getId(),
				subscription.getCreator().getId(),
				created
		);
	}
}
