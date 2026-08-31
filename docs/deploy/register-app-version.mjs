#!/usr/bin/env node
// -*- coding: utf-8 -*-
/**
 * 管理端登记 APP 版本：登录 → 上传 APK/zip → PUT /homeai/app/version/admin
 * 用法（在 jeecgboot-vue3 目录）：
 *   node ../../docs/deploy/register-app-version.mjs --apk <path> [--zip <path>] [--user admin] [--pass ***]
 */
import { createRequire } from 'node:module';
import { createHash, randomUUID } from 'node:crypto';
import { readFileSync, existsSync } from 'node:fs';
import { basename, resolve } from 'node:path';
import { execSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const require = createRequire(resolve(fileURLToPath(new URL('.', import.meta.url)), '../../JeecgBoot/jeecgboot-vue3/package.json'));
const CryptoJS = require('crypto-js');

const BASE = process.env.HOMEAI_API_BASE || 'http://127.0.0.1:8080/jeecg-boot';
const SIGN_SECRET = 'dd05f1c54d63749eda95f9fa6d49v442a';

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    apk: '',
    zip: '',
    user: process.env.JEECG_ADMIN_USER || 'admin',
    pass: process.env.JEECG_ADMIN_PASS || '123456',
    versionName: '1.0.1',
    versionCode: 101,
    minShellCode: 101,
    updateMode: 'apk',
    changelog: '第118轮全栈优化：安全契约、三端一致性与性能索引',
  };
  for (let i = 0; i < args.length; i++) {
    const a = args[i];
    if (a === '--apk') opts.apk = args[++i];
    else if (a === '--zip') opts.zip = args[++i];
    else if (a === '--user') opts.user = args[++i];
    else if (a === '--pass') opts.pass = args[++i];
    else if (a === '--version') opts.versionName = args[++i];
    else if (a === '--code') opts.versionCode = Number(args[++i]);
    else if (a === '--mode') opts.updateMode = args[++i];
    else if (a === '--changelog') opts.changelog = args[++i];
  }
  if (!opts.apk) throw new Error('缺少 --apk');
  opts.apk = resolve(opts.apk);
  if (opts.zip) opts.zip = resolve(opts.zip);
  return opts;
}

function md5(text) {
  return createHash('md5').update(text, 'utf8').digest('hex');
}

function encryptPassword(plain) {
  const key = CryptoJS.enc.Utf8.parse('1234567890adbcde');
  const iv = CryptoJS.enc.Utf8.parse('1234567890hjlkew');
  return CryptoJS.AES.encrypt(plain, key, {
    iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7,
  }).toString();
}

async function fetchCaptcha(checkKey) {
  const res = await fetch(`${BASE}/sys/randomImage/${checkKey}`);
  const json = await res.json();
  if (!json?.success) throw new Error(json?.message || '获取验证码失败');
  const prefix = md5(checkKey + SIGN_SECRET);
  const keys = execSync(`redis-cli KEYS "${prefix}*"`, { encoding: 'utf8' })
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean);
  if (!keys.length) throw new Error('Redis 中未找到验证码 key，请确认 Redis 与本机后端可用');
  const key = keys[0];
  return key.slice(prefix.length);
}

async function login(user, pass) {
  const checkKey = randomUUID().replace(/-/g, '');
  const captcha = await fetchCaptcha(checkKey);
  const body = {
    username: user,
    password: encryptPassword(pass),
    captcha,
    checkKey,
  };
  const res = await fetch(`${BASE}/sys/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const json = await res.json();
  if (!json?.success) throw new Error(json?.message || '登录失败');
  const token = json.result?.token;
  if (!token) throw new Error('登录响应无 token');
  return token;
}

async function uploadPackage(token, filePath, kind) {
  if (!existsSync(filePath)) throw new Error(`文件不存在: ${filePath}`);
  const buf = readFileSync(filePath);
  const form = new FormData();
  form.append('file', new Blob([buf]), basename(filePath));
  form.append('kind', kind);
  const res = await fetch(`${BASE}/homeai/app/version/upload`, {
    method: 'POST',
    headers: { 'X-Access-Token': token },
    body: form,
  });
  const json = await res.json();
  if (!json?.success) throw new Error(json?.message || `上传 ${kind} 失败`);
  return json.result || {};
}

async function saveVersion(token, payload) {
  const res = await fetch(`${BASE}/homeai/app/version/admin`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'X-Access-Token': token,
    },
    body: JSON.stringify(payload),
  });
  const json = await res.json();
  if (!json?.success) throw new Error(json?.message || '保存版本失败');
}

async function main() {
  const opts = parseArgs();
  console.log('[APP版本] 登录管理端...');
  const token = await login(opts.user, opts.pass);

  console.log('[APP版本] 上传 APK...');
  const apk = await uploadPackage(token, opts.apk, 'apk');

  let resource = {};
  if (opts.zip) {
    console.log('[APP版本] 上传 H5 zip...');
    resource = await uploadPackage(token, opts.zip, 'resource');
  }

  const body = {
    versionName: opts.versionName,
    versionCode: opts.versionCode,
    updateMode: opts.updateMode,
    forceUpdate: 0,
    minShellCode: opts.minShellCode,
    changelog: opts.changelog,
    enabled: 1,
    apkUrl: apk.stored || apk.url || '',
    apkSha256: apk.sha256 || '',
    resourceUrl: resource.stored || resource.url || '',
    resourceSha256: resource.sha256 || '',
  };

  console.log('[APP版本] 保存并开启对 APP 生效...');
  await saveVersion(token, body);
  console.log('[APP版本] 登记完成');
  console.log(`  versionName=${body.versionName}  versionCode=${body.versionCode}  mode=${body.updateMode}`);
  console.log(`  apkUrl=${body.apkUrl}`);
  if (body.resourceUrl) console.log(`  resourceUrl=${body.resourceUrl}`);
}

main().catch((e) => {
  console.error('[APP版本] 失败:', e.message || e);
  process.exit(1);
});
