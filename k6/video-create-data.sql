-- k6 비디오 생성 부하 테스트에 필요한 크리에이터와 구독 데이터를 생성한다.
INSERT INTO members (id, name, created_at)
VALUES (1, 'load-creator', CURRENT_TIMESTAMP);

INSERT INTO members (id, name, created_at)
WITH RECURSIVE sequence AS (
	SELECT 1 AS number
	UNION ALL
	SELECT number + 1
	FROM sequence
	WHERE number < 1000
)
SELECT number + 1, CONCAT('load-subscriber-', number), CURRENT_TIMESTAMP
FROM sequence;

INSERT INTO subscriptions (subscriber_id, creator_id)
WITH RECURSIVE sequence AS (
	SELECT 1 AS number
	UNION ALL
	SELECT number + 1
	FROM sequence
	WHERE number < 1000
)
SELECT number + 1, 1
FROM sequence;
