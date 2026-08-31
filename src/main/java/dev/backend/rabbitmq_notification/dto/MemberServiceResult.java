// 회원 서비스 처리 결과를 전달하는 DTO.
package dev.backend.rabbitmq_notification.dto.member;

import java.time.LocalDateTime;

public record MemberServiceResult(Long id, String name, LocalDateTime createdAt) {
}
