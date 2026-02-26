import http from 'k6/http';
import { check, sleep } from 'k6';

const BACKEND_URL = __ENV.BACKEND_URL || 'http://localhost:8080';
const KEYWORD = __ENV.KEYWORD || 'java';
const JOB_ID = __ENV.JOB_ID || '';
const SKILL_ID = __ENV.SKILL_ID || '';
const SIZE = __ENV.SIZE || 10;
const VUS = Number(__ENV.VUS || 30);
const DURATION = __ENV.DURATION || '1m';
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || 0.2);

export const options = {
  vus: VUS,
  duration: DURATION,
  thresholds: {
    http_req_duration: ['p(95)<10000'],
    http_req_failed: ['rate<0.01'],
  },
};

function buildQuery(keyword) {
  const params = [];
  if (keyword) params.push(`keyword=${encodeURIComponent(keyword)}`);
  if (JOB_ID) params.push(`job_id=${encodeURIComponent(JOB_ID)}`);
  if (SKILL_ID) params.push(`skill_id=${encodeURIComponent(SKILL_ID)}`);
  params.push(`size=${encodeURIComponent(SIZE)}`);
  return params.join('&');
}

export default function () {
  const tags = { name: 'expert_search_only', keyword: KEYWORD };
  const url = `${BACKEND_URL}/api/v1/experts?${buildQuery(KEYWORD)}`;
  const res = http.get(url, { tags });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(THINK_TIME_SEC);
}
