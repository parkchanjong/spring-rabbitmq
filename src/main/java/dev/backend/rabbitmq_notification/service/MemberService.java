// 회원 생성과 조회 유스케이스를 처리하는 서비스.
package dev.backend.rabbitmq_notification.service;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.dto.member.MemberServiceResult;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Transactional
	public MemberServiceResult create(String name) {
		return toResult(memberRepository.save(Member.create(name)));
	}

	public List<MemberServiceResult> findAll() {
		return memberRepository.findAll().stream().map(this::toResult).toList();
	}

	public MemberServiceResult findById(Long id) {
		return memberRepository.findById(id)
				.map(this::toResult)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
	}

	private MemberServiceResult toResult(Member member) {
		return new MemberServiceResult(member.getId(), member.getName(), member.getCreatedAt());
	}
}
