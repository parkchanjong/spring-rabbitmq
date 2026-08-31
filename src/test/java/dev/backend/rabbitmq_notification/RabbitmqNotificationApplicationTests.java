// Member, Video, Subscription 동작을 MySQL 컨테이너에서 검증하는 통합 테스트.
package dev.backend.rabbitmq_notification;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.domain.Subscription;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import dev.backend.rabbitmq_notification.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RabbitmqNotificationApplicationTests {

	@Container
	static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("rabbitmq_notification")
			.withUsername("notification")
			.withPassword("notification");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	@Test
	void memberCanBeCreatedAndRetrieved() throws Exception {
		long memberId = createMember("chan");

		mockMvc.perform(get("/members/{id}", memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(memberId))
				.andExpect(jsonPath("$.name").value("chan"))
				.andExpect(jsonPath("$.createdAt").exists());

		mockMvc.perform(get("/members"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem((int) memberId)));
	}

	@Test
	void videoCanBeCreatedAndRetrieved() throws Exception {
		long memberId = createMember("chan");
		String response = mockMvc.perform(post("/videos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"memberId": %d, "title": "Spring RabbitMQ", "description": "intro"}
								""".formatted(memberId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId").value(memberId))
				.andExpect(jsonPath("$.viewCount").value(0))
				.andExpect(jsonPath("$.likeCount").value(0))
				.andReturn().getResponse().getContentAsString();
		long videoId = objectMapper.readTree(response).path("id").asLong();

		mockMvc.perform(get("/videos/{id}", videoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(videoId))
				.andExpect(jsonPath("$.title").value("Spring RabbitMQ"));
	}

	@Test
	void missingRequiredValuesReturnBadRequest() throws Exception {
		mockMvc.perform(post("/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"\"}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/videos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\": \"video\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingResourcesReturnNotFound() throws Exception {
		mockMvc.perform(get("/members/{id}", 999L))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/videos/{id}", 999L))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/videos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"memberId\": 999, \"title\": \"video\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void subscriptionCanBeCreatedBetweenDifferentMembers() {
		Member subscriber = memberRepository.save(Member.create("subscriber"));
		Member creator = memberRepository.save(Member.create("creator"));

		Subscription subscription = subscriptionRepository.saveAndFlush(Subscription.create(subscriber, creator));

		assertEquals(subscriber.getId(), subscription.getSubscriber().getId());
		assertEquals(creator.getId(), subscription.getCreator().getId());
	}

	@Test
	void duplicateSubscriptionIsRejected() {
		Member subscriber = memberRepository.save(Member.create("subscriber"));
		Member creator = memberRepository.save(Member.create("creator"));
		subscriptionRepository.saveAndFlush(Subscription.create(subscriber, creator));

		assertThrows(
				DataIntegrityViolationException.class,
				() -> subscriptionRepository.saveAndFlush(Subscription.create(subscriber, creator))
		);
	}

	@Test
	void subscriptionRejectsSelfSubscription() {
		Member member = Member.create("member");

		assertThrows(IllegalArgumentException.class, () -> Subscription.create(member, member));
	}

	@Test
	void subscriptionCanBeCreatedAndCancelledThroughApi() throws Exception {
		long subscriberId = createMember("subscriber");
		long creatorId = createMember("creator");
		String subscriptionPath = "/members/%d/subscriptions/%d".formatted(subscriberId, creatorId);

		mockMvc.perform(post(subscriptionPath))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", subscriptionPath))
				.andExpect(jsonPath("$.subscriberId").value(subscriberId))
				.andExpect(jsonPath("$.creatorId").value(creatorId));

		mockMvc.perform(delete(subscriptionPath))
				.andExpect(status().isNoContent());

		assertTrue(subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId).isEmpty());
	}

	@Test
	void repeatedSubscriptionAndCancellationAreIdempotent() throws Exception {
		long subscriberId = createMember("subscriber");
		long creatorId = createMember("creator");
		String subscriptionPath = "/members/%d/subscriptions/%d".formatted(subscriberId, creatorId);

		mockMvc.perform(post(subscriptionPath))
				.andExpect(status().isCreated());

		mockMvc.perform(post(subscriptionPath))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriberId").value(subscriberId))
				.andExpect(jsonPath("$.creatorId").value(creatorId));

		assertEquals(1, subscriptionRepository.countBySubscriberIdAndCreatorId(subscriberId, creatorId));

		mockMvc.perform(delete(subscriptionPath))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete(subscriptionPath))
				.andExpect(status().isNoContent());
	}

	@Test
	void invalidSubscriptionTargetsReturnClientErrors() throws Exception {
		long memberId = createMember("member");

		mockMvc.perform(post("/members/{subscriberId}/subscriptions/{creatorId}", memberId, memberId))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/members/{subscriberId}/subscriptions/{creatorId}", 999L, memberId))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/members/{subscriberId}/subscriptions/{creatorId}", memberId, 999L))
				.andExpect(status().isNotFound());
	}

	private long createMember(String name) throws Exception {
		String response = mockMvc.perform(post("/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"%s\"}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(response);
		return json.path("id").asLong();
	}
}
