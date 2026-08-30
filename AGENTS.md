# Repository Guidelines

## Project Structure & Module Organization

This is a Java 26 Spring Boot application that demonstrates notification-related
services backed by MySQL and RabbitMQ. Application code lives in
`src/main/java/dev/backend/rabbitmq_notification`, organized into `controller`,
`service`, `repository`, and domain packages such as `domain/member` and
`domain/video`. Keep new code in the layer and domain it serves. Runtime
configuration is in `src/main/resources/application.properties`. Integration
tests are in `src/test/java`, while Prometheus and Grafana provisioning files
live under `monitoring/`.

## Build, Test, and Development Commands

- `docker compose up -d` starts MySQL, RabbitMQ, Prometheus, and Grafana for
  local development.
- `./gradlew bootRun` starts the Spring Boot application on port 8080.
- `./gradlew test` runs the JUnit test suite, including MySQL Testcontainers.
- `docker compose down` stops local infrastructure; add `--volumes` only when
  intentionally removing persisted local data.

Use the Gradle wrapper rather than a locally installed Gradle version. Docker
must be running before executing integration tests.

## Coding Style & Naming Conventions

Use Java 26 and follow the existing Spring conventions. Indent Java code with
tabs, use PascalCase for classes and records, camelCase for methods and fields,
and lowercase package names. Name tests as behavior, for example
`memberCanBeCreatedAndRetrieved` or `missingResourcesReturnNotFound`.

Place a short Korean comment on the first line of each new Java source file
describing its role. Prefer focused controllers, services, repositories, and
JPA entities over cross-layer shortcuts. No formatter or linter is configured;
match the surrounding code instead of reformatting unrelated files.

## Testing Guidelines

Write JUnit Jupiter tests for user-visible behavior. The current suite uses
`@SpringBootTest`, `MockMvc`, and a MySQL Testcontainer, so exercise HTTP
requests and validate status codes and response fields. Cover success paths,
validation failures, and missing-resource behavior when changing an endpoint.
Run `./gradlew test` before opening a pull request and report the exact result.

## Commits, Pull Requests, and Configuration

Recent commits use short, imperative Korean summaries such as `RabbitMQ 알림
인프라 구성 추가`. Keep each commit limited to one logical change. Pull requests
should explain the change, link relevant issues when available, list validation
performed, and include screenshots or API examples for observable changes.

Do not commit credentials. Configure local database, RabbitMQ, and monitoring
settings through the environment variables documented in `README.md`; the
checked-in defaults are for local development only.
