# TRD. 비디오 구독 알림 예제

## 문서 정보

| 항목 | 내용 |
| --- | --- |
| 버전 | 1.0 |
| 작성일 | 2026-09-01 |
| 상태 | Draft |
| 관련 제품 문서 | [PRD](PRD.md) |
| 구현 지침 | [REQUIREMENTS](REQUIREMENTS.md) |

## 1. 기술 개요

### 시스템 아키텍처

```text
HTTP Client
    │
    ▼
Spring Boot 애플리케이션
    ├── Member, Subscription, Video Controller와 Service
    │       │
    │       ▼
    │   MySQL 8.4
    │   members, subscriptions, videos, outbox_events, notifications
    │
    ├── OutboxEventPublisher
    │       │  1초 주기, 미발행 이벤트 최대 100건
    │       ▼
    │   RabbitMQ 4.3.5
    │   video.events ── video.created ──> video.notification
    │                                      │
    │                                      ▼
    ├── VideoNotificationConsumer ─────> MySQL notifications
    │       │ 실패 시 재시도
    │       ▼
    │   video.events.dlx ──> video.notification.dlq
    │
    └── Actuator / Prometheus / Grafana
```

### 기술 스택

| 영역 | 기술 | 버전 또는 구성 |
| --- | --- | --- |
| 언어·빌드 | Java, Gradle Wrapper | Java 26 |
| 애플리케이션 | Spring Boot | 4.1.1 |
| 웹·검증 | Spring Web MVC, Bean Validation | Spring Boot 의존성 관리 |
| 영속성 | Spring Data JPA, Hibernate | Spring Boot 의존성 관리 |
| 데이터베이스 | MySQL | 8.4 |
| 메시징 | Spring AMQP, RabbitMQ Management | RabbitMQ 4.3.5 |
| 모니터링 | Actuator, Micrometer Prometheus, Prometheus, Grafana | Prometheus 3.13.1, Grafana 13.1.0 |
| 테스트 | JUnit Jupiter, MockMvc, Testcontainers | MySQL·RabbitMQ 컨테이너 |
| 부하 검증 | k6 | `k6/video-create.js` |

## 2. 컴포넌트와 메시지 흐름

### 책임 분리

| 컴포넌트 | 책임 |
| --- | --- |
| Controller | 회원, 구독, 비디오 HTTP 요청을 받고 성공 결과를 `{ "data": ... }`로 반환 |
| Service | 도메인 규칙, 트랜잭션, 리소스 존재 여부를 처리 |
| Repository | JPA 기반 조회·저장과 알림의 멱등 삽입을 수행 |
| OutboxEventPublisher | 미발행 이벤트를 조회해 RabbitMQ confirm 후 발행 완료 처리 |
| VideoNotificationConsumer | 비디오 생성 이벤트를 수신해 현재 구독자별 알림 이력을 저장 |
| NotificationRabbitConfig | Exchange, Queue, DLQ, JSON 변환기, 소비 재시도를 구성 |

### 비디오 생성 흐름

1. 클라이언트가 `POST /videos`로 작성자 ID, 제목, 설명을 요청한다.
2. `VideoService`가 작성자를 조회한다. 없으면 `404 Not Found`를 반환한다.
3. 같은 데이터베이스 트랜잭션에서 `videos` 레코드와 UUID 이벤트 ID를 가진 `outbox_events` 레코드를 저장한다.
4. HTTP 응답은 생성된 비디오를 반환한다. 메시지 발행과 알림 저장은 이 응답과 비동기로 수행된다.
5. `OutboxEventPublisher`는 `published_at`이 비어 있는 이벤트를 ID 오름차순으로 최대 100건 조회한다.
6. 이벤트를 durable direct exchange `video.events`의 `video.created` 라우팅 키로 persistent 메시지로 발행한다.
7. 5초 안에 publisher confirm ACK를 받으면 `published_at`을 기록한다. confirm 실패나 예외 발생 시 완료 처리하지 않으므로 다음 스케줄에서 다시 대상이 된다.
8. `VideoNotificationConsumer`가 `video.notification` 큐에서 이벤트를 수신하고, 생성자를 구독한 회원 ID를 조회한다.
9. 수신자마다 `INSERT IGNORE`로 `notifications` 이력을 저장한다. 같은 `(event_id, recipient_id)`는 유니크 제약으로 한 번만 남는다.

### 실패 처리

- 소비자 컨테이너는 동시 소비자 3개로 동작한다.
- 소비 중 런타임 오류는 `maxRetries(2)` 설정과 1초, 2초, 최대 4초 backoff 정책을 적용한다.
- 재시도 소진 메시지는 `video.events.dlx` exchange와 `video.notification.failed` routing key를 거쳐 `video.notification.dlq`에 저장한다.
- Outbox 발행은 예외를 기록하고 이벤트를 미발행 상태로 남긴다. 메시지 발행은 at-least-once가 될 수 있으므로 소비자의 멱등 저장이 필수다.

## 3. 데이터 모델

```text
Member 1 ──── N Video
Member 1 ──── N Subscription as subscriber
Member 1 ──── N Subscription as creator
Member 1 ──── N Notification as recipient

Video 1 ──── 1 OutboxEvent
OutboxEvent.eventId + Notification.recipientId = 알림 중복 방지 기준
```

| 테이블 | 주요 컬럼 | 제약·용도 |
| --- | --- | --- |
| `members` | `id`, `name`, `created_at` | 회원 정보. 이름과 생성 시각은 필수 |
| `videos` | `id`, `member_id`, `title`, `description`, `view_count`, `like_count`, `created_at` | 비디오 정보. 작성자와 제목은 필수. 조회·좋아요 수는 0으로 시작 |
| `subscriptions` | `id`, `subscriber_id`, `creator_id` | 회원 간 구독 관계. `(subscriber_id, creator_id)` 유니크 |
| `outbox_events` | `id`, `event_id`, `video_id`, `creator_id`, `video_title`, `occurred_at`, `published_at` | RabbitMQ 발행 대기 이벤트. `event_id` 유니크 |
| `notifications` | `id`, `event_id`, `recipient_id`, `video_id`, `video_title`, `created_at` | 수신자별 알림 이력. `(event_id, recipient_id)` 유니크 |

### 이벤트 스키마

`VideoCreatedEvent`는 JSON 메시지로 변환되어 RabbitMQ에 전달된다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `eventId` | string UUID | Outbox와 멱등 처리의 식별자 |
| `videoId` | long | 생성된 비디오 ID |
| `creatorId` | long | 비디오 작성자 ID |
| `videoTitle` | string | 알림 이력에 저장할 제목 |
| `occurredAt` | LocalDateTime | 비디오 생성 시각 |

## 4. HTTP API

### 공통 규칙

- Base URL은 애플리케이션 루트다.
- 성공 응답은 `ApiResponse<T>`에 따라 `{ "data": T }` 형식이다.
- 현재 Controller는 생성과 취소를 포함한 성공 응답에 `200 OK`를 반환한다.
- Bean Validation 실패와 자기 자신 구독은 `400 Bad Request`를 반환한다.
- 존재하지 않는 회원 또는 비디오는 `404 Not Found`를 반환한다.
- 예외 응답 본문은 별도 전역 포맷을 정의하지 않았으므로 안정된 오류 JSON 계약으로 문서화하지 않는다.

| Method | Endpoint | 요청 | 성공 응답 |
| --- | --- | --- | --- |
| POST | `/members` | `{ "name": "..." }` | 회원 객체 |
| GET | `/members` | 없음 | 회원 객체 배열 |
| GET | `/members/{id}` | 없음 | 회원 객체 |
| POST | `/members/{subscriberId}/subscriptions/{creatorId}` | 없음 | 구독 객체 |
| DELETE | `/members/{subscriberId}/subscriptions/{creatorId}` | 없음 | 본문 없음 |
| POST | `/videos` | `{ "memberId": 1, "title": "...", "description": "..." }` | 비디오 객체 |
| GET | `/videos/{id}` | 없음 | 비디오 객체 |

### 응답 예시

```json
{
  "data": {
    "id": 1,
    "memberId": 1,
    "title": "Spring RabbitMQ",
    "description": "intro",
    "viewCount": 0,
    "likeCount": 0,
    "createdAt": "2026-09-01T10:00:00"
  }
}
```

## 5. 인프라와 운영

### 로컬 구성

`docker compose up -d`는 MySQL, RabbitMQ, Prometheus, Grafana를 시작한다. 애플리케이션은 `./gradlew bootRun`으로 별도 실행한다.

| 구성 요소 | 기본 접속 정보 |
| --- | --- |
| MySQL | `localhost:3306/rabbitmq_notification` |
| RabbitMQ AMQP | `localhost:5672` |
| RabbitMQ 관리 UI | `http://localhost:15672` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

### 설정

- MySQL 연결은 `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`로 재정의한다.
- RabbitMQ 연결은 `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`로 재정의한다.
- Outbox 폴링 주기는 `NOTIFICATION_OUTBOX_FIXED_DELAY`로 재정의하며 기본값은 1,000ms다.
- 관리 endpoint는 `health`, `info`, `prometheus`를 노출한다.
- Prometheus는 `host.docker.internal:8080/actuator/prometheus`를 15초마다 수집한다.

## 6. 테스트 전략

| 범위 | 도구 | 확인 내용 |
| --- | --- | --- |
| 도메인 | JUnit Jupiter | 회원 생성, 비디오 초기 카운터, 자기 구독 거부 |
| 서비스 | JUnit Jupiter와 Mock | 생성·조회·존재하지 않는 리소스·구독 멱등성 |
| Controller | MockMvc | 성공 응답, 검증 실패 400, 리소스 부재 404 |
| 통합 | Spring Boot, Testcontainers | MySQL과 RabbitMQ에서 비디오 생성, 이벤트 재발행, 수신자별 멱등 알림 |
| 부하 | k6 | 1,000명 구독자를 둔 비디오 생성 시나리오의 실패율과 p95 |

통합 테스트는 Docker 데몬이 실행 중이어야 한다. 전체 검증 명령은 `./gradlew test`다.
