// 회원 생성과 조회 HTTP 요청을 처리하는 Controller.
package dev.backend.rabbitmq_notification.controller;

import dev.backend.rabbitmq_notification.dto.member.CreateMemberRequest;
import dev.backend.rabbitmq_notification.dto.member.MemberResponse;
import dev.backend.rabbitmq_notification.dto.member.MemberServiceResult;
import dev.backend.rabbitmq_notification.service.MemberService;
import jakarta.validation.Valid;
import java.net.URI;
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
		MemberServiceResult result = memberService.create(request.name());
		return ResponseEntity.created(URI.create("/members/" + result.id()))
				.body(MemberResponse.from(result));
	}

	@GetMapping
	public List<MemberResponse> findAll() {
		return memberService.findAll().stream().map(MemberResponse::from).toList();
	}

	@GetMapping("/{id}")
	public MemberResponse findById(@PathVariable Long id) {
		return MemberResponse.from(memberService.findById(id));
	}
}
