// 성공 HTTP 응답 본문을 공통 형식으로 감싸는 DTO.
package dev.backend.rabbitmq_notification.dto.common;

public record ApiResponse<S>(S data) {

	public static <S> ApiResponse<S> success(S data) {
		return new ApiResponse<>(data);
	}
}
