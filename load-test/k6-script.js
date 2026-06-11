import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const errorRate = new Rate('errors');

export const options = {
  scenarios: {
   warmup: {
      executor: 'constant-vus',
      vus: 50,
      duration: '2m',
    },
    steady_load: {
      executor: 'constant-arrival-rate',
      rate: 250,          // 250 TPS
      timeUnit: '1s',
      duration: '15m',    // start with 10 min test first
      preAllocatedVUs: 800,
      maxVUs: 2000,
      gracefulStop: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function randomId() {
  return Math.floor(Math.random() * 1000000);
}

export default function () {
  const payload = JSON.stringify({
    idempotencyKey: uuidv4(),
    senderId: "a0000000-0000-0000-0000-000000000001",
    receiverId: "a0000000-0000-0000-0000-000000000002",
    amount: (Math.random() * 10 + 1).toFixed(2),
    currency: "USD"
  });

  const res = http.post(`${BASE_URL}/v1/payments`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const ok = check(res, {
    'status is 202': (r) => r.status === 202,
  });

  errorRate.add(!ok);
}