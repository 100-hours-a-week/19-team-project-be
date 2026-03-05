import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DEV_TOKEN_URL = __ENV.DEV_TOKEN_URL || `${BASE_URL}/api/v1/auth/dev/token`;
function parseNumberCsv(value) {
  return (value || '')
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v.length > 0)
    .map((v) => Number(v))
    .filter((v) => Number.isFinite(v));
}

function parseRange(startRaw, endRaw) {
  const start = Number(startRaw);
  const end = Number(endRaw);
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) return [];
  const out = [];
  for (let i = start; i <= end; i += 1) out.push(i);
  return out;
}

const USER_IDS_CSV = parseNumberCsv(__ENV.USER_IDS || '');
const USER_IDS_RANGE = parseRange(__ENV.USER_ID_START, __ENV.USER_ID_END);
const USER_IDS = Array.from(new Set([...USER_IDS_CSV, ...USER_IDS_RANGE]));
const TOKENS = (__ENV.TOKENS || '').split(',').map((v) => v.trim()).filter((v) => v.length > 0);
const CHAT_IDS = parseNumberCsv(__ENV.CHAT_IDS || '');
const REST_SIZE = Number(__ENV.REST_SIZE || 20);
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || 0.1);
const WS_SESSION_TIMEOUT_MS = Number(__ENV.WS_SESSION_TIMEOUT_MS || 12000);
const WS_MESSAGES_PER_SESSION = Number(__ENV.WS_MESSAGES_PER_SESSION || 5);
const WS_SEND_INTERVAL_MS = Number(__ENV.WS_SEND_INTERVAL_MS || 150);
const MESSAGE_TEXT = __ENV.MESSAGE_TEXT || 'k6 burst test';

const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 20);
const PEAK_VUS = Number(__ENV.PEAK_VUS || 250);
const SUSTAIN_VUS = Number(__ENV.SUSTAIN_VUS || 120);
const WARMUP_DURATION = __ENV.WARMUP_DURATION || '30s';
const BURST_DURATION = __ENV.BURST_DURATION || '20s';
const SUSTAIN_DURATION = __ENV.SUSTAIN_DURATION || '90s';
const COOL_DURATION = __ENV.COOL_DURATION || '20s';
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '10m';
const DEV_TOKEN_RETRIES = Number(__ENV.DEV_TOKEN_RETRIES || 5);
const DEV_TOKEN_BACKOFF_MS = Number(__ENV.DEV_TOKEN_BACKOFF_MS || 300);
const DEV_TOKEN_DELAY_MS = Number(__ENV.DEV_TOKEN_DELAY_MS || 20);
const DEV_TOKEN_MAX_BACKOFF_MS = Number(__ENV.DEV_TOKEN_MAX_BACKOFF_MS || 5000);
const DEV_TOKEN_SKIP_ON_FAIL = String(__ENV.DEV_TOKEN_SKIP_ON_FAIL || 'true') === 'true';
const MIN_ISSUED_TOKENS = Number(__ENV.MIN_ISSUED_TOKENS || 10);

const wsConnectSuccess = new Rate('ws_connect_success');
const stompConnected = new Rate('stomp_connected');
const wsMessageReceived = new Rate('ws_message_received');
const wsRoundTripMs = new Trend('ws_round_trip_ms');
const wsSentCount = new Counter('ws_sent_count');

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  stages: [
    { duration: WARMUP_DURATION, target: WARMUP_VUS },
    { duration: BURST_DURATION, target: PEAK_VUS },
    { duration: SUSTAIN_DURATION, target: SUSTAIN_VUS },
    { duration: COOL_DURATION, target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<3000', 'p(99)<6000'],
    ws_connect_success: ['rate>0.95'],
    stomp_connected: ['rate>0.95'],
    ws_message_received: ['rate>0.90'],
    ws_round_trip_ms: ['p(95)<3000', 'p(99)<6000'],
  },
};

function toWsUrl(baseUrl) {
  if (baseUrl.startsWith('https://')) return `${baseUrl.replace('https://', 'wss://')}/ws`;
  if (baseUrl.startsWith('http://')) return `${baseUrl.replace('http://', 'ws://')}/ws`;
  return `${baseUrl}/ws`;
}

function authHeaders(token) {
  return { Authorization: `Bearer ${token}` };
}

function stompConnectFrame(token) {
  return `CONNECT
accept-version:1.2
heart-beat:0,0
Authorization:Bearer ${token}

\u0000`;
}

function stompSubscribeFrame(chatId, subId) {
  return `SUBSCRIBE
id:${subId}
destination:/queue/chat.${chatId}
ack:auto

\u0000`;
}

function stompSendFrame(chatId, content, clientMessageId) {
  const payload = JSON.stringify({
    chat_id: chatId,
    content,
    message_type: 'TEXT',
    client_message_id: clientMessageId,
  });
  const byteLength = encodeURIComponent(payload).replace(/%[A-F\d]{2}/g, 'U').length;
  return `SEND
destination:/app/chat.sendMessage
content-type:application/json
content-length:${byteLength}

${payload}\u0000`;
}

function parseStompFrames(raw) {
  const text = String(raw || '');
  return text.split('\u0000').map((chunk) => chunk.trim()).filter((chunk) => chunk.length > 0);
}

function extractBody(frame) {
  const idx = frame.indexOf('\n\n');
  if (idx < 0) return '';
  return frame.substring(idx + 2);
}

function extractAccessToken(parsed) {
  if (!parsed || typeof parsed !== 'object') return null;
  if (typeof parsed.access_token === 'string') return parsed.access_token;
  if (typeof parsed.accessToken === 'string') return parsed.accessToken;
  if (parsed.data && typeof parsed.data === 'object') {
    if (typeof parsed.data.access_token === 'string') return parsed.data.access_token;
    if (typeof parsed.data.accessToken === 'string') return parsed.data.accessToken;
  }
  return null;
}

function issueDevTokens() {
  const issued = [];
  const issuedUserIds = [];
  const failedUserIds = [];
  for (const userId of USER_IDS) {
    let res = null;
    for (let attempt = 0; attempt <= DEV_TOKEN_RETRIES; attempt += 1) {
      res = http.post(
        DEV_TOKEN_URL,
        JSON.stringify({ user_id: userId }),
        {
          headers: { 'Content-Type': 'application/json' },
          tags: { name: 'dev_token_issue' },
        },
      );
      if (res && res.status === 200) break;
      if (!res || res.status !== 429 || attempt === DEV_TOKEN_RETRIES) break;
      const waitMs = Math.min(
        DEV_TOKEN_MAX_BACKOFF_MS,
        DEV_TOKEN_BACKOFF_MS * (2 ** attempt),
      );
      sleep(waitMs / 1000);
    }
    const ok = check(res, { 'dev token issue 200': (r) => r.status === 200 });
    if (!ok) {
      failedUserIds.push(userId);
      if (!DEV_TOKEN_SKIP_ON_FAIL) {
        throw new Error(`failed to issue token for user_id=${userId}. body=${res && res.body}`);
      }
      if (DEV_TOKEN_DELAY_MS > 0) sleep(DEV_TOKEN_DELAY_MS / 1000);
      continue;
    }

    let parsed;
    try {
      parsed = res.json();
    } catch (_) {
      parsed = null;
    }
    const token = extractAccessToken(parsed);
    if (!token) {
      failedUserIds.push(userId);
      if (!DEV_TOKEN_SKIP_ON_FAIL) {
        throw new Error(`failed to parse access token for user_id=${userId}. body=${res.body}`);
      }
      if (DEV_TOKEN_DELAY_MS > 0) sleep(DEV_TOKEN_DELAY_MS / 1000);
      continue;
    }
    issued.push(token);
    issuedUserIds.push(userId);
    if (DEV_TOKEN_DELAY_MS > 0) sleep(DEV_TOKEN_DELAY_MS / 1000);
  }
  if (issued.length < MIN_ISSUED_TOKENS) {
    throw new Error(
      `issued tokens too few: issued=${issued.length}, min=${MIN_ISSUED_TOKENS}, failed=${failedUserIds.length}`,
    );
  }
  return { tokens: issued, userIds: issuedUserIds, failedUserIds };
}

let runtimeTokens = TOKENS;
let runtimeChatIds = CHAT_IDS;
let runtimeTokenChatPairs = [];

function fetchActiveChatIdsForToken(baseUrl, token, filterChatIds) {
  const listRes = http.get(`${baseUrl}/api/v1/chats?status=ACTIVE&size=500`, {
    headers: authHeaders(token),
    tags: { name: 'chat_list_for_mapping' },
  });
  if (listRes.status !== 200) return [];

  let body;
  try {
    body = listRes.json();
  } catch (_) {
    body = null;
  }
  const chats = body?.data?.chats || [];
  const ids = chats.map((c) => c?.chat_id).filter((v) => Number.isFinite(v));
  if (!filterChatIds || filterChatIds.length === 0) return ids;
  const allow = new Set(filterChatIds);
  return ids.filter((id) => allow.has(id));
}

export function setup() {
  const tokenChatPairs = [];
  if (runtimeTokens.length === 0) {
    if (USER_IDS.length === 0) {
      throw new Error('TOKENS or USER_IDS/USER_ID_START+USER_ID_END is required.');
    }
    const issued = issueDevTokens();
    runtimeTokens = issued.tokens;
    if (issued.userIds && issued.userIds.length > 0) {
      for (let i = 0; i < issued.userIds.length; i += 1) {
        USER_IDS[i] = issued.userIds[i];
      }
      USER_IDS.length = issued.userIds.length;
    }
  }
  for (let i = 0; i < runtimeTokens.length; i++) {
    const token = runtimeTokens[i];
    const userId = USER_IDS[i] || null;
    const chats = fetchActiveChatIdsForToken(BASE_URL, token, runtimeChatIds);
    if (chats.length > 0) {
      tokenChatPairs.push({ token, userId, chats });
    }
  }

  if (tokenChatPairs.length === 0 && runtimeChatIds.length === 0) {
    throw new Error('No ACTIVE chat rooms found for provided users/tokens.');
  }
  return { tokens: runtimeTokens, chatIds: runtimeChatIds, tokenChatPairs };
}

function pickToken() {
  return runtimeTokens[(__VU - 1) % runtimeTokens.length];
}

function pickChatId() {
  if (runtimeChatIds.length === 0) {
    throw new Error('CHAT_IDS is empty and token-chat mapping is unavailable.');
  }
  return runtimeChatIds[(__VU + __ITER) % runtimeChatIds.length];
}

function runRestFlow(baseUrl, token, chatId) {
  const headers = authHeaders(token);

  const listRes = http.get(`${baseUrl}/api/v1/chats?status=ACTIVE&size=${REST_SIZE}`, {
    headers,
    tags: { name: 'chat_list' },
  });
  check(listRes, { 'chat list 200': (r) => r.status === 200 });

  const msgRes = http.get(`${baseUrl}/api/v1/chats/${chatId}/messages?size=${REST_SIZE}`, {
    headers,
    tags: { name: 'chat_messages' },
  });
  check(msgRes, { 'chat messages 200': (r) => r.status === 200 });
}

function runWsFlow(baseUrl, token, chatId) {
  const wsUrl = toWsUrl(baseUrl);

  const connectRes = ws.connect(wsUrl, {}, (socket) => {
    let connectOk = false;
    let sendTimer = null;
    let sentCount = 0;
    let recvCount = 0;
    const sentAtByClientId = {};
    const pendingClientIds = new Set();
    const subId = `sub-${__VU}-${__ITER}`;

    socket.on('open', () => {
      wsConnectSuccess.add(true);
      socket.send(stompConnectFrame(token));
    });

    socket.on('message', (raw) => {
      const frames = parseStompFrames(raw);
      for (const frame of frames) {
        if (frame.startsWith('CONNECTED')) {
          connectOk = true;
          stompConnected.add(true);
          socket.send(stompSubscribeFrame(chatId, subId));

          // 지속 연결형: 한 세션에서 여러 메시지를 간격 전송하여 CONNECT 폭풍을 완화
          sendTimer = socket.setInterval(() => {
            if (sentCount >= WS_MESSAGES_PER_SESSION) return;
            const clientMessageId = `k6b-${__VU}-${__ITER}-${Date.now()}-${sentCount}`;
            sentAtByClientId[clientMessageId] = Date.now();
            pendingClientIds.add(clientMessageId);
            socket.send(stompSendFrame(chatId, MESSAGE_TEXT, clientMessageId));
            wsSentCount.add(1);
            sentCount += 1;
          }, WS_SEND_INTERVAL_MS);
          continue;
        }

        if (frame.startsWith('MESSAGE')) {
          const body = extractBody(frame);
          for (const clientId of Array.from(pendingClientIds)) {
            if (body.includes(clientId)) {
              pendingClientIds.delete(clientId);
              recvCount += 1;
              wsMessageReceived.add(true);
              const sentAt = sentAtByClientId[clientId];
              if (sentAt > 0) wsRoundTripMs.add(Date.now() - sentAt);
            }
          }

          if (sentCount >= WS_MESSAGES_PER_SESSION && pendingClientIds.size === 0) {
            if (sendTimer) socket.clearInterval(sendTimer);
            socket.close();
            return;
          }
        }

        if (frame.startsWith('ERROR')) {
          if (sendTimer) socket.clearInterval(sendTimer);
          socket.close();
          return;
        }
      }
    });

    socket.on('close', () => {
      if (sendTimer) socket.clearInterval(sendTimer);
      if (!connectOk) {
        stompConnected.add(false);
        wsMessageReceived.add(false);
        return;
      }

      // 세션 내 미수신이 있으면 실패로 기록
      if (sentCount > 0 && recvCount < sentCount) {
        wsMessageReceived.add(false);
      }
    });

    socket.on('error', () => {
      if (sendTimer) socket.clearInterval(sendTimer);
      wsConnectSuccess.add(false);
      stompConnected.add(false);
      wsMessageReceived.add(false);
    });

    socket.setTimeout(() => {
      if (sendTimer) socket.clearInterval(sendTimer);
      if (!connectOk) {
        wsConnectSuccess.add(false);
        stompConnected.add(false);
      } else if (sentCount === 0 || recvCount < sentCount) {
        wsMessageReceived.add(false);
      }
      socket.close();
    }, WS_SESSION_TIMEOUT_MS);
  });

  check(connectRes, { 'ws connect status 101': (r) => r && r.status === 101 });
}

export default function (data) {
  runtimeTokens = data.tokens;
  runtimeChatIds = data.chatIds;
  runtimeTokenChatPairs = data.tokenChatPairs || [];

  let token;
  let chatId;
  if (runtimeTokenChatPairs.length > 0) {
    const pair = runtimeTokenChatPairs[(__VU + __ITER) % runtimeTokenChatPairs.length];
    token = pair.token;
    chatId = pair.chats[(__ITER + __VU) % pair.chats.length];
  } else {
    token = pickToken();
    chatId = pickChatId();
  }

  runRestFlow(BASE_URL, token, chatId);
  runWsFlow(BASE_URL, token, chatId);
  sleep(THINK_TIME_SEC);
}
