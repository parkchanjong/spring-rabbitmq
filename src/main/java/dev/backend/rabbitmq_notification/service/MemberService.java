// 회원 생성과 조회 유스케이스를 처리하는 서비스.
package dev.backend.rabbitmq_notification.service;

import dev.backend.rabbitmq_notification.domain.member.Member;
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
	public Member create(String name) {
		return memberRepository.save(Member.create(name));
	}

	public List<Member> findAll() {
		return memberRepository.findAll();
	}

	public Member findById(Long id) {
		return memberRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
	}
}
