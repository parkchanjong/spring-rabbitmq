// 회원 컨트롤러의 HTTP 요청과 응답을 검증하는 슬라이스 테스트.
package dev.backend.rabbitmq_notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.backend.rabbitmq_notification.dto.MemberServiceResult;
import dev.backend.rabbitmq_notification.service.MemberService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService memberService;

	@Test
	void createReturnsOkMember() throws Exception {
		when(memberService.create("chan")).thenReturn(member(1L, "chan"));

		mockMvc.perform(post("/members")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\": \"chan\"}"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.name").value("chan"));

		verify(memberService).create("chan");
	}

	@Test
	void findAllReturnsMembers() throws Exception {
		when(memberService.findAll()).thenReturn(List.of(member(1L, "first"), member(2L, "second")));

		mockMvc.perform(get("/members"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[1].name").value("second"));
	}

	@Test
	void findByIdReturnsMember() throws Exception {
		when(memberService.findById(1L)).thenReturn(member(1L, "chan"));

		mockMvc.perform(get("/members/{id}", 1L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.name").value("chan"));
	}

	@Test
	void createRejectsBlankName() throws Exception {
		mockMvc.perform(post("/members")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\": \"\"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(memberService);
	}

	@Test
	void findByIdReturnsNotFoundFromService() throws Exception {
		when(memberService.findById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

		mockMvc.perform(get("/members/{id}", 99L))
				.andExpect(status().isNotFound());
	}

	private MemberServiceResult member(Long id, String name) {
		return new MemberServiceResult(id, name, LocalDateTime.of(2026, 8, 31, 12, 0));
	}
}
