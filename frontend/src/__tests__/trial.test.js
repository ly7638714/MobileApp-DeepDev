import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const mem = new Map()
globalThis.localStorage = {
  getItem: (k) => (mem.has(k) ? mem.get(k) : null),
  setItem: (k, v) => mem.set(k, String(v)),
  removeItem: (k) => mem.delete(k),
  clear: () => mem.clear()
}

async function loadTrial(target) {
  vi.resetModules()
  vi.stubEnv('VITE_TRIAL_MODE', 'true')
  vi.stubEnv('VITE_TRIAL_TARGET', target)
  vi.stubEnv('VITE_TRIAL_DAYS', '5')
  return import('../utils/trial')
}

beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllEnvs()
  vi.resetModules()
})

describe('试用批次门禁配置', () => {
  it('iOS 试用 PWA 不再限制名额', async () => {
    const t = await loadTrial('ios-trial')
    expect(t.trialEnabled()).toBe(true)
    expect(t.trialTarget()).toBe('ios-trial')
    expect(t.trialSlotsText()).toBe('')
  })

  it('Android 测试 APK 不再限制名额', async () => {
    const t = await loadTrial('android-trial')
    expect(t.trialSlotsText()).toBe('')
  })

  it('统一邀请码为 XINGCE-5-SHIYONG，旧邀请码失效', async () => {
    const t = await loadTrial('ios-trial')
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-18T23:59:59+08:00'))
    expect(t.trialLocked()).toBe(true)
    vi.setSystemTime(new Date('2026-09-19T00:00:00+08:00'))
    expect(t.trialLocked()).toBe(true)
    expect(t.trialUnlock('XINGCE-LY13-3')).toEqual({ ok: false, reason: 'badcode' })
    expect(t.trialUnlock('XINGCE-5-SHIYONG').ok).toBe(true)
    expect(t.trialLocked()).toBe(false)
  })

  it('固定体验窗口为 2026-09-19 00:00 到 2026-09-23 23:59:59（+08:00）', async () => {
    const t = await loadTrial('ios-trial')
    expect(t.trialStartTs()).toBe(Date.parse('2026-09-19T00:00:00+08:00'))
    expect(t.trialExpiryTs()).toBe(Date.parse('2026-09-23T23:59:59+08:00'))
  })

  it('截止后强制 expired', async () => {
    const t = await loadTrial('ios-trial')
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-23T23:59:59+08:00'))
    expect(t.trialExpired()).toBe(false)
    vi.setSystemTime(new Date('2026-09-24T00:00:00+08:00'))
    expect(t.trialExpired()).toBe(true)
  })
})
