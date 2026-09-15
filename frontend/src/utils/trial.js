// utils/trial.js —— 试用模式门禁（仅当构建时开启 VITE_TRIAL_MODE=true 才生效，正式构建完全不受影响）
// ⚠️ 红线：本文件与 components/TrialGate.vue 属【正式源码】——由 VITE_TRIAL_MODE 编译期开关保护
//   （正式构建 Rollup 树摇剔除，零影响）。切勿按旧「试用版残留」红线删除/清理，否则破坏正式构建。
// 配置来源：固定时间窗口（试用 APK 与 iOS PWA 共用，统一邀请码、无名额限制）
// 说明：纯前端“软限制”——足够限制普通体验用户；到期后自动锁定，无法进入。
const FIXED_CODE = 'XINGCE-5-SHIYONG'
const FIXED_START = '2026-09-19T00:00:00+08:00'
const FIXED_EXPIRES = '2026-09-23T23:59:59+08:00'
const TARGET = String(import.meta.env.VITE_TRIAL_TARGET || 'ios-trial').trim()
const CFG = {
  enabled: import.meta.env.VITE_TRIAL_MODE === 'true',
  code: FIXED_CODE,
  start: FIXED_START,
  expires: FIXED_EXPIRES,
  days: 5
}
const LS_KEY = 'xc_trial_unlocked_v1'

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
  return ''
}
export function trialTarget() {
  return TARGET
}
export function trialExpiryTs() {
  return parseExpires()
}
function parseStart() {
  if (!CFG.start) return NaN
  return new Date(String(CFG.start)).getTime()
}
export function trialStartTs() {
  return parseStart()
}
function deadlineTs() { return parseExpires() }
export function trialExpiresText() {
  const t = deadlineTs()
  if (!Number.isFinite(t)) return '解锁后 ' + CFG.days + ' 天'
  const d = new Date(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
export function trialStartsText() {
  const t = parseStart()
  if (!Number.isFinite(t)) return '本周六'
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
  const start = parseStart()
  if (Number.isFinite(start) && Date.now() < start) return true
  try { return localStorage.getItem(LS_KEY) !== '1' } catch (e) { return true }
}
export function trialUnlock(code) {
  if (!CFG.enabled) return { ok: true, reason: '' }
  if (trialExpired()) return { ok: false, reason: 'expired' }
  const start = parseStart()
  if (Number.isFinite(start) && Date.now() < start) return { ok: false, reason: 'not-started' }
  if (String(code || '').trim() !== CFG.code) return { ok: false, reason: 'badcode' }
  try { localStorage.setItem(LS_KEY, '1') } catch (e) {}
  return { ok: true, reason: '' }
}
