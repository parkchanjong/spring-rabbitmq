// 동영상 생성과 단건 조회 HTTP 요청을 처리하는 Controller.
package dev.backend.rabbitmq_notification.controller;

import dev.backend.rabbitmq_notification.domain.video.Video;
import dev.backend.rabbitmq_notification.service.VideoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videos")
public class VideoController {

	private final VideoService videoService;

	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}

	@PostMapping
	public ResponseEntity<VideoResponse> create(@Valid @RequestBody CreateVideoRequest request) {
		Video video = videoService.create(request.memberId(), request.title(), request.description());
		return ResponseEntity.created(URI.create("/videos/" + video.getId()))
				.body(VideoResponse.from(video));
	}

	@GetMapping("/{id}")
	public VideoResponse findById(@PathVariable Long id) {
		return VideoResponse.from(videoService.findById(id));
	}

	public record CreateVideoRequest(@NotNull Long memberId, @NotBlank String title, String description) {
	}

	public record VideoResponse(
			Long id,
			Long memberId,
			String title,
			String description,
			long viewCount,
			long likeCount,
			LocalDateTime createdAt
	) {

		static VideoResponse from(Video video) {
			return new VideoResponse(
					video.getId(),
					video.getMember().getId(),
					video.getTitle(),
					video.getDescription(),
					video.getViewCount(),
					video.getLikeCount(),
					video.getCreatedAt()
			);
		}
	}
}
