// utils/trial.js —— 试用模式门禁（仅当构建时开启 VITE_TRIAL_MODE=true 才生效，正式构建完全不受影响）
// ⚠️ 红线：本文件与 components/TrialGate.vue 属【正式源码】——由 VITE_TRIAL_MODE 编译期开关保护
//   （正式构建 Rollup 树摇剔除，零影响）。切勿按旧「试用版残留」红线删除/清理，否则破坏正式构建。
// 配置来源：固定批次配置（iOS PWA 10 个名额；Android 试用 APK 20 个名额）
// 说明：纯前端“软限制”——足够限制普通体验用户；到期后自动锁定，无法进入。
const FIXED_CODE = 'XINGCE-LY13-3'
const FIXED_EXPIRES = '2026-09-19T19:24:14+08:00'
const TARGET = String(import.meta.env.VITE_TRIAL_TARGET || 'ios-trial').trim()
const CFG = {
  enabled: import.meta.env.VITE_TRIAL_MODE === 'true',
  code: FIXED_CODE,
  expires: FIXED_EXPIRES,
  slots: TARGET === 'android-trial' ? '20' : '10',
  days: Number(import.meta.env.VITE_TRIAL_DAYS) || 7
}
const LS_KEY = 'xc_trial_unlocked_v1'
const LS_START = 'xc_trial_started_v1'

export function trialEnabled() {
  return CFG.enabled
}
function parseExpires() {
  if (!CFG.expires) return NaN
  const raw = String(CFG.expires)
  // ISO 带 T/时区 直接解析；纯日期 'YYYY-MM-DD' 做兼容替换
  const d = raw.includes('T') ? new Date(raw) : new Date(raw.replace(/-/g, '/'))
  return d.getTime()
}
export function trialSlotsText() {
  if (!CFG.enabled || !CFG.slots) return ''
  return String(CFG.slots)
}
export function trialTarget() {
  return TARGET
}
export function trialExpiryTs() {
  return parseExpires()
}

function parseStarted() {
  try {
    const n = Number(localStorage.getItem(LS_START) || 0)
    return Number.isFinite(n) && n > 0 ? n : NaN
  } catch (e) {
    return NaN
  }
}
// 首次输对邀请码起算 7 天；同时配置了批次硬截止日期时取更早者
function deadlineTs() {
  const start = parseStarted()
  const abs = parseExpires()
  const user = Number.isFinite(start) ? start + CFG.days * 24 * 3600 * 1000 : NaN
  if (!Number.isFinite(user)) return abs
  return Number.isFinite(abs) ? Math.min(user, abs) : user
}
export function trialExpiresText() {
  const t = deadlineTs()
  if (!Number.isFinite(t)) return '解锁后 ' + CFG.days + ' 天'
  const d = new Date(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
export function trialExpired() {
  if (!CFG.enabled) return false
  const t = deadlineTs()
  return Number.isFinite(t) && Date.now() > t
}
export function trialLocked() {
  if (!CFG.enabled) return false
  if (trialExpired()) return true
  try {
    const hadUnlocked = localStorage.getItem(LS_KEY) === '1'
    // 老版本只写过解锁标记、没写起始时间：从这次补记开始计时，避免旧标记绕过 7 天限制
    if (hadUnlocked && !Number.isFinite(parseStarted())) localStorage.setItem(LS_START, String(Date.now()))
    if (!CFG.code) {
      if (!hadUnlocked) localStorage.setItem(LS_KEY, '1')
      if (!Number.isFinite(parseStarted())) localStorage.setItem(LS_START, String(Date.now()))
      return false
    }
    return !hadUnlocked
  } catch (e) { return true }
}
export function trialUnlock(code) {
  if (!CFG.enabled) return { ok: true, reason: '' }
  if (trialExpired()) return { ok: false, reason: 'expired' }
  if (!CFG.code) {
    try { localStorage.setItem(LS_KEY, '1') } catch (e) {}
    try { localStorage.setItem(LS_START, String(Date.now())) } catch (e) {}
    return { ok: true, reason: '' }
  }
  if (String(code || '').trim() === CFG.code) {
    try { localStorage.setItem(LS_KEY, '1') } catch (e) {}
    try { localStorage.setItem(LS_START, String(Date.now())) } catch (e) {}
    return { ok: true, reason: '' }
  }
  return { ok: false, reason: 'badcode' }
}
