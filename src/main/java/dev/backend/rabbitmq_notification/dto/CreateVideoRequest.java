// 동영상 생성 HTTP 요청 본문을 표현하는 DTO.
package dev.backend.rabbitmq_notification.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVideoRequest(@NotNull Long memberId, @NotBlank String title, String description) {
}
