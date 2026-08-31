// 동영상 HTTP 응답 본문을 표현하는 DTO.
package dev.backend.rabbitmq_notification.dto.video;

import java.time.LocalDateTime;

public record VideoResponse(
		Long id,
		Long memberId,
		String title,
		String description,
		long viewCount,
		long likeCount,
		LocalDateTime createdAt
) {

	public static VideoResponse from(VideoServiceResult result) {
		return new VideoResponse(
				result.id(),
				result.memberId(),
				result.title(),
				result.description(),
				result.viewCount(),
				result.likeCount(),
				result.createdAt()
		);
	}
}
