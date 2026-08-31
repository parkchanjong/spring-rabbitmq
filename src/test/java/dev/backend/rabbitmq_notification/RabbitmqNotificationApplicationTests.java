// Member, Video, Subscription 동작을 MySQL 컨테이너에서 검증하는 통합 테스트.
package dev.backend.rabbitmq_notification;

import dev.backend.rabbitmq_notification.domain.Member;
import dev.backend.rabbitmq_notification.domain.Subscription;
import dev.backend.rabbitmq_notification.domain.OutboxEvent;
import dev.backend.rabbitmq_notification.notification.NotificationRabbitConfig;
import dev.backend.rabbitmq_notification.notification.OutboxEventPublisher;
import dev.backend.rabbitmq_notification.notification.VideoCreatedEvent;
import dev.backend.rabbitmq_notification.repository.MemberRepository;
import dev.backend.rabbitmq_notification.repository.NotificationRepository;
import dev.backend.rabbitmq_notification.repository.OutboxEventRepository;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
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

	@Container
	static final GenericContainer<?> rabbitmq = new GenericContainer<>(DockerImageName.parse("rabbitmq:4.3.5-management"))
			.withExposedPorts(5672)
			.withEnv("RABBITMQ_DEFAULT_USER", "notification")
			.withEnv("RABBITMQ_DEFAULT_PASS", "notification");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private OutboxEventPublisher outboxEventPublisher;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.rabbitmq.host", rabbitmq::getHost);
		registry.add("spring.rabbitmq.port", () -> rabbitmq.getMappedPort(5672));
		registry.add("spring.rabbitmq.username", () -> "notification");
		registry.add("spring.rabbitmq.password", () -> "notification");
	}

	@Test
	void memberCanBeCreatedAndRetrieved() throws Exception {
		long memberId = createMember("chan");

		mockMvc.perform(get("/members/{id}", memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(memberId))
				.andExpect(jsonPath("$.data.name").value("chan"))
				.andExpect(jsonPath("$.data.createdAt").exists());

		mockMvc.perform(get("/members"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id", hasItem((int) memberId)));
	}

	@Test
	void videoCanBeCreatedAndRetrieved() throws Exception {
		long memberId = createMember("chan");
		var result = mockMvc.perform(post("/videos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"memberId": %d, "title": "Spring RabbitMQ", "description": "intro"}
								""".formatted(memberId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.memberId").value(memberId))
				.andExpect(jsonPath("$.data.viewCount").value(0))
				.andExpect(jsonPath("$.data.likeCount").value(0))
				.andReturn();
		String response = result.getResponse().getContentAsString();
		long videoId = objectMapper.readTree(response).path("data").path("id").asLong();

		mockMvc.perform(get("/videos/{id}", videoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(videoId))
				.andExpect(jsonPath("$.data.title").value("Spring RabbitMQ"));
	}

	@Test
	void videoCreatedEventCreatesOneNotificationPerSubscriberEvenWhenRedelivered() throws Exception {
		long creatorId = createMember("creator");
		long firstSubscriberId = createMember("subscriber-one");
		long secondSubscriberId = createMember("subscriber-two");
		subscriptionRepository.saveAndFlush(Subscription.create(
				memberRepository.getReferenceById(firstSubscriberId),
				memberRepository.getReferenceById(creatorId)
		));
		subscriptionRepository.saveAndFlush(Subscription.create(
				memberRepository.getReferenceById(secondSubscriberId),
				memberRepository.getReferenceById(creatorId)
		));

		mockMvc.perform(post("/videos")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"memberId": %d, "title": "RabbitMQ event", "description": "intro"}
							""".formatted(creatorId)))
				.andExpect(status().isOk());

		OutboxEvent outboxEvent = outboxEventRepository.findAll().stream()
				.filter(event -> event.getCreatorId().equals(creatorId))
				.findFirst()
				.orElseThrow();
		outboxEventPublisher.publishPending();
		awaitNotificationCount(outboxEvent.getEventId(), 2);

		rabbitTemplate.convertAndSend(
				NotificationRabbitConfig.VIDEO_EVENT_EXCHANGE,
				NotificationRabbitConfig.VIDEO_CREATED_ROUTING_KEY,
				new VideoCreatedEvent(
						outboxEvent.getEventId(),
						outboxEvent.getVideoId(),
						outboxEvent.getCreatorId(),
						outboxEvent.getVideoTitle(),
						outboxEvent.getOccurredAt()
				)
		);
		awaitNotificationCount(outboxEvent.getEventId(), 2);
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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.subscriberId").value(subscriberId))
				.andExpect(jsonPath("$.data.creatorId").value(creatorId));

		mockMvc.perform(delete(subscriptionPath))
				.andExpect(status().isOk());

		assertTrue(subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId).isEmpty());
	}

	@Test
	void repeatedSubscriptionAndCancellationAreIdempotent() throws Exception {
		long subscriberId = createMember("subscriber");
		long creatorId = createMember("creator");
		String subscriptionPath = "/members/%d/subscriptions/%d".formatted(subscriberId, creatorId);

		mockMvc.perform(post(subscriptionPath))
				.andExpect(status().isOk());

		mockMvc.perform(post(subscriptionPath))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.subscriberId").value(subscriberId))
				.andExpect(jsonPath("$.data.creatorId").value(creatorId));

		assertEquals(1, subscriptionRepository.countBySubscriberIdAndCreatorId(subscriberId, creatorId));

		mockMvc.perform(delete(subscriptionPath))
				.andExpect(status().isOk());

		mockMvc.perform(delete(subscriptionPath))
				.andExpect(status().isOk());
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
		var result = mockMvc.perform(post("/members")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"%s\"}".formatted(name)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		long memberId = json.path("data").path("id").asLong();
		return memberId;
	}

	private void awaitNotificationCount(String eventId, long expectedCount) throws InterruptedException {
		for (int attempt = 0; attempt < 50; attempt++) {
			if (notificationRepository.countByEventId(eventId) == expectedCount) {
				return;
			}
			Thread.sleep(100);
		}
		assertEquals(expectedCount, notificationRepository.countByEventId(eventId));
	}
}
