// 회원 도메인 엔티티의 생성 기본값을 검증하는 테스트.
package dev.backend.rabbitmq_notification.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MemberTest {

	@Test
	void memberIsCreatedWithNameAndCreatedAt() {
		Member member = Member.create("chan");

		assertNull(member.getId());
		assertEquals("chan", member.getName());
		assertNotNull(member.getCreatedAt());
	}
}
