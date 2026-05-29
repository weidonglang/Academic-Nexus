#!/usr/bin/env node

const { performance } = require('node:perf_hooks');
const net = require('node:net');
const fs = require('node:fs');
const path = require('node:path');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const OFFERING_ID = process.env.OFFERING_ID ? Number(process.env.OFFERING_ID) : 0;
const OFFERING_IDS = (process.env.OFFERING_IDS || '')
  .split(',')
  .map((item) => Number(item.trim()))
  .filter((item) => Number.isFinite(item) && item > 0);
const SMART_SWITCH = process.env.SMART_SWITCH === 'true';
const SMART_MODE = (process.env.SMART_MODE || (SMART_SWITCH ? 'random' : 'single')).toLowerCase();
const REDIS_HOST = process.env.REDIS_HOST || 'localhost';
const REDIS_PORT = Number(process.env.REDIS_PORT || 6379);
const REDIS_ENABLED = process.env.REDIS_ENABLED !== 'false';
const LOAD_USERS = Number(process.env.LOAD_USERS || 0);
const LOAD_USER_PREFIX = normalizePrefix(process.env.LOAD_USER_PREFIX || 'lt');
const LOAD_USER_START = Number(process.env.LOAD_USER_START || 1);
const ACCOUNT_BATCH_SIZE = Number(process.env.ACCOUNT_BATCH_SIZE || 500);
const CONCURRENCY = Number(process.env.CONCURRENCY || (LOAD_USERS >= 1000 ? 300 : 20));
const REQUESTS = Number(process.env.REQUESTS || (LOAD_USERS > 0 ? LOAD_USERS : 100));
const LOGIN_CONCURRENCY = Number(process.env.LOGIN_CONCURRENCY || (LOAD_USERS >= 1000 ? 100 : 20));
const PASSWORD = process.env.PASSWORD || '';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || process.env.CLEANUP_PASSWORD || '';
const CLEANUP = process.env.CLEANUP !== 'false';
const CLEANUP_USERNAME = process.env.CLEANUP_USERNAME || '';
const INCLUDE_DEFAULT_ACCOUNTS = process.env.INCLUDE_DEFAULT_ACCOUNTS === 'true';
const DEFAULT_ACCOUNTS = '';
const REPORT_DIR = process.env.REPORT_DIR || 'reports';
const REPORT_ENABLED = process.env.REPORT !== 'false';

const MANUAL_ACCOUNTS = (process.env.ACCOUNTS || (LOAD_USERS > 0 ? '' : DEFAULT_ACCOUNTS))
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean);

const runReport = {
  startedAt: new Date().toISOString(),
  config: {
    baseUrl: BASE_URL,
    redisHost: REDIS_HOST,
    redisPort: REDIS_PORT,
    loadUsers: LOAD_USERS,
    requests: REQUESTS,
    concurrency: CONCURRENCY,
    loginConcurrency: LOGIN_CONCURRENCY,
    accountBatchSize: ACCOUNT_BATCH_SIZE,
    smartMode: SMART_MODE,
    redisEnabled: REDIS_ENABLED,
    cleanup: CLEANUP,
  },
  redis: {},
  offerings: [],
  phases: [],
  accountBatches: [],
  login: {},
  summary: null,
  cleanup: {},
};

function isSuccessCode(code) {
  return code === '0' || code === '200' || code === 0 || code === 200;
}

function normalizePrefix(prefix) {
  const normalized = String(prefix || '').trim().replace(/[^A-Za-z0-9_-]/g, '');
  return normalized || 'lt';
}

function percentile(values, p) {
  if (!values.length) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  const index = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[index];
}

function startPhase(name, title) {
  const phase = {
    name,
    title,
    startedAt: new Date().toISOString(),
    startMs: performance.now(),
    durationMs: 0,
    status: 'RUNNING',
    details: {},
  };
  runReport.phases.push(phase);
  return phase;
}

function endPhase(phase, status = 'DONE', details = {}) {
  phase.durationMs = performance.now() - phase.startMs;
  phase.endedAt = new Date().toISOString();
  phase.status = status;
  phase.details = { ...phase.details, ...details };
}

function mapToObject(map) {
  return Object.fromEntries([...map.entries()].sort());
}

function countBy(values, keyFn) {
  const map = new Map();
  for (const value of values) {
    const key = String(keyFn(value));
    map.set(key, (map.get(key) || 0) + 1);
  }
  return mapToObject(map);
}

function loadUsername(prefix, serial) {
  return `${prefix}${String(serial).padStart(5, '0')}`;
}

function generatedAccounts() {
  if (LOAD_USERS <= 0) return [];
  return Array.from({ length: LOAD_USERS }, (_, index) => loadUsername(LOAD_USER_PREFIX, LOAD_USER_START + index));
}

async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`;
  let response;
  try {
    response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
      },
    });
  } catch (error) {
    const cause = error.cause ? `; cause=${error.cause.code || error.cause.message}` : '';
    throw new Error(`Request failed: ${options.method || 'GET'} ${url}; ${error.message}${cause}`);
  }
  const text = await response.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { response, body };
}

async function checkTcpPort(host, port, timeoutMs = 800) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host, port });
    const done = (ok) => {
      socket.removeAllListeners();
      socket.destroy();
      resolve(ok);
    };
    socket.setTimeout(timeoutMs);
    socket.once('connect', () => done(true));
    socket.once('timeout', () => done(false));
    socket.once('error', () => done(false));
  });
}

async function login(username, password = PASSWORD) {
  const { response, body } = await request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  if (!response.ok || !isSuccessCode(body?.code)) {
    throw new Error(`Login failed for ${username}: HTTP ${response.status} ${JSON.stringify(body)}`);
  }
  return {
    username,
    token: body.data.accessToken,
  };
}

async function ensureLoadAccounts(adminSession) {
  if (LOAD_USERS <= 0) return null;

  const startedAt = performance.now();
  let createdUsers = 0;
  let existingUsers = 0;
  let createdStudents = 0;
  let firstUsername = '';
  let lastUsername = '';

  for (let prepared = 0; prepared < LOAD_USERS; prepared += ACCOUNT_BATCH_SIZE) {
    const count = Math.min(ACCOUNT_BATCH_SIZE, LOAD_USERS - prepared);
    const startIndex = LOAD_USER_START + prepared;
    const { response, body } = await request('/api/test/course-selection/accounts', {
      method: 'POST',
      headers: { Authorization: `Bearer ${adminSession.token}` },
      body: JSON.stringify({
        prefix: LOAD_USER_PREFIX,
        startIndex,
        count,
        password: PASSWORD,
      }),
    });
    if (!response.ok || !isSuccessCode(body?.code)) {
      const message = body?.message || JSON.stringify(body);
      if (response.status === 500 && message === 'Internal server error') {
        throw new Error(
          `Cannot prepare load-test accounts batch ${startIndex}-${startIndex + count - 1}: HTTP 500 Internal server error. ` +
          'The backend may still be running old compiled code. Stop and restart Spring Boot, then run the script again.'
        );
      }
      throw new Error(`Cannot prepare load-test accounts batch ${startIndex}-${startIndex + count - 1}: HTTP ${response.status} ${JSON.stringify(body)}`);
    }

    const data = body.data;
    if (!firstUsername) firstUsername = data.firstUsername;
    lastUsername = data.lastUsername;
    createdUsers += data.createdUsers;
    existingUsers += data.existingUsers;
    createdStudents += data.createdStudents;

    const done = prepared + count;
    const elapsed = ((performance.now() - startedAt) / 1000).toFixed(1);
    runReport.accountBatches.push({
      done,
      total: LOAD_USERS,
      firstUsername: data.firstUsername,
      lastUsername: data.lastUsername,
      createdUsers: data.createdUsers,
      existingUsers: data.existingUsers,
      elapsedSeconds: Number(elapsed),
    });
    console.log(`  account progress ${done}/${LOAD_USERS}, batch=${data.firstUsername}..${data.lastUsername}, created=${data.createdUsers}, existing=${data.existingUsers}, elapsed=${elapsed}s`);
  }

  return {
    firstUsername,
    lastUsername,
    createdUsers,
    existingUsers,
    createdStudents,
  };
}

async function setBackendRedisMode(adminSession, enabled) {
  const { response, body } = await request('/api/test/course-selection/redis-mode', {
    method: 'POST',
    headers: { Authorization: `Bearer ${adminSession.token}` },
    body: JSON.stringify({ enabled }),
  });
  if (!response.ok || !isSuccessCode(body?.code)) {
    throw new Error(`Cannot set backend Redis mode: HTTP ${response.status} ${JSON.stringify(body)}`);
  }
  return body.data;
}

async function prewarmRedisStock(adminSession, offeringIds) {
  if (!REDIS_ENABLED || !offeringIds.length) {
    return null;
  }
  const { response, body } = await request('/api/test/course-selection/redis-stock/prewarm', {
    method: 'POST',
    headers: { Authorization: `Bearer ${adminSession.token}` },
    body: JSON.stringify({ offeringIds }),
  });
  if (!response.ok || !isSuccessCode(body?.code)) {
    throw new Error(`Cannot prewarm Redis stock: HTTP ${response.status} ${JSON.stringify(body)}`);
  }
  return body.data;
}

async function findOfferingIds(token) {
  if (OFFERING_IDS.length) return OFFERING_IDS;
  if (OFFERING_ID > 0) return [OFFERING_ID];
  const { response, body } = await request('/api/course-selection/offerings', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok || !isSuccessCode(body?.code)) {
    throw new Error(`Cannot read offerings: HTTP ${response.status} ${JSON.stringify(body)}`);
  }
  const offerings = isSmartMode()
    ? body.data.filter((item) => item.selectableNow && !item.selected)
    : [body.data.find((item) => item.selectableNow && !item.selected) || body.data[0]].filter(Boolean);
  if (!offerings.length) {
    throw new Error('No course offering found. Please create course offerings first.');
  }
  return offerings.map((item) => item.offeringId);
}

async function grabOne(session, offeringId, index) {
  const startedAt = performance.now();
  try {
    const { response, body } = await request('/api/course-selection/grab', {
      method: 'POST',
      headers: { Authorization: `Bearer ${session.token}` },
      body: JSON.stringify({
        offeringId,
        requestId: `load-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`,
      }),
    });
    const elapsed = performance.now() - startedAt;
    const message = body?.message || body?.data?.message || '';
    return {
      ok: response.ok && isSuccessCode(body?.code),
      httpStatus: response.status,
      apiCode: body?.code,
      status: body?.data?.status || inferBusinessStatus(message, response.status),
      message,
      offeringId,
      username: session.username,
      elapsed,
    };
  } catch (error) {
    return {
      ok: false,
      httpStatus: 0,
      apiCode: 'NETWORK',
      status: 'NETWORK_ERROR',
      message: error.message,
      offeringId,
      username: session.username,
      elapsed: performance.now() - startedAt,
    };
  }
}

async function grab(session, offeringIds, index) {
  const ids = offeringIdsForRequest(offeringIds, index);
  let lastResult = null;
  for (const offeringId of ids) {
    const result = await grabOne(session, offeringId, index);
    lastResult = result;
    if (result.status === 'SUCCESS') {
      return result;
    }
    if (!shouldTryNextOffering(result)) {
      return result;
    }
  }
  return lastResult;
}

function offeringIdsForRequest(offeringIds, index) {
  if (!offeringIds.length) return [];
  if (SMART_MODE === 'random') {
    return [offeringIds[Math.floor(Math.random() * offeringIds.length)]];
  }
  if (SMART_MODE === 'random-retry') {
    return shuffle(offeringIds);
  }
  if (SMART_MODE === 'sequential') {
    return offeringIds;
  }
  return [offeringIds[index % offeringIds.length]];
}

function shuffle(values) {
  const copy = [...values];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

async function cleanupSelections(adminSession, offeringId, selectedAfter, usernames) {
  const { response, body } = await request('/api/test/course-selection/cleanup', {
    method: 'POST',
    headers: { Authorization: `Bearer ${adminSession.token}` },
    body: JSON.stringify({
      offeringId,
      selectedAfter,
      usernames,
    }),
  });
  if (!response.ok || !isSuccessCode(body?.code)) {
    throw new Error(`Cleanup failed: HTTP ${response.status} ${JSON.stringify(body)}`);
  }
  return body.data.deleted;
}

function inferBusinessStatus(message, httpStatus) {
  const raw = String(message || '');
  const prefix = raw.includes(':') ? raw.split(':')[0].trim() : '';
  if (prefix) return prefix;
  if (httpStatus === 409) return 'CONFLICT';
  if (httpStatus >= 500) return 'SERVER_ERROR';
  return 'NO_STATUS';
}

function shouldTryNextOffering(result) {
  return (SMART_MODE === 'sequential' || SMART_MODE === 'random-retry')
    && ['FULL', 'ALREADY_SELECTED', 'NOT_STARTED', 'ENDED'].includes(result.status);
}

function isSmartMode() {
  return SMART_MODE === 'random' || SMART_MODE === 'random-retry' || SMART_MODE === 'sequential';
}

async function runPool(tasks, concurrency, onProgress) {
  const results = [];
  let next = 0;
  let finished = 0;

  async function worker() {
    while (next < tasks.length) {
      const taskIndex = next++;
      results[taskIndex] = await tasks[taskIndex]();
      finished++;
      if (onProgress) onProgress(finished, tasks.length);
    }
  }

  await Promise.all(Array.from({ length: Math.min(concurrency, tasks.length) }, worker));
  return results;
}

async function loginAccounts(accounts) {
  const tasks = accounts.map((username) => async () => {
    try {
      return await login(username);
    } catch (error) {
      return { username, error: error.message };
    }
  });
  let lastPrinted = 0;
  const results = await runPool(tasks, LOGIN_CONCURRENCY, (finished, total) => {
    if (total < 100) return;
    const percent = Math.floor((finished / total) * 100);
    if (percent >= lastPrinted + 10 || finished === total) {
      lastPrinted = percent;
      console.log(`  login progress ${finished}/${total}`);
    }
  });
  const sessions = [];
  const failures = [];
  for (const result of results) {
    if (result?.token) {
      sessions.push(result);
    } else {
      failures.push(result);
    }
  }
  return { sessions, failures };
}

function buildSummary(results, startedAt, accountCount, offeringIds) {
  const elapsedTotal = performance.now() - startedAt;
  const latencies = results.map((item) => item.elapsed);
  const byStatus = new Map();
  const byHttp = new Map();
  const byOffering = new Map();
  const errors = [];

  for (const item of results) {
    byStatus.set(item.status, (byStatus.get(item.status) || 0) + 1);
    byHttp.set(String(item.httpStatus), (byHttp.get(String(item.httpStatus)) || 0) + 1);
    const offeringKey = String(item.offeringId);
    if (!byOffering.has(offeringKey)) {
      byOffering.set(offeringKey, { offeringId: item.offeringId, total: 0, success: 0, full: 0, failed: 0 });
    }
    const offering = byOffering.get(offeringKey);
    offering.total++;
    if (item.status === 'SUCCESS') {
      offering.success++;
    } else if (item.status === 'FULL') {
      offering.full++;
      offering.failed++;
    } else {
      offering.failed++;
    }
    if (!item.ok || item.status !== 'SUCCESS') {
      errors.push(item);
    }
  }

  return {
    baseUrl: BASE_URL,
    offeringIds,
    accountCount,
    requestCount: results.length,
    concurrency: CONCURRENCY,
    totalTimeMs: elapsedTotal,
    throughput: results.length / (elapsedTotal / 1000),
    avgLatency: latencies.reduce((a, b) => a + b, 0) / latencies.length,
    p50: percentile(latencies, 50),
    p95: percentile(latencies, 95),
    p99: percentile(latencies, 99),
    byStatus: mapToObject(byStatus),
    byHttp: mapToObject(byHttp),
    byOffering: [...byOffering.values()].sort((a, b) => Number(a.offeringId) - Number(b.offeringId)),
    sampleErrors: errors.slice(0, 10),
  };
}

function printSummary(results, startedAt, accountCount, offeringIds) {
  const summary = buildSummary(results, startedAt, accountCount, offeringIds);
  console.log('\n=== Course Grab Load Test Summary ===');
  console.log(`Base URL       : ${summary.baseUrl}`);
  console.log(`Offering IDs   : ${summary.offeringIds.join(',')}`);
  console.log(`Accounts       : ${summary.accountCount}`);
  console.log(`Requests       : ${summary.requestCount}`);
  console.log(`Concurrency    : ${summary.concurrency}`);
  console.log(`Total time     : ${summary.totalTimeMs.toFixed(0)} ms`);
  console.log(`Throughput     : ${summary.throughput.toFixed(2)} req/s`);
  console.log(`Avg latency    : ${summary.avgLatency.toFixed(2)} ms`);
  console.log(`P50 latency    : ${summary.p50.toFixed(2)} ms`);
  console.log(`P95 latency    : ${summary.p95.toFixed(2)} ms`);
  console.log(`P99 latency    : ${summary.p99.toFixed(2)} ms`);

  console.log('\nBy business status:');
  Object.entries(summary.byStatus).forEach(([status, count]) => console.log(`  ${status}: ${count}`));

  console.log('\nBy HTTP status:');
  Object.entries(summary.byHttp).forEach(([status, count]) => console.log(`  ${status}: ${count}`));

  if (summary.sampleErrors.length) {
    console.log('\nSample non-success results:');
    summary.sampleErrors.forEach((item) => {
      console.log(`  [${item.httpStatus}/${item.apiCode}/${item.status}] offering=${item.offeringId} ${item.username} ${item.elapsed.toFixed(1)}ms ${item.message}`);
    });
  }
  return summary;
}

function unique(values) {
  return [...new Set(values)];
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function bar(width, color = '#2563eb') {
  return `<span class="bar"><span style="width:${Math.max(0, Math.min(100, width))}%;background:${color}"></span></span>`;
}

function generateReportFiles(report) {
  if (!REPORT_ENABLED || !report.summary) {
    return null;
  }
  const dir = path.resolve(process.cwd(), REPORT_DIR);
  fs.mkdirSync(dir, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const jsonPath = path.join(dir, `load-test-${stamp}.json`);
  const htmlPath = path.join(dir, `load-test-${stamp}.html`);
  fs.writeFileSync(jsonPath, JSON.stringify(report, null, 2), 'utf-8');
  fs.writeFileSync(htmlPath, renderHtmlReportV2(report, path.basename(jsonPath)), 'utf-8');
  return { jsonPath, htmlPath };
}

function renderHtmlReport(report, jsonName) {
  const summary = report.summary;
  const statusRows = Object.entries(summary.byStatus)
    .map(([status, count]) => {
      const pct = (count / summary.requestCount) * 100;
      return `<tr><td>${escapeHtml(status)}</td><td>${count}</td><td>${pct.toFixed(2)}%</td><td>${bar(pct, status === 'SUCCESS' ? '#16a34a' : '#dc2626')}</td></tr>`;
    }).join('');
  const phaseRows = report.phases.map((phase, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${escapeHtml(phase.title)}</td>
      <td>${escapeHtml(phase.status)}</td>
      <td>${phase.durationMs.toFixed(0)} ms</td>
      <td><code>${escapeHtml(JSON.stringify(phase.details))}</code></td>
    </tr>
  `).join('');
  const maxOfferingTotal = Math.max(1, ...summary.byOffering.map((item) => item.total));
  const offeringRows = summary.byOffering.map((item) => `
    <tr>
      <td>${item.offeringId}</td>
      <td>${item.total}</td>
      <td>${item.success}</td>
      <td>${item.full}</td>
      <td>${item.failed}</td>
      <td>${bar((item.total / maxOfferingTotal) * 100, '#0f766e')}</td>
    </tr>
  `).join('');
  const batchRows = report.accountBatches.slice(-20).map((item) => `
    <tr>
      <td>${item.done}/${item.total}</td>
      <td>${escapeHtml(item.firstUsername)} - ${escapeHtml(item.lastUsername)}</td>
      <td>${item.createdUsers}</td>
      <td>${item.existingUsers}</td>
      <td>${item.elapsedSeconds}s</td>
    </tr>
  `).join('');
  const errorRows = summary.sampleErrors.map((item) => `
    <tr>
      <td>${item.offeringId}</td>
      <td>${escapeHtml(item.username)}</td>
      <td>${escapeHtml(item.status)}</td>
      <td>${item.elapsed.toFixed(1)} ms</td>
      <td>${escapeHtml(item.message)}</td>
    </tr>
  `).join('');
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>智慧选课压测报告</title>
  <style>
    body{margin:0;background:#f6f7fb;color:#172033;font-family:"Microsoft YaHei",Arial,sans-serif}
    header{background:#17324d;color:#fff;padding:24px 32px}
    main{padding:24px 32px;max-width:1320px;margin:auto}
    h1{margin:0 0 8px;font-size:26px}
    h2{margin:0 0 14px;font-size:18px}
    .muted{color:#64748b}
    .grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:18px}
    .card{background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:16px;box-shadow:0 1px 2px rgba(15,23,42,.04)}
    .metric{font-size:26px;font-weight:700;margin-top:6px;color:#0f172a}
    table{width:100%;border-collapse:collapse;background:#fff;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;margin-bottom:18px}
    th,td{border-bottom:1px solid #e5e7eb;padding:9px 10px;text-align:left;font-size:13px;vertical-align:top}
    th{background:#eef2f7;color:#334155}
    tr:last-child td{border-bottom:0}
    code{font-family:Consolas,monospace;font-size:12px;color:#334155;white-space:normal}
    .bar{display:block;height:10px;background:#e5e7eb;border-radius:999px;overflow:hidden;min-width:120px}
    .bar span{display:block;height:100%}
    .section{margin:22px 0}
    .pill{display:inline-block;background:#e0f2fe;color:#075985;padding:3px 8px;border-radius:999px;margin-right:6px;font-size:12px}
  </style>
</head>
<body>
<header>
  <h1>智慧选课高并发压测报告</h1>
  <div>生成时间：${escapeHtml(new Date().toLocaleString('zh-CN'))}　数据文件：${escapeHtml(jsonName)}</div>
</header>
<main>
  <div class="grid">
    <div class="card"><div class="muted">请求总数</div><div class="metric">${summary.requestCount}</div></div>
    <div class="card"><div class="muted">成功选课</div><div class="metric">${summary.byStatus.SUCCESS || 0}</div></div>
    <div class="card"><div class="muted">吞吐量</div><div class="metric">${summary.throughput.toFixed(2)} /s</div></div>
    <div class="card"><div class="muted">平均响应</div><div class="metric">${summary.avgLatency.toFixed(1)} ms</div></div>
  </div>

  <div class="section card">
    <h2>压测配置</h2>
    <span class="pill">模式：${escapeHtml(report.config.smartMode)}</span>
    <span class="pill">并发：${report.config.concurrency}</span>
    <span class="pill">登录并发：${report.config.loginConcurrency}</span>
    <span class="pill">Redis：${report.redis.reachable ? '已连接' : '未连接'}</span>
    <p class="muted">课程池：${escapeHtml(summary.offeringIds.join(', '))}</p>
  </div>

  <div class="section">
    <h2>阶段记录</h2>
    <table><thead><tr><th>#</th><th>阶段</th><th>状态</th><th>耗时</th><th>阶段数据</th></tr></thead><tbody>${phaseRows}</tbody></table>
  </div>

  <div class="section">
    <h2>业务结果分布</h2>
    <table><thead><tr><th>状态</th><th>数量</th><th>占比</th><th>可视化</th></tr></thead><tbody>${statusRows}</tbody></table>
  </div>

  <div class="section">
    <h2>延迟指标</h2>
    <table><tbody>
      <tr><th>总耗时</th><td>${summary.totalTimeMs.toFixed(0)} ms</td><th>P50</th><td>${summary.p50.toFixed(2)} ms</td></tr>
      <tr><th>P95</th><td>${summary.p95.toFixed(2)} ms</td><th>P99</th><td>${summary.p99.toFixed(2)} ms</td></tr>
    </tbody></table>
  </div>

  <div class="section">
    <h2>课程随机分布</h2>
    <table><thead><tr><th>教学班ID</th><th>随机命中</th><th>成功</th><th>满员</th><th>失败</th><th>命中分布</th></tr></thead><tbody>${offeringRows}</tbody></table>
  </div>

  <div class="section">
    <h2>账号准备阶段</h2>
    <table><thead><tr><th>进度</th><th>账号范围</th><th>新建</th><th>已存在</th><th>累计耗时</th></tr></thead><tbody>${batchRows}</tbody></table>
  </div>

  <div class="section">
    <h2>失败样例</h2>
    <table><thead><tr><th>教学班ID</th><th>账号</th><th>状态</th><th>耗时</th><th>说明</th></tr></thead><tbody>${errorRows || '<tr><td colspan="5">无失败样例</td></tr>'}</tbody></table>
  </div>
</main>
</body>
</html>`;
}

function renderHtmlReportV2(report, jsonName) {
  const summary = report.summary;
  const success = summary.byStatus.SUCCESS || 0;
  const full = summary.byStatus.FULL || 0;
  const failed = Math.max(0, summary.requestCount - success - full);
  const successRate = summary.requestCount ? (success / summary.requestCount) * 100 : 0;
  const fullRate = summary.requestCount ? (full / summary.requestCount) * 100 : 0;
  const failedRate = summary.requestCount ? (failed / summary.requestCount) * 100 : 0;
  const phaseMax = Math.max(1, ...report.phases.map((phase) => phase.durationMs));
  const offeringMax = Math.max(1, ...summary.byOffering.map((item) => item.total));
  const latencyMax = Math.max(1, summary.p50, summary.p95, summary.p99);
  const generatedAt = new Date().toLocaleString('zh-CN', { hour12: false });

  const statusRows = Object.entries(summary.byStatus)
    .sort((a, b) => b[1] - a[1])
    .map(([status, count]) => {
      const pct = summary.requestCount ? (count / summary.requestCount) * 100 : 0;
      const color = status === 'SUCCESS' ? '#24834f' : status === 'FULL' ? '#c77a24' : '#b94242';
      return `<tr><td><span class="status-dot" style="background:${color}"></span>${escapeHtml(status)}</td><td>${count}</td><td>${pct.toFixed(2)}%</td><td>${bar(pct, color)}</td></tr>`;
    }).join('');

  const phaseRows = report.phases.map((phase, index) => {
    const width = (phase.durationMs / phaseMax) * 100;
    return `<tr>
      <td>${index + 1}</td>
      <td>${escapeHtml(phase.title)}</td>
      <td><span class="tag ${phase.status === 'DONE' ? 'ok' : 'warn'}">${escapeHtml(phase.status)}</span></td>
      <td>${phase.durationMs.toFixed(0)} ms</td>
      <td>${bar(width, '#1f6f7a')}</td>
      <td><code>${escapeHtml(JSON.stringify(phase.details))}</code></td>
    </tr>`;
  }).join('');

  const offeringRows = summary.byOffering.map((item) => {
    const width = (item.total / offeringMax) * 100;
    return `<tr>
      <td>${item.offeringId}</td>
      <td>${item.total}</td>
      <td>${item.success}</td>
      <td>${item.full}</td>
      <td>${item.failed}</td>
      <td>${bar(width, '#2b8379')}</td>
    </tr>`;
  }).join('');

  const offeringBars = summary.byOffering.slice(0, 20).map((item) => {
    const height = Math.max(6, (item.total / offeringMax) * 150);
    return `<div class="hist-item" title="教学班 ${item.offeringId}：${item.total} 次">
      <span style="height:${height}px"></span>
      <small>${item.offeringId}</small>
    </div>`;
  }).join('');

  const phaseTimeline = report.phases.map((phase, index) => {
    const width = Math.max(4, (phase.durationMs / phaseMax) * 100);
    return `<div class="timeline-row">
      <span>${index + 1}. ${escapeHtml(phase.title)}</span>
      <div><i style="width:${width}%"></i></div>
      <strong>${phase.durationMs.toFixed(0)} ms</strong>
    </div>`;
  }).join('');

  const batchRows = report.accountBatches.slice(-20).map((item) => `
    <tr>
      <td>${item.done}/${item.total}</td>
      <td>${escapeHtml(item.firstUsername)} - ${escapeHtml(item.lastUsername)}</td>
      <td>${item.createdUsers}</td>
      <td>${item.existingUsers}</td>
      <td>${item.elapsedSeconds}s</td>
    </tr>
  `).join('');

  const errorRows = summary.sampleErrors.map((item) => `
    <tr>
      <td>${item.offeringId}</td>
      <td>${escapeHtml(item.username)}</td>
      <td>${escapeHtml(item.status)}</td>
      <td>${item.elapsed.toFixed(1)} ms</td>
      <td>${escapeHtml(item.message)}</td>
    </tr>
  `).join('');

  const latencyBars = [
    ['P50', summary.p50, '#2b8379'],
    ['P95', summary.p95, '#c77a24'],
    ['P99', summary.p99, '#b94242'],
  ].map(([label, value, color]) => {
    const width = (Number(value) / latencyMax) * 100;
    return `<div class="latency-row"><span>${label}</span><div>${bar(width, color)}</div><strong>${Number(value).toFixed(2)} ms</strong></div>`;
  }).join('');

  const donut = (() => {
    const radius = 54;
    const circumference = 2 * Math.PI * radius;
    const successLen = (successRate / 100) * circumference;
    const fullLen = (fullRate / 100) * circumference;
    const failedLen = (failedRate / 100) * circumference;
    return `<svg class="donut" viewBox="0 0 140 140" role="img" aria-label="业务状态分布">
      <circle cx="70" cy="70" r="${radius}" fill="none" stroke="#e4edf0" stroke-width="18"></circle>
      <circle cx="70" cy="70" r="${radius}" fill="none" stroke="#24834f" stroke-width="18" stroke-dasharray="${successLen} ${circumference - successLen}" stroke-dashoffset="0" transform="rotate(-90 70 70)"></circle>
      <circle cx="70" cy="70" r="${radius}" fill="none" stroke="#c77a24" stroke-width="18" stroke-dasharray="${fullLen} ${circumference - fullLen}" stroke-dashoffset="${-successLen}" transform="rotate(-90 70 70)"></circle>
      <circle cx="70" cy="70" r="${radius}" fill="none" stroke="#b94242" stroke-width="18" stroke-dasharray="${failedLen} ${circumference - failedLen}" stroke-dashoffset="${-(successLen + fullLen)}" transform="rotate(-90 70 70)"></circle>
      <text x="70" y="65" text-anchor="middle" class="donut-main">${successRate.toFixed(1)}%</text>
      <text x="70" y="83" text-anchor="middle" class="donut-sub">成功率</text>
    </svg>`;
  })();

  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>智慧选课高并发压测报告</title>
  <style>
    :root{--bg:#f3f7f8;--panel:#fff;--line:#d8e4e8;--text:#172033;--muted:#637381;--primary:#1f6f7a;--accent:#c77a24;--success:#24834f;--danger:#b94242}
    *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font-family:"Microsoft YaHei","PingFang SC",Arial,sans-serif}
    header{padding:28px 40px;color:#fff;background:linear-gradient(90deg,#113e55,#1b7a86 58%,#2c8273)}
    main{width:min(1380px,100%);margin:0 auto;padding:22px 28px 36px}
    h1{margin:0 0 8px;font-size:28px;letter-spacing:0}h2{margin:0 0 14px;font-size:18px}.sub{color:rgba(255,255,255,.78)}
    .hero-meta{display:flex;flex-wrap:wrap;gap:8px;margin-top:16px}.pill{display:inline-flex;align-items:center;min-height:28px;padding:0 10px;border:1px solid rgba(255,255,255,.28);border-radius:999px;background:rgba(255,255,255,.12);font-size:13px}
    .grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.card{background:var(--panel);border:1px solid var(--line);border-radius:8px;box-shadow:0 8px 22px rgba(25,55,70,.06)}
    .metric{padding:17px 18px}.metric span{display:block;color:var(--muted);font-size:13px}.metric strong{display:block;margin-top:9px;font-size:30px;letter-spacing:0}.metric small{display:block;margin-top:4px;color:var(--muted)}
    .section{margin-top:18px;padding:18px}.two-col{display:grid;grid-template-columns:360px minmax(0,1fr);gap:18px}.three-col{display:grid;grid-template-columns:1fr 1fr 1fr;gap:18px}
    .donut-wrap{display:grid;grid-template-columns:150px minmax(0,1fr);gap:16px;align-items:center}.donut{width:150px;height:150px}.donut-main{font-size:21px;font-weight:800;fill:#172033}.donut-sub{font-size:12px;fill:#637381}
    .legend{display:grid;gap:10px}.legend-row{display:grid;grid-template-columns:90px 1fr 70px;gap:10px;align-items:center}.status-dot{display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:8px}
    table{width:100%;border-collapse:collapse;background:#fff;border:1px solid var(--line);border-radius:8px;overflow:hidden}th,td{border-bottom:1px solid #e7eef1;padding:9px 10px;text-align:left;font-size:13px;vertical-align:top}th{background:#edf4f6;color:#334155;font-weight:700}tr:last-child td{border-bottom:0}
    code{font-family:Consolas,monospace;font-size:12px;color:#425466;white-space:normal}.bar{display:block;height:10px;background:#e4ecef;border-radius:999px;overflow:hidden;min-width:100px}.bar span{display:block;height:100%}
    .tag{display:inline-flex;align-items:center;height:22px;padding:0 8px;border-radius:999px;font-size:12px;background:#eef2f4;color:#425466}.tag.ok{background:#e7f5ec;color:#216d43}.tag.warn{background:#fff2df;color:#935c16}
    .timeline{display:grid;gap:10px}.timeline-row{display:grid;grid-template-columns:210px 1fr 90px;gap:12px;align-items:center}.timeline-row span{color:#334155;font-size:13px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.timeline-row div{height:12px;background:#e4ecef;border-radius:999px;overflow:hidden}.timeline-row i{display:block;height:100%;background:var(--primary)}.timeline-row strong{text-align:right;font-size:13px}
    .histogram{height:200px;display:flex;align-items:flex-end;gap:8px;padding:12px 4px 0;border-bottom:1px solid var(--line);overflow-x:auto}.hist-item{min-width:28px;display:grid;align-items:end;justify-items:center;gap:6px}.hist-item span{width:18px;background:linear-gradient(180deg,#2b8379,#1f6f7a);border-radius:5px 5px 0 0}.hist-item small{color:var(--muted);font-size:11px}
    .latency-row{display:grid;grid-template-columns:48px 1fr 100px;gap:10px;align-items:center;margin:10px 0}.latency-row strong{text-align:right;font-size:13px}
    @media(max-width:960px){header{padding:22px}main{padding:14px}.grid,.two-col,.three-col{grid-template-columns:1fr}.timeline-row{grid-template-columns:1fr}.timeline-row strong{text-align:left}}
  </style>
</head>
<body>
<header>
  <h1>智慧选课高并发压测报告</h1>
  <div class="sub">生成时间：${escapeHtml(generatedAt)}　数据文件：${escapeHtml(jsonName)}</div>
  <div class="hero-meta">
    <span class="pill">模式：${escapeHtml(report.config.smartMode)}</span>
    <span class="pill">请求：${summary.requestCount}</span>
    <span class="pill">并发：${report.config.concurrency}</span>
    <span class="pill">登录并发：${report.config.loginConcurrency}</span>
    <span class="pill">Redis：${report.redis.backendEnabled === false ? '已关闭，数据库兜底' : (report.redis.reachable ? '已连接' : '未连接，数据库兜底')}</span>
  </div>
</header>
<main>
  <section class="grid">
    <article class="card metric"><span>请求总数</span><strong>${summary.requestCount}</strong><small>参与统计的抢课请求</small></article>
    <article class="card metric"><span>成功选课</span><strong>${success}</strong><small>成功率 ${successRate.toFixed(2)}%</small></article>
    <article class="card metric"><span>吞吐量</span><strong>${summary.throughput.toFixed(2)} /s</strong><small>总耗时 ${summary.totalTimeMs.toFixed(0)} ms</small></article>
    <article class="card metric"><span>平均响应</span><strong>${summary.avgLatency.toFixed(1)} ms</strong><small>P95 ${summary.p95.toFixed(1)} ms</small></article>
  </section>

  <section class="two-col">
    <article class="card section">
      <h2>业务结果分布</h2>
      <div class="donut-wrap">
        ${donut}
        <div class="legend">
          <div class="legend-row"><span><i class="status-dot" style="background:#24834f"></i>成功</span>${bar(successRate, '#24834f')}<strong>${success}</strong></div>
          <div class="legend-row"><span><i class="status-dot" style="background:#c77a24"></i>满员</span>${bar(fullRate, '#c77a24')}<strong>${full}</strong></div>
          <div class="legend-row"><span><i class="status-dot" style="background:#b94242"></i>失败</span>${bar(failedRate, '#b94242')}<strong>${failed}</strong></div>
        </div>
      </div>
    </article>
    <article class="card section">
      <h2>阶段耗时</h2>
      <div class="timeline">${phaseTimeline}</div>
    </article>
  </section>

  <section class="three-col">
    <article class="card section"><h2>Redis 状态</h2><table><tbody><tr><th>地址</th><td>${escapeHtml(report.redis.host)}:${report.redis.port}</td></tr><tr><th>后端模式</th><td>${report.redis.backendEnabled === false ? 'Redis 已关闭，数据库兜底' : 'Redis 已开启'}</td></tr><tr><th>端口连通</th><td>${report.redis.reachable ? '可连接' : '不可连接'}</td></tr><tr><th>自动清理</th><td>${report.config.cleanup ? '开启' : '关闭'}</td></tr></tbody></table></article>
    <article class="card section"><h2>延迟指标</h2>${latencyBars}</article>
    <article class="card section"><h2>课程池</h2><table><tbody><tr><th>教学班数</th><td>${summary.offeringIds.length}</td></tr><tr><th>教学班ID</th><td>${escapeHtml(summary.offeringIds.join(', '))}</td></tr></tbody></table></article>
  </section>

  <section class="card section">
    <h2>课程随机命中 Top 20</h2>
    <div class="histogram">${offeringBars || '<p>暂无课程命中数据</p>'}</div>
  </section>

  <section class="card section">
    <h2>阶段记录明细</h2>
    <table><thead><tr><th>#</th><th>阶段</th><th>状态</th><th>耗时</th><th>耗时占比</th><th>阶段数据</th></tr></thead><tbody>${phaseRows}</tbody></table>
  </section>

  <section class="card section">
    <h2>业务状态明细</h2>
    <table><thead><tr><th>状态</th><th>数量</th><th>占比</th><th>可视化</th></tr></thead><tbody>${statusRows}</tbody></table>
  </section>

  <section class="card section">
    <h2>课程随机分布</h2>
    <table><thead><tr><th>教学班ID</th><th>随机命中</th><th>成功</th><th>满员</th><th>失败</th><th>命中分布</th></tr></thead><tbody>${offeringRows}</tbody></table>
  </section>

  <section class="card section">
    <h2>账号准备批次</h2>
    <table><thead><tr><th>进度</th><th>账号范围</th><th>新建用户</th><th>已存在用户</th><th>累计耗时</th></tr></thead><tbody>${batchRows || '<tr><td colspan="5">本次未批量准备账号</td></tr>'}</tbody></table>
  </section>

  <section class="card section">
    <h2>失败样例</h2>
    <table><thead><tr><th>教学班ID</th><th>账号</th><th>状态</th><th>耗时</th><th>说明</th></tr></thead><tbody>${errorRows || '<tr><td colspan="5">无失败样例</td></tr>'}</tbody></table>
  </section>
</main>
</body>
</html>`;
}

async function main() {
  if (!CLEANUP_USERNAME || !ADMIN_PASSWORD || !PASSWORD) {
    throw new Error('Missing credentials. Please set CLEANUP_USERNAME, ADMIN_PASSWORD and PASSWORD in the panel or environment variables.');
  }
  console.log(`Base URL: ${BASE_URL}`);
  const redisPhase = startPhase('redis_check', 'Redis连通性检查');
  const redisReachable = await checkTcpPort(REDIS_HOST, REDIS_PORT);
  runReport.redis = { host: REDIS_HOST, port: REDIS_PORT, reachable: redisReachable };
  endPhase(redisPhase, redisReachable ? 'DONE' : 'FALLBACK', runReport.redis);
  console.log(`Redis   : ${REDIS_HOST}:${REDIS_PORT} ${redisReachable ? 'reachable' : 'not reachable, backend will fall back to database'}`);

  const adminPhase = startPhase('admin_login', '管理员登录');
  console.log('Logging in cleanup/admin account...');
  const cleanupSession = await login(CLEANUP_USERNAME, ADMIN_PASSWORD);
  endPhase(adminPhase, 'DONE', { username: CLEANUP_USERNAME });

  const redisModePhase = startPhase('redis_mode', '后端 Redis 模式设置');
  console.log(`Backend Redis mode: ${REDIS_ENABLED ? 'enabled' : 'disabled, database fallback only'}`);
  const redisMode = await setBackendRedisMode(cleanupSession, REDIS_ENABLED);
  runReport.redis.backendEnabled = redisMode.enabled;
  endPhase(redisModePhase, 'DONE', { backendRedisEnabled: redisMode.enabled });

  if (LOAD_USERS > 0) {
    const accountPhase = startPhase('account_prepare', '压测账号准备');
    console.log(`Preparing ${LOAD_USERS} load-test student accounts...`);
    const prepared = await ensureLoadAccounts(cleanupSession);
    console.log(`  accounts ${prepared.firstUsername}..${prepared.lastUsername}, created=${prepared.createdUsers}, existing=${prepared.existingUsers}`);
    endPhase(accountPhase, 'DONE', prepared);
  }

  const accounts = unique([
    ...generatedAccounts(),
    ...(INCLUDE_DEFAULT_ACCOUNTS ? DEFAULT_ACCOUNTS.split(',') : []),
    ...MANUAL_ACCOUNTS,
  ]);

  if (!accounts.length) {
    throw new Error('No test accounts configured. Set LOAD_USERS or ACCOUNTS.');
  }

  const loginPhase = startPhase('student_login', '学生账号批量登录');
  console.log(`Logging in ${accounts.length} test accounts with concurrency=${LOGIN_CONCURRENCY}...`);
  const { sessions, failures } = await loginAccounts(accounts);
  runReport.login = { total: accounts.length, success: sessions.length, failed: failures.length };
  failures.slice(0, 10).forEach((failure) => console.log(`  failed ${failure.username}: ${failure.error}`));
  if (failures.length > 10) {
    console.log(`  ... ${failures.length - 10} more login failures hidden`);
  }

  if (!sessions.length) {
    throw new Error('No account logged in. Check backend profile demo/dev and test accounts.');
  }
  console.log(`  logged in ${sessions.length}/${accounts.length}`);
  endPhase(loginPhase, failures.length ? 'PARTIAL' : 'DONE', runReport.login);

  const offeringPhase = startPhase('offering_prepare', '课程池准备');
  const offeringIds = await findOfferingIds(sessions[0].token);
  runReport.offerings = offeringIds;
  console.log(`Using offeringIds=${offeringIds.join(',')}`);
  if (REDIS_ENABLED) {
    const prewarmPhase = startPhase('redis_stock_prewarm', 'Redis库存预热');
    const prewarm = await prewarmRedisStock(cleanupSession, offeringIds);
    runReport.redis.prewarm = prewarm;
    console.log(`Redis stock prewarmed: ${prewarm.items.map((item) => `${item.offeringId}=${item.remaining}`).join(', ')}`);
    endPhase(prewarmPhase, 'DONE', prewarm);
  }
  if (SMART_MODE === 'random') {
    console.log('Smart mode=random: each request randomly picks one offering from the selected pool.');
  } else if (SMART_MODE === 'random-retry') {
    console.log('Smart mode=random-retry: each request tries offerings in random order until one succeeds or all fail.');
  } else if (SMART_MODE === 'sequential') {
    console.log('Smart mode=sequential: failed users try the next available offering in order.');
  }
  endPhase(offeringPhase, 'DONE', { offeringCount: offeringIds.length, offeringIds });

  const tasks = Array.from({ length: REQUESTS }, (_, index) => {
    const session = sessions[index % sessions.length];
    return () => grab(session, offeringIds, index + 1);
  });

  const selectedAfter = new Date(Date.now() - 1000).toISOString();
  const grabPhase = startPhase('course_grab', '并发抢课执行');
  const startedAt = performance.now();
  let results = [];
  try {
    results = await runPool(tasks, CONCURRENCY);
    runReport.summary = printSummary(results, startedAt, sessions.length, offeringIds);
    endPhase(grabPhase, 'DONE', {
      requests: results.length,
      success: runReport.summary.byStatus.SUCCESS || 0,
      full: runReport.summary.byStatus.FULL || 0,
      throughput: Number(runReport.summary.throughput.toFixed(2)),
    });
  } finally {
    if (CLEANUP) {
      const cleanupPhase = startPhase('cleanup', '压测数据清理');
      try {
        const deleted = await cleanupSelections(
          cleanupSession,
          offeringIds[0],
          selectedAfter,
          sessions.map((session) => session.username)
        );
        let totalDeleted = deleted;
        for (const offeringId of offeringIds.slice(1)) {
          totalDeleted += await cleanupSelections(
            cleanupSession,
            offeringId,
            selectedAfter,
            sessions.map((session) => session.username)
          );
        }
        console.log(`\nCleanup: deleted ${totalDeleted} load-test course_selection rows for offeringIds=${offeringIds.join(',')}.`);
        runReport.cleanup = { enabled: true, deleted: totalDeleted };
        endPhase(cleanupPhase, 'DONE', runReport.cleanup);
      } catch (error) {
        console.log(`\nCleanup failed: ${error.message}`);
        runReport.cleanup = { enabled: true, error: error.message };
        endPhase(cleanupPhase, 'FAILED', runReport.cleanup);
      }
    } else {
      console.log('\nCleanup skipped. Set CLEANUP=true to enable it.');
      runReport.cleanup = { enabled: false };
    }
    runReport.endedAt = new Date().toISOString();
    const reportFiles = generateReportFiles(runReport);
    if (reportFiles) {
      console.log(`\nReport JSON : ${reportFiles.jsonPath}`);
      console.log(`Report HTML : ${reportFiles.htmlPath}`);
    }
    if (!REDIS_ENABLED) {
      try {
        await setBackendRedisMode(cleanupSession, true);
        console.log('\nBackend Redis mode restored to enabled.');
      } catch (error) {
        console.log(`\nCannot restore backend Redis mode: ${error.message}`);
      }
    }
  }
}

main().catch((error) => {
  console.error('\nLoad test failed:', error.message);
  process.exit(1);
});
