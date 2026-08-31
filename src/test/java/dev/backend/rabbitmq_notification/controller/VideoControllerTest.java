// 동영상 컨트롤러의 HTTP 요청과 응답을 검증하는 슬라이스 테스트.
package dev.backend.rabbitmq_notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.backend.rabbitmq_notification.dto.VideoServiceResult;
import dev.backend.rabbitmq_notification.service.VideoService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(VideoController.class)
class VideoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VideoService videoService;

	@Test
	void createReturnsOkVideo() throws Exception {
		when(videoService.create(1L, "title", "description")).thenReturn(video(2L));

		mockMvc.perform(post("/videos")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"memberId\": 1, \"title\": \"title\", \"description\": \"description\"}"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(jsonPath("$.data.id").value(2))
				.andExpect(jsonPath("$.data.memberId").value(1))
				.andExpect(jsonPath("$.data.viewCount").value(0))
				.andExpect(jsonPath("$.data.likeCount").value(0));

		verify(videoService).create(1L, "title", "description");
	}

	@Test
	void findByIdReturnsVideo() throws Exception {
		when(videoService.findById(2L)).thenReturn(video(2L));

		mockMvc.perform(get("/videos/{id}", 2L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("title"));
	}

	@Test
	void createRejectsMissingRequiredMemberId() throws Exception {
		mockMvc.perform(post("/videos")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\": \"title\"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(videoService);
	}

	@Test
	void findByIdReturnsNotFoundFromService() throws Exception {
		when(videoService.findById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

		mockMvc.perform(get("/videos/{id}", 99L))
				.andExpect(status().isNotFound());
	}

	private VideoServiceResult video(Long id) {
		return new VideoServiceResult(
				id,
				1L,
				"title",
				"description",
				0,
				0,
				LocalDateTime.of(2026, 8, 31, 12, 0)
		);
	}
}
