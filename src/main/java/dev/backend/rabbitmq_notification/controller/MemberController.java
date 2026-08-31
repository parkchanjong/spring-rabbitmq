// 회원 생성과 조회 HTTP 요청을 처리하는 Controller.
package dev.backend.rabbitmq_notification.controller;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@PostMapping
	public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
		Member member = memberService.create(request.name());
		return ResponseEntity.created(URI.create("/members/" + member.getId()))
				.body(MemberResponse.from(member));
	}

	@GetMapping
	public List<MemberResponse> findAll() {
		return memberService.findAll().stream().map(MemberResponse::from).toList();
	}

	@GetMapping("/{id}")
	public MemberResponse findById(@PathVariable Long id) {
		return MemberResponse.from(memberService.findById(id));
	}

	public record CreateMemberRequest(@NotBlank String name) {
	}

	public record MemberResponse(Long id, String name, LocalDateTime createdAt) {

		static MemberResponse from(Member member) {
			return new MemberResponse(member.getId(), member.getName(), member.getCreatedAt());
		}
	}
}
