// 회원 서비스의 생성과 조회 동작을 검증하는 단위 테스트.
package dev.backend.rabbitmq_notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.dto.MemberServiceResult;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
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
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@InjectMocks
	private MemberService memberService;

	@Test
	void createSavesMemberAndReturnsResult() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 8, 31, 12, 0);
		Member savedMember = member(1L, "chan", createdAt);
		when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

		MemberServiceResult result = memberService.create("chan");

		ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
		verify(memberRepository).save(captor.capture());
		assertEquals("chan", captor.getValue().getName());
		assertEquals(new MemberServiceResult(1L, "chan", createdAt), result);
	}

	@Test
	void findAllReturnsMappedMembers() {
		LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 8, 31, 12, 0);
		LocalDateTime secondCreatedAt = LocalDateTime.of(2026, 8, 31, 12, 1);
		Member first = member(1L, "first", firstCreatedAt);
		Member second = member(2L, "second", secondCreatedAt);
		when(memberRepository.findAll()).thenReturn(List.of(first, second));

		List<MemberServiceResult> results = memberService.findAll();

		assertEquals(List.of(
				new MemberServiceResult(1L, "first", firstCreatedAt),
				new MemberServiceResult(2L, "second", secondCreatedAt)
		), results);
	}

	@Test
	void findByIdReturnsMappedMember() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 8, 31, 12, 0);
		Member member = member(1L, "chan", createdAt);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

		MemberServiceResult result = memberService.findById(1L);

		assertEquals(new MemberServiceResult(1L, "chan", createdAt), result);
	}

	@Test
	void findByIdRejectsMissingMember() {
		when(memberRepository.findById(99L)).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> memberService.findById(99L)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	private Member member(Long id, String name, LocalDateTime createdAt) {
		Member member = mock(Member.class);
		when(member.getId()).thenReturn(id);
		when(member.getName()).thenReturn(name);
		when(member.getCreatedAt()).thenReturn(createdAt);
		return member;
	}
}
