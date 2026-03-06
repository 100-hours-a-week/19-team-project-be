import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DEV_TOKEN_URL = __ENV.DEV_TOKEN_URL || `${BASE_URL}/api/v1/auth/dev/token`;
const SIGNUP_URL = __ENV.SIGNUP_URL || `${BASE_URL}/api/v1/auth/signup`;
const AUTO_SIGNUP = (__ENV.AUTO_SIGNUP || 'false').toLowerCase() === 'true';
const SIGNUP_SEEKER_COUNT = Number(__ENV.SIGNUP_SEEKER_COUNT || 4);
const SIGNUP_EXPERT_COUNT = Number(__ENV.SIGNUP_EXPERT_COUNT || 2);
const SIGNUP_CAREER_LEVEL_ID = Number(__ENV.SIGNUP_CAREER_LEVEL_ID || 1);
const SIGNUP_PREFIX = __ENV.SIGNUP_PREFIX || 'k6c';
const USER_IDS = (__ENV.USER_IDS || '').split(',').map((v) => Number(v.trim())).filter((v) => Number.isFinite(v));
const REQUESTER_IDS = (__ENV.REQUESTER_IDS || '').split(',').map((v) => Number(v.trim())).filter((v) => Number.isFinite(v));
const RECEIVER_IDS = (__ENV.RECEIVER_IDS || '').split(',').map((v) => Number(v.trim())).filter((v) => Number.isFinite(v));
const AUTO_CREATE_CHATS = (__ENV.AUTO_CREATE_CHATS || 'false').toLowerCase() === 'true';
const VUS = Number(__ENV.VUS || 30);
const DURATION = __ENV.DURATION || '2m';
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || 0.2);
const WS_WAIT_MS = Number(__ENV.WS_WAIT_MS || 2500);
const REST_SIZE = Number(__ENV.REST_SIZE || 20);
const MESSAGE_TEXT = __ENV.MESSAGE_TEXT || 'k6 chat load test';
const CHAT_IDS = (__ENV.CHAT_IDS || '').split(',').map((v) => Number(v.trim())).filter((v) => Number.isFinite(v));
const TOKENS = (__ENV.TOKENS || '').split(',').map((v) => v.trim()).filter((v) => v.length > 0);
const CHAT_REQUEST_TYPE = __ENV.CHAT_REQUEST_TYPE || 'COFFEE_CHAT';

const wsConnectSuccess = new Rate('ws_connect_success');
const stompConnected = new Rate('stomp_connected');
const wsMessageReceived = new Rate('ws_message_received');
const wsRoundTripMs = new Trend('ws_round_trip_ms');
const wsSentCount = new Counter('ws_sent_count');

export const options = {
  vus: VUS,
  duration: DURATION,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000', 'p(99)<4000'],
    ws_connect_success: ['rate>0.99'],
    stomp_connected: ['rate>0.99'],
    ws_message_received: ['rate>0.95'],
    ws_round_trip_ms: ['p(95)<1500', 'p(99)<3000'],
  },
};

function toWsUrl(baseUrl) {
  if (baseUrl.startsWith('https://')) return `${baseUrl.replace('https://', 'wss://')}/ws`;
  if (baseUrl.startsWith('http://')) return `${baseUrl.replace('http://', 'ws://')}/ws`;
  return `${baseUrl}/ws`;
}

function pickToken() {
  return runtimeTokens[(__VU - 1) % runtimeTokens.length];
}

function pickChatId() {
  return runtimeChatIds[(__VU + __ITER) % runtimeChatIds.length];
}

function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
  };
}

let runtimeTokens = TOKENS;
let runtimeChatIds = CHAT_IDS;
let runtimeUserIds = [...USER_IDS];
let runtimeTokenChatPairs = [];

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

function extractUserId(parsed) {
  if (!parsed || typeof parsed !== 'object') return null;
  if (typeof parsed.user_id === 'number') return parsed.user_id;
  if (typeof parsed.userId === 'number') return parsed.userId;
  if (parsed.data && typeof parsed.data === 'object') {
    if (typeof parsed.data.user_id === 'number') return parsed.data.user_id;
    if (typeof parsed.data.userId === 'number') return parsed.data.userId;
  }
  return null;
}

function buildSignupPayload(kind, index) {
  const suffix = `${Date.now()}${index}`;
  const short = suffix.slice(-6);
  const nickname = `${kind === 'EXPERT' ? 'ke' : 'ks'}${short}`.slice(0, 10);
  return {
    oauth_provider: 'KAKAO',
    oauth_id: `${SIGNUP_PREFIX}_${kind}_${suffix}`,
    email: `${SIGNUP_PREFIX}_${kind.toLowerCase()}_${suffix}@example.com`,
    nickname,
    user_type: kind === 'EXPERT' ? 'EXPERT' : 'JOB_SEEKER',
    career_level_id: SIGNUP_CAREER_LEVEL_ID,
    job_ids: [],
    skills: [],
    introduction: 'k6 load test user',
    company_name: kind === 'EXPERT' ? 'K6 Corp' : null,
    company_email: kind === 'EXPERT' ? `${SIGNUP_PREFIX}_${suffix}@k6corp.com` : null,
    terms_agreed: true,
  };
}

function signupUsersIfNeeded() {
  if (!AUTO_SIGNUP) return { issued: [], tokenByUserId: {}, userIds: [], requesterIds: [], receiverIds: [] };

  const all = [];
  const tokenByUserId = {};
  const requesterIds = [];
  const receiverIds = [];

  const plans = [
    { kind: 'JOB_SEEKER', count: SIGNUP_SEEKER_COUNT },
    { kind: 'EXPERT', count: SIGNUP_EXPERT_COUNT },
  ];

  let seq = 0;
  for (const p of plans) {
    for (let i = 0; i < p.count; i++) {
      seq += 1;
      const payload = buildSignupPayload(p.kind === 'EXPERT' ? 'EXPERT' : 'SEEKER', seq);
      const res = http.post(SIGNUP_URL, JSON.stringify(payload), {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'signup_seed' },
      });
      check(res, { 'signup seed 201': (r) => r.status === 201 || r.status === 200 });

      let parsed;
      try {
        parsed = res.json();
      } catch (_) {
        parsed = null;
      }
      const userId = extractUserId(parsed);
      const token = extractAccessToken(parsed);
      if (!userId || !token) {
        throw new Error(`failed signup parse. status=${res.status} body=${res.body}`);
      }

      all.push(token);
      tokenByUserId[userId] = token;
      if (p.kind === 'EXPERT') receiverIds.push(userId);
      else requesterIds.push(userId);
    }
  }

  const userIds = [...requesterIds, ...receiverIds];
  return { issued: all, tokenByUserId, userIds, requesterIds, receiverIds };
}

function issueDevTokens() {
  const tokenByUserId = {};
  const issued = [];
  for (const userId of USER_IDS) {
    const res = http.post(
      DEV_TOKEN_URL,
      JSON.stringify({ user_id: userId }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'dev_token_issue' },
      },
    );
    check(res, { 'dev token issue 200': (r) => r.status === 200 });

    let parsed;
    try {
      parsed = res.json();
    } catch (_) {
      parsed = null;
    }
    const token = extractAccessToken(parsed);
    if (!token) {
      throw new Error(`failed to parse access token for user_id=${userId}. body=${res.body}`);
    }
    issued.push(token);
    tokenByUserId[userId] = token;
  }
  return { issued, tokenByUserId };
}

function tryFindExistingRoomId(baseUrl, token, requesterId, receiverId) {
  const listRes = http.get(`${baseUrl}/api/v1/chats?status=ACTIVE&size=200`, {
    headers: authHeaders(token),
    tags: { name: 'chat_list_for_seed' },
  });
  if (listRes.status !== 200) return null;

  let body;
  try {
    body = listRes.json();
  } catch (_) {
    return null;
  }

  const chats = body?.data?.chats || [];
  for (const c of chats) {
    const reqId = c?.requester?.user_id;
    const recId = c?.receiver?.user_id;
    if (
      (reqId === requesterId && recId === receiverId) ||
      (reqId === receiverId && recId === requesterId)
    ) {
      return c?.chat_id || null;
    }
  }
  return null;
}

function createChatsIfNeeded(baseUrl, tokenByUserId) {
  if (!AUTO_CREATE_CHATS) return [];

  let requesters = REQUESTER_IDS;
  let receivers = RECEIVER_IDS;
  if (requesters.length === 0 || receivers.length === 0) {
    if (USER_IDS.length < 2) {
      throw new Error('AUTO_CREATE_CHATS=true requires REQUESTER_IDS/RECEIVER_IDS or USER_IDS>=2');
    }
    const mid = Math.floor(USER_IDS.length / 2);
    requesters = USER_IDS.slice(0, mid);
    receivers = USER_IDS.slice(mid);
  }
  if (requesters.length === 0 || receivers.length === 0) {
    throw new Error('not enough users to auto create chats');
  }

  const created = [];
  for (let i = 0; i < requesters.length; i++) {
    const requesterId = requesters[i];
    const receiverId = receivers[i % receivers.length];
    const requesterToken = tokenByUserId[requesterId];
    if (!requesterToken) {
      throw new Error(`missing requester token for user_id=${requesterId}`);
    }

    const payload = JSON.stringify({
      receiver_id: receiverId,
      request_type: CHAT_REQUEST_TYPE,
    });
    const res = http.post(`${baseUrl}/api/v1/chats`, payload, {
      headers: {
        ...authHeaders(requesterToken),
        'Content-Type': 'application/json',
      },
      tags: { name: 'chat_create_seed' },
    });

    if (res.status === 201 || res.status === 200) {
      let parsed;
      try {
        parsed = res.json();
      } catch (_) {
        parsed = null;
      }
      const chatId = parsed?.data?.chat_id;
      if (chatId) {
        created.push(chatId);
        continue;
      }
    }

    const existing = tryFindExistingRoomId(baseUrl, requesterToken, requesterId, receiverId);
    if (existing) {
      created.push(existing);
      continue;
    }

    throw new Error(`chat create failed requester=${requesterId} receiver=${receiverId} status=${res.status} body=${res.body}`);
  }
  return created;
}

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
  let tokenByUserId = {};
  let runtimeRequesterIds = [...REQUESTER_IDS];
  let runtimeReceiverIds = [...RECEIVER_IDS];

  if (runtimeTokens.length === 0 && runtimeUserIds.length === 0 && AUTO_SIGNUP) {
    const signupResult = signupUsersIfNeeded();
    runtimeTokens = signupResult.issued;
    tokenByUserId = signupResult.tokenByUserId;
    runtimeUserIds = signupResult.userIds;
    runtimeRequesterIds = signupResult.requesterIds;
    runtimeReceiverIds = signupResult.receiverIds;
  }

  if (runtimeTokens.length === 0) {
    if (runtimeUserIds.length === 0) {
      throw new Error('TOKENS or USER_IDS is required. example: USER_IDS=3001,3002');
    }
    const originalUserIds = [...USER_IDS];
    USER_IDS.length = 0;
    runtimeUserIds.forEach((id) => USER_IDS.push(id));
    const tokenResult = issueDevTokens();
    USER_IDS.length = 0;
    originalUserIds.forEach((id) => USER_IDS.push(id));
    runtimeTokens = tokenResult.issued;
    tokenByUserId = tokenResult.tokenByUserId;
  } else if (runtimeUserIds.length > 0) {
    // manual TOKENS + USER_IDS 함께 전달하면 매핑 사용
    runtimeUserIds.forEach((uid, idx) => {
      if (runtimeTokens[idx]) tokenByUserId[uid] = runtimeTokens[idx];
    });
  }

  if (runtimeChatIds.length === 0) {
    if (runtimeRequesterIds.length > 0 && runtimeReceiverIds.length > 0) {
      REQUESTER_IDS.length = 0;
      RECEIVER_IDS.length = 0;
      runtimeRequesterIds.forEach((id) => REQUESTER_IDS.push(id));
      runtimeReceiverIds.forEach((id) => RECEIVER_IDS.push(id));
    }
    const autoCreated = createChatsIfNeeded(BASE_URL, tokenByUserId);
    runtimeChatIds = autoCreated;
  }
  if (runtimeChatIds.length === 0) {
    throw new Error('CHAT_IDS is required, or set AUTO_CREATE_CHATS=true with USER_IDS/REQUESTER_IDS/RECEIVER_IDS');
  }

  const tokenChatPairs = [];
  if (runtimeUserIds.length === runtimeTokens.length && runtimeUserIds.length > 0) {
    for (let i = 0; i < runtimeTokens.length; i++) {
      const token = runtimeTokens[i];
      const userId = runtimeUserIds[i];
      const chats = fetchActiveChatIdsForToken(BASE_URL, token, runtimeChatIds);
      if (chats.length > 0) {
        tokenChatPairs.push({ token, userId, chats });
      }
    }
  }

  return {
    tokens: runtimeTokens,
    chatIds: runtimeChatIds,
    userIds: runtimeUserIds,
    tokenChatPairs,
  };
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
  // k6 runtime does not provide TextEncoder in all versions.
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
  const clientMessageId = `k6-${__VU}-${__ITER}-${Date.now()}`;

  const connectRes = ws.connect(wsUrl, {}, (socket) => {
    let connectOk = false;
    let sentAt = 0;
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
          sentAt = Date.now();
          socket.send(stompSendFrame(chatId, MESSAGE_TEXT, clientMessageId));
          wsSentCount.add(1);
          continue;
        }

        if (frame.startsWith('MESSAGE')) {
          const body = extractBody(frame);
          if (body.includes(clientMessageId)) {
            wsMessageReceived.add(true);
            if (sentAt > 0) {
              wsRoundTripMs.add(Date.now() - sentAt);
            }
            socket.close();
            return;
          }
        }

        if (frame.startsWith('ERROR')) {
          socket.close();
          return;
        }
      }
    });

    socket.on('close', () => {
      if (!connectOk) {
        stompConnected.add(false);
        wsMessageReceived.add(false);
      }
    });

    socket.on('error', () => {
      wsConnectSuccess.add(false);
      stompConnected.add(false);
      wsMessageReceived.add(false);
    });

    socket.setTimeout(() => {
      if (connectOk) {
        wsMessageReceived.add(false);
      } else {
        wsConnectSuccess.add(false);
        stompConnected.add(false);
      }
      socket.close();
    }, WS_WAIT_MS);
  });

  check(connectRes, { 'ws connect status 101': (r) => r && r.status === 101 });
}

export default function (data) {
  runtimeTokens = data.tokens;
  runtimeChatIds = data.chatIds;
  runtimeUserIds = data.userIds || runtimeUserIds;
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
