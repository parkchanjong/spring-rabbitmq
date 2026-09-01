// 비디오 생성 API의 RabbitMQ 비동기 알림 분리 성능을 검증하는 k6 시나리오.
import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const seedSubscriberCount = 1000;
const seedRequestParams = {
	headers: { 'Content-Type': 'application/json' },
	tags: { endpoint: 'seed' },
};

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

export function setup() {
	if (__ENV.CREATOR_ID) {
		const creatorId = Number(__ENV.CREATOR_ID);
		if (!Number.isSafeInteger(creatorId) || creatorId < 1) {
			throw new Error('CREATOR_ID는 1 이상의 정수여야 합니다.');
		}
		return { creatorId };
	}

	const creatorId = createMember('load-creator');
	for (let index = 1; index <= seedSubscriberCount; index += 1) {
		const subscriberId = createMember(`load-subscriber-${index}`);
		createSubscription(subscriberId, creatorId);
	}
	return { creatorId };
}

export default function ({ creatorId }) {
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

function createMember(name) {
	const response = http.post(
		`${baseUrl}/members`,
		JSON.stringify({ name }),
		seedRequestParams
	);
	return responseId(response, `회원 생성 실패. name=${name}`);
}

function createSubscription(subscriberId, creatorId) {
	const response = http.post(
		`${baseUrl}/members/${subscriberId}/subscriptions/${creatorId}`,
		null,
		seedRequestParams
	);
	if (response.status !== 200) {
		throw new Error(
			`구독 생성 실패. subscriberId=${subscriberId}, creatorId=${creatorId}, status=${response.status}, body=${response.body}`
		);
	}
}

function responseId(response, errorMessage) {
	if (response.status !== 200) {
		throw new Error(`${errorMessage}, status=${response.status}, body=${response.body}`);
	}

	const id = response.json('data.id');
	if (!Number.isSafeInteger(id) || id < 1) {
		throw new Error(`${errorMessage}, 응답에 유효한 회원 ID가 없습니다. body=${response.body}`);
	}
	return id;
}
