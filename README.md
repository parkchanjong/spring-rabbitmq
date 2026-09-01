# rabbitmq-notification

로컬 Spring Boot 애플리케이션이 MySQL, RabbitMQ, Actuator Prometheus 메트릭과 연동되는 예제 프로젝트입니다.

## 사전 요구 사항

- Java 26
- Docker 및 Docker Compose

## 로컬 인프라 실행

```bash
docker compose up -d
```

애플리케이션은 별도로 실행합니다.

```bash
./gradlew bootRun
```

기본 개발 설정은 아래와 같습니다. 환경 변수를 지정하면 값을 변경할 수 있습니다.

| 구성 요소 | 기본 접속 정보 |
| --- | --- |
| MySQL | `localhost:3306/rabbitmq_notification`, 사용자 `notification`, 비밀번호 `notification` |
| RabbitMQ | AMQP `localhost:5672`, 사용자 `notification`, 비밀번호 `notification` |
| RabbitMQ 관리 UI | http://localhost:15672 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000, 사용자 `admin`, 비밀번호 `admin` |

MySQL은 `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MYSQL_PORT`로 재정의할 수 있습니다. RabbitMQ는 `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`, `RABBITMQ_PORT`, `RABBITMQ_MANAGEMENT_PORT`를 사용합니다. Grafana와 Prometheus 포트는 각각 `GRAFANA_PORT`, `PROMETHEUS_PORT`로 변경할 수 있습니다.

## 모니터링 확인

애플리케이션 실행 후 다음 엔드포인트로 상태와 Prometheus 형식 메트릭을 확인합니다.

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/prometheus
```

Prometheus의 Status > Targets에서 `rabbitmq-notification` 대상이 `UP`인지 확인합니다. Grafana는 시작 시 Prometheus 데이터 소스를 자동으로 등록합니다.

## 비디오 생성 흐름

`POST /videos` 요청은 작성자 ID(`memberId`), 제목, 설명을 받습니다. 생성 로직은 다음 순서로 동작합니다.

1. `memberId`로 작성자를 조회하고, 존재하지 않으면 `404 Not Found`를 반환합니다.
2. 조회한 작성자와 연결된 비디오를 저장합니다.
3. 저장된 비디오의 ID, 작성자 ID, 제목, 생성 시각을 담은 Outbox 이벤트를 기록합니다.
4. 비디오 정보와 생성 결과를 응답으로 반환합니다.

비디오와 Outbox 이벤트는 하나의 트랜잭션으로 저장합니다. 따라서 비디오만 저장되고 알림 발행을 위한 이벤트가 누락되는 상황을 방지하며, 커밋된 이벤트는 이후 비동기 알림 처리로 전달됩니다.

## 비디오 알림 비동기 처리

`POST /videos`는 비디오와 Outbox 이벤트만 같은 트랜잭션으로 저장합니다. 별도 발행기가 RabbitMQ의 `video.events` Exchange로 이벤트를 발행하고, Consumer가 소비 시점의 구독자에게 수신자별 알림 이력을 저장합니다.

Outbox 발행은 RabbitMQ publisher confirm을 받은 뒤 완료 처리하므로 브로커 장애 중에도 이벤트가 남습니다. 발행 완료 처리 전에 프로세스가 중단되면 이벤트가 재발행될 수 있으며, `notifications` 테이블의 `(event_id, recipient_id)` 유니크 제약이 중복 알림 이력을 막습니다. Consumer 저장 실패는 총 3회 시도한 뒤 `video.notification.dlq`로 이동합니다.

## k6 부하 테스트

애플리케이션과 로컬 인프라를 실행한 뒤, 빈 로컬 MySQL DB에 시드 데이터를 주입하고 k6를 실행합니다.

```bash
docker compose exec -T mysql mysql --user="${MYSQL_USERNAME:-notification}" --password="${MYSQL_PASSWORD:-notification}" "${MYSQL_DATABASE:-rabbitmq_notification}" < k6/video-create-data.sql
k6 run k6/video-create.js
```

시드는 크리에이터 ID `1`과 구독자 1,000명, 구독 관계 1,000건을 생성합니다. 이미 데이터가 있는 DB에는 실행하지 말고, 원격 환경처럼 크리에이터 ID가 다를 때는 `CREATOR_ID`를 지정합니다.

```bash
K6_WEB_DASHBOARD=true CREATOR_ID=42 k6 run k6/video-create.js
```

스크립트는 30 VU가 1분 동안 비디오를 생성하며, 비디오 생성 요청의 실패율 1% 미만과 p95 150ms 미만을 통과 기준으로 검사합니다. 실행마다 비디오와 알림 데이터가 추가됩니다.

## 인프라 종료

```bash
docker compose down
```

데이터까지 초기화하려면 named volume을 포함해 제거합니다.

```bash
docker compose down --volumes
```
