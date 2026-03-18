import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HOT_KEYWORD = __ENV.HOT_KEYWORD || 'java';
const SIZE = Number(__ENV.SIZE || 10);
const PREWARM_REQUESTS = Number(__ENV.PREWARM_REQUESTS || 5);
const CACHE_WAIT_SEC = Number(__ENV.CACHE_WAIT_SEC || 0);
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || 0);

const burstReqDuration = new Trend('cache_stampede_burst_req_duration');
const burstReqFailed = new Rate('cache_stampede_burst_req_failed');
const burstReqCount = new Counter('cache_stampede_burst_req_count');

export const options = {
  scenarios: {
    stampede_burst: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.BURST_RATE || 200),
      timeUnit: '1s',
      duration: __ENV.BURST_DURATION || '20s',
      preAllocatedVUs: Number(__ENV.PREALLOCATED_VUS || 200),
      maxVUs: Number(__ENV.MAX_VUS || 400),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<3000', 'p(99)<5000'],
    cache_stampede_burst_req_failed: ['rate<0.02'],
    cache_stampede_burst_req_duration: ['p(95)<3000', 'p(99)<5000'],
  },
};

function buildSearchUrl() {
  return `${BASE_URL}/api/v1/experts?keyword=${encodeURIComponent(HOT_KEYWORD)}&size=${encodeURIComponent(SIZE)}`;
}

export function setup() {
  const url = buildSearchUrl();
  for (let i = 0; i < PREWARM_REQUESTS; i += 1) {
    const res = http.get(url, {
      tags: {
        name: 'cache_stampede_prewarm',
        keyword: HOT_KEYWORD,
      },
    });
    check(res, { 'prewarm status is 200': (r) => r.status === 200 });
  }

  if (CACHE_WAIT_SEC > 0) {
    sleep(CACHE_WAIT_SEC);
  }

  return { warmedKeyword: HOT_KEYWORD };
}

export default function (data) {
  const res = http.get(buildSearchUrl(), {
    tags: {
      name: 'cache_stampede_burst',
      keyword: data.warmedKeyword,
      scenario: exec.scenario.name,
    },
  });

  const ok = check(res, { 'burst status is 200': (r) => r.status === 200 });
  burstReqCount.add(1);
  burstReqDuration.add(res.timings.duration);
  burstReqFailed.add(!ok);

  if (THINK_TIME_SEC > 0) {
    sleep(THINK_TIME_SEC);
  }
}
