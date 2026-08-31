// 동영상 서비스 처리 결과를 전달하는 DTO.
package dev.backend.rabbitmq_notification.dto.video;

import java.time.LocalDateTime;

public record VideoServiceResult(
		Long id,
		Long memberId,
		String title,
		String description,
		long viewCount,
		long likeCount,
		LocalDateTime createdAt
) {
}
