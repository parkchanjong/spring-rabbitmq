// 회원 생성 HTTP 요청 본문을 표현하는 DTO.
package dev.backend.rabbitmq_notification.dto.member;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(@NotBlank String name) {
}
