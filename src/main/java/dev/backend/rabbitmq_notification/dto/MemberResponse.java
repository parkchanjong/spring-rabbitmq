// 회원 HTTP 응답 본문을 표현하는 DTO.
package dev.backend.rabbitmq_notification.dto;

import java.time.LocalDateTime;

public record MemberResponse(Long id, String name, LocalDateTime createdAt) {

	public static MemberResponse from(MemberServiceResult result) {
		return new MemberResponse(result.id(), result.name(), result.createdAt());
	}
}
