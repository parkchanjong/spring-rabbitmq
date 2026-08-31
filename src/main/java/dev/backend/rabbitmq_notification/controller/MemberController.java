// 회원 생성과 조회 HTTP 요청을 처리하는 Controller.
package dev.backend.rabbitmq_notification.controller;

import dev.backend.rabbitmq_notification.dto.ApiResponse;
import dev.backend.rabbitmq_notification.dto.CreateMemberRequest;
import dev.backend.rabbitmq_notification.dto.MemberResponse;
import dev.backend.rabbitmq_notification.dto.MemberServiceResult;
import dev.backend.rabbitmq_notification.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<MemberResponse> create(
			@Valid @RequestBody CreateMemberRequest request,
			HttpServletResponse response
	) {
		MemberServiceResult result = memberService.create(request.name());
		response.setHeader(HttpHeaders.LOCATION, "/members/" + result.id());
		return ApiResponse.success(MemberResponse.from(result));
	}

	@GetMapping
	public ApiResponse<List<MemberResponse>> findAll() {
		return ApiResponse.success(memberService.findAll().stream().map(MemberResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ApiResponse<MemberResponse> findById(@PathVariable Long id) {
		return ApiResponse.success(MemberResponse.from(memberService.findById(id)));
	}
}
