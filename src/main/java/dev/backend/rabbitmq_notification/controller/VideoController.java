// 동영상 생성과 단건 조회 HTTP 요청을 처리하는 Controller.
package dev.backend.rabbitmq_notification.controller;

import dev.backend.rabbitmq_notification.dto.common.ApiResponse;
import dev.backend.rabbitmq_notification.dto.video.CreateVideoRequest;
import dev.backend.rabbitmq_notification.dto.video.VideoResponse;
import dev.backend.rabbitmq_notification.dto.video.VideoServiceResult;
import dev.backend.rabbitmq_notification.service.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videos")
public class VideoController {

	private final VideoService videoService;

	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<VideoResponse> create(
			@Valid @RequestBody CreateVideoRequest request,
			HttpServletResponse response
	) {
		VideoServiceResult result = videoService.create(request.memberId(), request.title(), request.description());
		response.setHeader(HttpHeaders.LOCATION, "/videos/" + result.id());
		return ApiResponse.success(VideoResponse.from(result));
	}

	@GetMapping("/{id}")
	public ApiResponse<VideoResponse> findById(@PathVariable Long id) {
		return ApiResponse.success(VideoResponse.from(videoService.findById(id)));
	}
}
