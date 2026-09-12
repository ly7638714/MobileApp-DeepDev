import { describe, it, expect, vi, afterEach } from 'vitest'

async function loadTrial(target) {
  vi.resetModules()
  vi.stubEnv('VITE_TRIAL_MODE', 'true')
  vi.stubEnv('VITE_TRIAL_TARGET', target)
  vi.stubEnv('VITE_TRIAL_DAYS', '7')
  return import('../utils/trial')
}

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllEnvs()
  vi.resetModules()
})

describe('试用批次门禁配置', () => {
  it('iOS 试用 PWA 为 10 个名额', async () => {
    const t = await loadTrial('ios-trial')
    expect(t.trialEnabled()).toBe(true)
    expect(t.trialTarget()).toBe('ios-trial')
    expect(t.trialSlotsText()).toBe('10')
  })

  it('Android 测试 APK 为 20 个名额', async () => {
    const t = await loadTrial('android-trial')
    expect(t.trialSlotsText()).toBe('20')
  })

  it('邀请码统一为 XINGCE-LY13-3，旧邀请码失效', async () => {
    const t = await loadTrial('ios-trial')
    expect(t.trialUnlock('XINGCE-LY13-3').ok).toBe(true)
    expect(t.trialUnlock('XINGCE-7D-KG269')).toEqual({ ok: false, reason: 'badcode' })
  })

  it('绝对截止时间为 2026-09-19 19:24:14（+08:00）', async () => {
    const t = await loadTrial('ios-trial')
    expect(t.trialExpiryTs()).toBe(Date.parse('2026-09-19T19:24:14+08:00'))
  })

  it('截止后强制 expired', async () => {
    const t = await loadTrial('ios-trial')
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-19T19:24:15+08:00'))
    expect(t.trialExpired()).toBe(true)
  })
})
