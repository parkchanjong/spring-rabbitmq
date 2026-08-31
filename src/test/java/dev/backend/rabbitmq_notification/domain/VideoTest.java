// 동영상 도메인 엔티티의 생성 기본값을 검증하는 테스트.
package dev.backend.rabbitmq_notification.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class VideoTest {

	@Test
	void videoIsCreatedWithInitialCounters() {
		Member member = Member.create("creator");

		Video video = Video.create(member, "Spring RabbitMQ", "intro");

		assertNull(video.getId());
		assertSame(member, video.getMember());
		assertEquals("Spring RabbitMQ", video.getTitle());
		assertEquals("intro", video.getDescription());
		assertEquals(0, video.getViewCount());
		assertEquals(0, video.getLikeCount());
		assertNotNull(video.getCreatedAt());
	}
}
