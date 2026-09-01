# 구현 요구사항. 비디오 구독 알림 예제

## 문서 목적

이 문서는 AI 코딩 도구가 이 프로젝트를 변경할 때 따라야 할 현재 구현 기준과 제약을 제공한다. 기능의 비즈니스 목적은 [PRD](PRD.md), 전체 설계와 계약은 [TRD](TRD.md)를 먼저 확인한다.

## 프로젝트 기준선

- Java 26과 Spring Boot 4.1.1 기반의 단일 애플리케이션이다.
- MySQL에 회원, 비디오, 구독, Outbox 이벤트, 알림 이력을 저장한다.
- RabbitMQ로 비디오 생성 이벤트를 발행하고, 소비자가 현재 구독자별 알림 이력을 저장한다.
- 이 문서는 현재 구현된 동작을 기준으로 한다. 사용자 요청 없이 인증, 알림 조회·전달 채널, 비디오 CRUD 확장, 분산 서비스 전환을 추가하지 않는다.

## 기술 스택과 실행

| 구분 | 기술 |
| --- | --- |
| 언어·프레임워크 | Java 26, Spring Boot 4.1.1 |
| 웹·검증 | Spring Web MVC, Bean Validation |
| 데이터 | Spring Data JPA, MySQL 8.4 |
| 메시지 | Spring AMQP, RabbitMQ 4.3.5 |
| 관측성 | Actuator, Micrometer Prometheus, Prometheus, Grafana |
| 테스트 | JUnit Jupiter, MockMvc, Testcontainers |

```bash
docker compose up -d
./gradlew bootRun
./gradlew test
```

테스트는 MySQL과 RabbitMQ Testcontainers를 사용하므로 Docker가 필요하다.

## 구조와 코딩 규칙

```text
src/main/java/dev/backend/rabbitmq_notification/
├── controller/       HTTP 요청과 응답
├── service/          유스케이스와 트랜잭션
├── repository/       JPA 조회·저장
├── domain/           JPA 엔티티와 도메인 규칙
├── dto/              요청·응답·서비스 결과 record
└── notification/     RabbitMQ 설정, Outbox 발행, 이벤트 소비

src/test/java/dev/backend/rabbitmq_notification/
├── domain/
├── service/
├── controller/
└── notification/
```

- Java 파일은 탭으로 들여쓴다.
- 클래스와 record는 PascalCase, 메서드·필드는 camelCase, 패키지는 소문자를 사용한다.
- 새 Java 소스 파일의 첫 줄에는 역할을 설명하는 한 줄 한국어 주석을 둔다.
- Controller는 HTTP 경계만 처리하고, 도메인 규칙과 트랜잭션은 Service에 둔다.
- 기존 코드의 인접한 리팩터링, 포맷 변경, 사용하지 않는 코드 정리는 요청 범위에 포함되지 않으면 하지 않는다.
- 성공 API 응답은 기존 `ApiResponse<T>` 형식인 `{ "data": ... }`를 유지한다.
- 새 공개 API나 데이터 모델을 추가하기 전에는 TRD와 PRD를 함께 갱신한다.

## 유지해야 할 도메인·메시지 계약

### HTTP 계약

| 기능 | 현재 endpoint | 핵심 규칙 |
| --- | --- | --- |
| 회원 | `POST /members`, `GET /members`, `GET /members/{id}` | 이름은 필수 |
| 구독 | `POST`·`DELETE /members/{subscriberId}/subscriptions/{creatorId}` | 자기 구독은 400. 생성·취소는 멱등 |
| 비디오 | `POST /videos`, `GET /videos/{id}` | 작성자 ID와 제목은 필수. 없는 작성자·비디오는 404 |

- 현재 성공 상태 코드는 생성·취소를 포함해 `200 OK`다. 요청 없이 `201`이나 `204`로 변경하지 않는다.
- Validation 실패는 `400 Bad Request`, 없는 회원·비디오는 `404 Not Found`를 유지한다.
- 전역 오류 응답 스키마는 아직 계약으로 정의되어 있지 않다. 오류 형식을 추가할 때는 기존 테스트와 API 문서를 함께 변경한다.

### Outbox와 RabbitMQ 계약

- 비디오와 `OutboxEvent.videoCreated(video)`는 하나의 Service 트랜잭션에서 저장해야 한다.
- 미발행 이벤트는 `published_at`이 비어 있는 `outbox_events` 레코드다.
- 발행 exchange는 `video.events`, routing key는 `video.created`, 수신 큐는 `video.notification`이다.
- publisher confirm ACK를 받은 뒤에만 `published_at`을 기록한다.
- 이벤트 payload는 `eventId`, `videoId`, `creatorId`, `videoTitle`, `occurredAt` 필드를 유지한다.
- 소비자는 이벤트 처리 시점의 구독자를 조회한다.
- `notifications`의 `(event_id, recipient_id)` 유니크 제약과 `INSERT IGNORE` 멱등 삽입을 제거하거나 우회하지 않는다.
- 소비 실패는 현재 재시도 정책을 거쳐 `video.notification.dlq`로 보낸다. 재시도·DLQ 변경은 실패와 재전달 테스트를 반드시 추가한다.

## 테스트 요구사항

기능을 변경하면 가장 작은 관련 테스트부터 실행하고, 메시지·API·영속성 변경은 전체 테스트까지 실행한다.

| 변경 범위 | 최소 검증 |
| --- | --- |
| 도메인 규칙 | 해당 `domain` 단위 테스트 |
| Service 동작 | 해당 `service` 단위 테스트와 성공·실패 경로 |
| HTTP API | 해당 `controller` MockMvc 테스트와 400·404 경로 |
| Outbox·RabbitMQ | `VideoNotificationConsumerTest` 및 재발행 멱등 통합 테스트 |
| 스키마·전역 설정 | `./gradlew test` |

- 테스트 이름은 관찰 가능한 동작을 설명하는 영어 camelCase를 사용한다.
- 성공, 입력 검증 실패, 리소스 부재를 함께 검증한다.
- 메시지 재전달이 가능한 변경은 동일 이벤트와 수신자 조합이 한 건만 남는지 검증한다.
- 테스트 결과를 최종 응답에 실제 실행 명령과 함께 보고한다.

## 금지 사항과 운영 제약

- 사용자 요청 없이 인증·인가, 알림 조회 API, 외부 푸시 전송, 비디오 수정·삭제 기능을 추가하지 않는다.
- 자격 증명과 운영 비밀을 커밋하지 않는다. 접속 설정은 기존 환경 변수를 사용한다.
- RabbitMQ confirm 전에 Outbox 이벤트를 발행 완료로 표시하지 않는다.
- 중복 이벤트를 전제로 한 소비자 멱등성을 약화시키지 않는다.
- Docker가 필요한 통합 테스트를 실행하지 못한 경우, 통과했다고 주장하지 않고 사유를 보고한다.

## 작업 완료 기준

1. 변경 범위가 PRD와 TRD의 현재 계약에 부합한다.
2. 관련 단위·Controller·통합 테스트가 추가 또는 갱신된다.
3. 필요한 검증 명령이 통과한다.
4. 문서 또는 공개 API 계약을 변경했다면 PRD, TRD, REQUIREMENTS를 같은 변경에 반영한다.
