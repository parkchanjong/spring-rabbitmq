// 동영상 생성과 조회 유스케이스를 처리하는 서비스.
package dev.backend.rabbitmq_notification.service;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.domain.Video;
import dev.backend.rabbitmq_notification.dto.VideoServiceResult;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import dev.backend.rabbitmq_notification.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class VideoService {

	private final MemberRepository memberRepository;
	private final VideoRepository videoRepository;

	public VideoService(MemberRepository memberRepository, VideoRepository videoRepository) {
		this.memberRepository = memberRepository;
		this.videoRepository = videoRepository;
	}

	@Transactional
	public VideoServiceResult create(Long memberId, String title, String description) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
		return toResult(videoRepository.save(Video.create(member, title, description)));
	}

	public VideoServiceResult findById(Long id) {
		return videoRepository.findById(id)
				.map(this::toResult)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
	}

	private VideoServiceResult toResult(Video video) {
		return new VideoServiceResult(
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
