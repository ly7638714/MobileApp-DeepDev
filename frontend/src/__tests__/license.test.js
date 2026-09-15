import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { PLANS, licenseState, licenseInit, consumeLicensePoints, verifyLicenseCode } from '../utils/license'

const mem = new Map()
let nativeBackup = ''
globalThis.localStorage = {
  getItem: (k) => (mem.has(k) ? mem.get(k) : null),
  setItem: (k, v) => mem.set(k, String(v)),
  removeItem: (k) => mem.delete(k),
  clear: () => mem.clear()
}
globalThis.window = {
  xcnative: {
    licenseBackupRead: () => nativeBackup,
    licenseBackupWrite: (value) => { nativeBackup = String(value || ''); return true }
  }
}

const CODE = 'XC1.eyJ2IjoxLCJsaWQiOiJYQy1NVTJBWUFWQiIsInBsYW4iOiJwcm92aW5jZSIsImV4YW0iOiJ0ZXN0IiwiZXhwIjoyMDUxMjc5OTk5MDAwLCJkZXZpY2VzIjpbIlhDLVRFU1QtMDAwMC0wMDAxIl0sImZlYXR1cmVzIjpbImNoYXQiLCJxdWl6Iiwid3JvbmciLCJ0dHMiLCJzZWFyY2giLCJwZGYiXSwiaWF0IjoxNzg5NDU0NDA5MDk1fQ.LC8sKcMChU-YoMxpQUbSwRntaCoh8VlJkhjPBayPIvVsfC6W_AwsBD91Pu4V4do8G2hj0w-707wJQbFc3Lc36w'

describe('离线授权', () => {
  beforeEach(() => {
    localStorage.clear()
    nativeBackup = ''
    licenseState.ready = false
    licenseState.active = true
    licenseState.mode = 'none'
    licenseState.trialPoints = 30
    licenseState.message = ''
  })

  it('签名激活码只能在匹配设备上通过', async () => {
    expect((await verifyLicenseCode(CODE, 'XC-TEST-0000-0001')).ok).toBe(true)
    expect((await verifyLicenseCode(CODE, 'XC-TEST-9999-9999')).ok).toBe(false)
    expect((await verifyLicenseCode(CODE.replace('XC1.', 'XC2.'), 'XC-TEST-0000-0001')).ok).toBe(false)
  })

  it('首次运行自动开始 7 天 30 点试用', async () => {
    await licenseInit()
    expect(licenseState.mode).toBe('trial')
    expect(licenseState.active).toBe(true)
    expect(licenseState.trialPoints).toBe(30)
    expect(licenseState.trialDaysLeft).toBe(7)
  })

  it('试用点数会被逐次扣减，扣完后锁定 AI', async () => {
    await licenseInit()
    const r = consumeLicensePoints(30)
    expect(r.ok).toBe(true)
    expect(licenseState.trialPoints).toBe(0)
    expect(licenseState.active).toBe(false)
    expect(consumeLicensePoints(1).ok).toBe(false)
  })

  it('正式版套餐包含单月、连续月和季度订阅', async () => {
    expect(PLANS.map((p) => p.id)).toEqual(['month', 'continuous', 'quarter', 'gk', 'province'])
    expect(PLANS.find((p) => p.id === 'continuous')).toMatchObject({ price: 35, days: 30 })
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
  })

  it('覆盖安装后可从安卓私有备份恢复已订阅会员和设备码', async () => {
    nativeBackup = JSON.stringify({
      v: 1,
      license: JSON.stringify({ code: CODE, activatedAt: 1789454409095 }),
      device: 'XC-TEST-0000-0001',
      trial: JSON.stringify({ start: 1789454409095, points: 0, device: 'XC-TEST-0000-0001' })
    })
    await licenseInit()
    expect(licenseState.mode).toBe('paid')
    expect(licenseState.active).toBe(true)
    expect(licenseState.deviceCode).toBe('XC-TEST-0000-0001')
    expect(localStorage.getItem('xc_device_code_v1')).toBe('XC-TEST-0000-0001')
    expect(JSON.parse(localStorage.getItem('xc_offline_license_v1')).code).toBe(CODE)
  })

  it('本机授权变化会同步写入安卓私有备份', async () => {
    await licenseInit()
    const saved = JSON.parse(nativeBackup)
    expect(saved.v).toBe(1)
    expect(saved.device).toMatch(/^XC-/)
    expect(saved.trial).toBeTruthy()
  })

  it('试用构建不启用会员门禁，只由试用窗口控制', async () => {
    vi.resetModules()
    vi.stubEnv('VITE_TRIAL_MODE', 'true')
    const trialLicense = await import('../utils/license')
    await trialLicense.licenseInit()
    expect(trialLicense.licenseState.mode).toBe('trial')
    expect(trialLicense.licenseState.active).toBe(true)
    expect(trialLicense.requireLicense(999).ok).toBe(true)
  })
})
