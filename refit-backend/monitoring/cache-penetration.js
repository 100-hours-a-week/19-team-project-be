import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SIZE = Number(__ENV.SIZE || 10);
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || 0.1);
const MISS_KEYWORDS = (__ENV.MISS_KEYWORDS || 'naver,zzzz-no-match,unlikely-keyword-12345')
  .split(',')
  .map((v) => v.trim())
  .filter((v) => v.length > 0);
const HIT_KEYWORD = __ENV.HIT_KEYWORD || 'java';
const MISS_RATIO = Number(__ENV.MISS_RATIO || 0.9);

const missRequestCount = new Counter('cache_penetration_miss_request_count');
const missZeroResultCount = new Counter('cache_penetration_zero_result_count');
const missReqDuration = new Trend('cache_penetration_miss_req_duration');
const hitReqDuration = new Trend('cache_penetration_hit_req_duration');
const missReqFailed = new Rate('cache_penetration_miss_req_failed');

export const options = {
  scenarios: {
    penetration_mixed: {
      executor: 'ramping-vus',
      startVUs: Number(__ENV.START_VUS || 10),
      stages: [
        { duration: __ENV.WARMUP_DURATION || '30s', target: Number(__ENV.WARMUP_VUS || 50) },
        { duration: __ENV.SUSTAIN_DURATION || '1m', target: Number(__ENV.PEAK_VUS || 150) },
        { duration: __ENV.COOL_DURATION || '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<3000', 'p(99)<5000'],
    cache_penetration_miss_req_failed: ['rate<0.02'],
    cache_penetration_miss_req_duration: ['p(95)<3000', 'p(99)<5000'],
  },
};

function buildSearchUrl(keyword) {
  return `${BASE_URL}/api/v1/experts?keyword=${encodeURIComponent(keyword)}&size=${encodeURIComponent(SIZE)}`;
}

function pickMissKeyword() {
  return MISS_KEYWORDS[(__VU + __ITER) % MISS_KEYWORDS.length];
}

function extractCount(res) {
  try {
    const body = res.json();
    return body?.data?.experts?.length ?? body?.data?.items?.length ?? 0;
  } catch (_) {
    return null;
  }
}

export default function () {
  const useMiss = Math.random() < MISS_RATIO;
  const keyword = useMiss ? pickMissKeyword() : HIT_KEYWORD;
  const tags = {
    name: useMiss ? 'cache_penetration_miss' : 'cache_penetration_hit',
    keyword,
    cache_case: useMiss ? 'miss' : 'hit',
  };

  const res = http.get(buildSearchUrl(keyword), { tags });
  const ok = check(res, { 'search status is 200': (r) => r.status === 200 });
  const count = extractCount(res);

  if (useMiss) {
    missRequestCount.add(1);
    missReqDuration.add(res.timings.duration);
    missReqFailed.add(!ok);
    if (count === 0) {
      missZeroResultCount.add(1);
    }
  } else {
    hitReqDuration.add(res.timings.duration);
  }

  sleep(THINK_TIME_SEC);
}
