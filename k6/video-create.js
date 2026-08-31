// 비디오 생성 API의 RabbitMQ 비동기 알림 분리 성능을 검증하는 k6 시나리오.
import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const creatorId = Number(__ENV.CREATOR_ID || 1);

export const options = {
	scenarios: {
		video_create: {
			executor: 'constant-vus',
			vus: 30,
			duration: '1m',
		},
	},
	thresholds: {
		'http_req_failed{endpoint:video-create}': ['rate<0.01'],
		'http_req_duration{endpoint:video-create}': ['p(95)<150'],
	},
};

export default function () {
	const response = http.post(
		`${baseUrl}/videos`,
		JSON.stringify({
			memberId: creatorId,
			title: `load-video-${__VU}-${__ITER}`,
			description: 'RabbitMQ async notification load test',
		}),
		{
			headers: { 'Content-Type': 'application/json' },
			tags: { endpoint: 'video-create' },
		}
	);
	check(response, { '비디오 생성 성공': (result) => result.status === 200 });
	sleep(0.1);
}
