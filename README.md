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

## 인프라 종료

```bash
docker compose down
```

데이터까지 초기화하려면 named volume을 포함해 제거합니다.

```bash
docker compose down --volumes
```
