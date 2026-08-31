// 동영상 서비스의 생성과 조회 동작을 검증하는 단위 테스트.
package dev.backend.rabbitmq_notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.domain.OutboxEvent;
import dev.backend.rabbitmq_notification.domain.Video;
import dev.backend.rabbitmq_notification.dto.VideoServiceResult;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import dev.backend.rabbitmq_notification.repository.OutboxEventRepository;
import dev.backend.rabbitmq_notification.repository.VideoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private VideoRepository videoRepository;

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@InjectMocks
	private VideoService videoService;

	@Test
	void createSavesVideoForExistingMemberAndReturnsResult() {
		Member member = member(1L);
		LocalDateTime createdAt = LocalDateTime.of(2026, 8, 31, 12, 0);
		Video savedVideo = video(2L, member, "title", "description", createdAt);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
		when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

		VideoServiceResult result = videoService.create(1L, "title", "description");

		ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
		ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(videoRepository).save(captor.capture());
		verify(outboxEventRepository).save(outboxCaptor.capture());
		assertEquals(member, captor.getValue().getMember());
		assertEquals("title", captor.getValue().getTitle());
		assertEquals("description", captor.getValue().getDescription());
		assertEquals(2L, outboxCaptor.getValue().getVideoId());
		assertEquals(1L, outboxCaptor.getValue().getCreatorId());
		assertEquals("title", outboxCaptor.getValue().getVideoTitle());
		assertEquals(new VideoServiceResult(2L, 1L, "title", "description", 0, 0, createdAt), result);
	}

	@Test
	void createRejectsMissingMember() {
		when(memberRepository.findById(99L)).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> videoService.create(99L, "title", "description")
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void findByIdReturnsMappedVideo() {
		Member member = member(1L);
		LocalDateTime createdAt = LocalDateTime.of(2026, 8, 31, 12, 0);
		Video video = video(2L, member, "title", "description", createdAt);
		when(videoRepository.findById(2L)).thenReturn(Optional.of(video));

		VideoServiceResult result = videoService.findById(2L);

		assertEquals(new VideoServiceResult(2L, 1L, "title", "description", 0, 0, createdAt), result);
	}

	@Test
	void findByIdRejectsMissingVideo() {
		when(videoRepository.findById(99L)).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> videoService.findById(99L)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	private Member member(Long id) {
		Member member = mock(Member.class);
		when(member.getId()).thenReturn(id);
		return member;
	}

	private Video video(Long id, Member member, String title, String description, LocalDateTime createdAt) {
		Video video = mock(Video.class);
		when(video.getId()).thenReturn(id);
		when(video.getMember()).thenReturn(member);
		when(video.getTitle()).thenReturn(title);
		when(video.getDescription()).thenReturn(description);
		when(video.getViewCount()).thenReturn(0L);
		when(video.getLikeCount()).thenReturn(0L);
		when(video.getCreatedAt()).thenReturn(createdAt);
		return video;
	}
}
