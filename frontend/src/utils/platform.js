// platform.js —— 安卓/多端「宿主适配桥」统一收口（MobileApp-DeepDev 深度适配 v1.0）
// -----------------------------------------------------------------------------
// 设计目标：
//   1. 把一切「壳层/原生能力」集中在本模块，页面与工具代码只 import 这里；
//   2. 宿主可替换：A = HBuilderX 5+App（window.plus 运行时，当前）
//                  B = 自建 Android WebView 宿主 / Capacitor（未来，实现同接口即可，业务零改动）
//   3. 浏览器/PWA / 非原生环境下所有能力自动降级，绝不抛异常、绝不阻塞。
// 约定：本文件不依赖 Vue/store；全部函数必须 try/catch 自包含。
// -----------------------------------------------------------------------------
/* global plus, navigator, document, window */

// ============ 宿主识别 ============
export function isPlusHost() {
  try { return !!(typeof window !== 'undefined' && window.plus && plus.os) } catch (e) { return false }
}
export function isAndroidUA() {
  try { return /android/i.test(navigator.userAgent || '') } catch (e) { return false }
}
export function isIOSUA() {
  try { return /iphone|ipad|ipod/i.test(navigator.userAgent || '') } catch (e) { return false }
}
/** hostKind(): 'plus' | 'browser'（未来: 'capacitor' / 'twa'） */
export function hostKind() {
  if (isPlusHost()) return 'plus'
  return 'browser'
}
/** 汇总平台快照，供设置页/调试显示 */
export function platformInfo() {
  const kind = hostKind()
  let os = 'web'
  let ver = ''
  try {
    if (kind === 'plus' && plus.os) { os = String(plus.os.name || 'android').toLowerCase(); ver = String(plus.os.version || '') }
  } catch (e) {}
  return { kind, isApp: kind !== 'browser', os, ver, isAndroid: kind === 'plus' ? os === 'android' : isAndroidUA(), isIOS: kind === 'plus' ? os === 'ios' : isIOSUA() }
}

// ============ 文件（plus.io / 浏览器降级） ============
/** 5+ 运行时 Download 目录相对根（Android 10+ 分区存储下亦由 5+ 运行时桥接） */
export const DOWNLOADS_REL = '_downloads/'
export function downloadsAbsRoot() {
  try { return plus.io.convertLocalFileSystemURL(DOWNLOADS_REL) } catch (e) { return '' }
}
/** 把相对路径转为绝对路径用于展示/plus.gallery 等 */
export function toAbsolute(rel) {
  try { return plus.io.convertLocalFileSystemURL(rel) } catch (e) { return rel || '' }
}
/** 在 _downloads 下确保目录存在（返回 DirectoryEntry） */
export function ensureDownloadsDir(subDir) {
  return new Promise((resolve, reject) => {
    if (!isPlusHost()) return reject(new Error('非原生宿主'))
    plus.io.resolveLocalFileSystemURL(DOWNLOADS_REL, (root) => {
      if (!subDir) return resolve(root)
      root.getDirectory(subDir, { create: true }, (dir) => resolve(dir), (e) => reject(e || new Error('建目录失败')))
    }, (e) => reject(e || new Error('无法访问 Download')))
  })
}
/** 写文本文件到 _downloads/<relPath>（自动建父目录），返回绝对路径 */
export function writeTextFile(relPath, text) {
  return new Promise((resolve, reject) => {
    if (!isPlusHost()) return reject(new Error('非原生宿主'))
    const seg = String(relPath || '').split('/').filter(Boolean)
    const name = seg.pop()
    if (!name) return reject(new Error('文件名缺失'))
    plus.io.resolveLocalFileSystemURL(DOWNLOADS_REL, (root) => {
      const walk = (dir, idx) => {
        if (idx >= seg.length) return dir.getFile(name, { create: true }, (fe) => {
          fe.createWriter((w) => { w.onwrite = () => resolve(toAbsolute(DOWNLOADS_REL + relPath)); w.onerror = () => reject(new Error('写入失败')); w.write(text) },
            () => reject(new Error('创建写入器失败')))
        }, (e) => reject(new Error('创建文件失败：' + ((e && e.message) || e))))
        dir.getDirectory(seg[idx], { create: true }, (d) => walk(d, idx + 1), (e) => reject(new Error('建目录失败：' + ((e && e.message) || e))))
      }
      walk(root, 0)
    }, (e) => reject(new Error('无法访问 Download：' + ((e && e.message) || e))))
  })
}
/** 写二进制文件（Blob/ArrayBuffer）到 _downloads/<relPath>，返回绝对路径 */
export function writeBlobFile(relPath, blob) {
  return new Promise((resolve, reject) => {
    if (!isPlusHost()) return reject(new Error('非原生宿主'))
    const seg = String(relPath || '').split('/').filter(Boolean)
    const name = seg.pop()
    if (!name) return reject(new Error('文件名缺失'))
    const reader = new FileReader()
    reader.onload = () => {
      plus.io.resolveLocalFileSystemURL(DOWNLOADS_REL, (root) => {
        const walk = (dir, idx) => {
          if (idx >= seg.length) return dir.getFile(name, { create: true }, (fe) => {
            fe.createWriter((w) => { w.onwrite = () => resolve(toAbsolute(DOWNLOADS_REL + relPath)); w.onerror = () => reject(new Error('写入失败')); w.write(reader.result) },
              () => reject(new Error('创建写入器失败')))
          }, (e) => reject(new Error('创建文件失败：' + ((e && e.message) || e))))
          dir.getDirectory(seg[idx], { create: true }, (d) => walk(d, idx + 1), (e) => reject(new Error('建目录失败：' + ((e && e.message) || e))))
        }
        walk(root, 0)
      }, (e) => reject(new Error('无法访问 Download：' + ((e && e.message) || e))))
    }
    reader.onerror = () => reject(new Error('读取数据失败'))
    try { reader.readAsArrayBuffer(blob) } catch (e) { reject(e || new Error('readAsArrayBuffer 失败')) }
  })
}

// ============ 剪贴板 ============
/** 写剪贴板：plus->navigator.clipboard->execCommand 逐级降级；返回是否成功 */
export async function setClipboard(text) {
  const t = String(text == null ? '' : text)
  if (!t) return false
  if (isPlusHost()) {
    try {
      const main = plus.android.runtimeMainActivity()
      const Context = plus.android.importClass('android.content.Context')
      const ClipData = plus.android.importClass('android.content.ClipData')
      const cm = main.getSystemService(Context.CLIPBOARD_SERVICE)
      cm.setPrimaryClip(ClipData.newPlainText('xc', t))
      return true
    } catch (e) { /* 继续降级 */ }
  }
  try { if (navigator.clipboard && navigator.clipboard.writeText) { await navigator.clipboard.writeText(t); return true } } catch (e) {}
  try {
    const ta = document.createElement('textarea')
    ta.value = t
    ta.style.cssText = 'position:fixed;left:-9999px;top:0;opacity:0'
    document.body.appendChild(ta)
    ta.focus(); ta.select()
    const ok = document.execCommand && document.execCommand('copy')
    document.body.removeChild(ta)
    return !!ok
  } catch (e) { return false }
}
/** 读剪贴板（多数 WebView 下不可用；plus 返回字符串，失败返回 null） */
export async function getClipboard() {
  if (isPlusHost()) {
    try {
      const main = plus.android.runtimeMainActivity()
      const Context = plus.android.importClass('android.content.Context')
      const cm = main.getSystemService(Context.CLIPBOARD_SERVICE)
      const d = cm.getPrimaryClip()
      if (d && d.getItemCount() > 0) { const it = d.getItemAt(0); const txt = it.coerceToText(main); if (txt) return String(txt) }
    } catch (e) {}
  }
  try { if (navigator.clipboard && navigator.clipboard.readText) return await navigator.clipboard.readText() } catch (e) {}
  return null
}

// ============ 系统分享（Android Intent.ACTION_SEND） ============
/** 文本分享：拉起系统分享面板（plus 宿主走 Intent，浏览器走 navigator.share 降级） */
export function shareText(text, title) {
  const t = String(text == null ? '' : text)
  if (!t) return Promise.resolve(false)
  if (isPlusHost()) {
    return new Promise((resolve) => {
      try {
        const main = plus.android.runtimeMainActivity()
        const Intent = plus.android.importClass('android.content.Intent')
        const i = new Intent(Intent.ACTION_SEND)
        i.setType('text/plain')
        i.putExtra(Intent.EXTRA_TEXT, t)
        if (title) i.putExtra(Intent.EXTRA_SUBJECT, String(title))
        main.startActivity(Intent.createChooser(i, '分享到'))
        resolve(true)
      } catch (e) { resolve(false) }
    })
  }
  try { if (navigator.share) return navigator.share({ title: title || '行测AI', text: t }).then(() => true, () => false) } catch (e) {}
  return Promise.resolve(false)
}

// ============ 硬件返回键（安卓物理返回/手势） ============
const _backHandlers = new Set()
/** 注册返回键消费回调：返回 true 表示已处理（阻止退出/默认）；返回注册的注销函数 */
export function onHardwareBack(fn) {
  if (typeof fn === 'function') _backHandlers.add(fn)
  return () => { _backHandlers.delete(fn) }
}
/** 触发已注册回调链，返回是否被消费 */
export function emitHardwareBack() {
  let consumed = false
  for (const fn of Array.from(_backHandlers)) { try { if (fn() === true) consumed = true } catch (e) {} }
  return consumed
}
export function exitApp() {
  try { if (isPlusHost()) plus.runtime.quit(); return true } catch (e) { return false }
}
let _lastBackAt = 0
let _backInstalled = false
/**
 * 安装 5+ 宿主返回键策略：先交已注册回调消费；未消费 → 双击退出（1.6s 内再按一次）。
 * 仅需在应用入口调用一次；浏览器下自动 no-op。
 * @param {object} [opt] { delay, onFirstBack } delay=双击间隔毫秒；onFirstBack 提示文案函数
 */
export function installPlusBackBehavior(opt) {
  if (!isPlusHost() || _backInstalled) return
  _backInstalled = true
  const delay = (opt && opt.delay) || 1600
  const onFirstBack = (opt && opt.onFirstBack) || null
  try {
    plus.key.addEventListener('backbutton', () => {
      if (emitHardwareBack()) return
      const now = Date.now()
      if (now - _lastBackAt < delay) { exitApp() } else {
        _lastBackAt = now
        try { if (onFirstBack) onFirstBack() } catch (e) {}
      }
    })
  } catch (e) {}
}
/** 卸载返回键策略（供设置页开关） */
export function uninstallPlusBackBehavior() {
  if (!_backInstalled) return
  _backInstalled = false
  try { plus.key.removeEventListener('backbutton', null) } catch (e) {}
}

// ============ 其它系统能力 ============
/** 震动（毫秒）。plus 宿主优先，浏览器降级 navigator.vibrate */
export function vibrate(ms) {
  const d = Number(ms) || 20
  try { if (isPlusHost() && plus.device && plus.device.vibrate) { plus.device.vibrate(d); return true } } catch (e) {}
  try { if (navigator.vibrate) { navigator.vibrate(d); return true } } catch (e) {}
  return false
}
/** 系统原生 toast（5+）；非原生返回 false，由前端自有 toast 兜底 */
export function nativeToast(msg) {
  try { if (isPlusHost() && plus.nativeUI && plus.nativeUI.toast) { plus.nativeUI.toast(String(msg || '')); return true } } catch (e) {}
  return false
}
/** 外部浏览器/URL 打开（应用内自更新页、教程页等），失败返回 false */
export function openExternal(url) {
  try { if (isPlusHost() && plus.runtime && plus.runtime.openURL) { plus.runtime.openURL(String(url)); return true } } catch (e) {}
  try { const w = window.open(String(url), '_blank'); if (w) return true } catch (e) {}
  return false
}
/** 状态栏高度（px，非沉浸时通常为 0） */
export function statusbarHeight() {
  try { if (isPlusHost() && plus.navigator && plus.navigator.getStatusbarHeight) return plus.navigator.getStatusbarHeight() } catch (e) {}
  return 0
}
/** 运行时版本号（5+ 为 Runtime 版本；自定义宿主可注入 window.__APP_VERSION__） */
export function runtimeVersion() {
  try { if (typeof window !== 'undefined' && window.__APP_VERSION__) return String(window.__APP_VERSION__) } catch (e) {}
  try { if (isPlusHost() && plus.runtime && plus.runtime.version) return String(plus.runtime.version) } catch (e) {}
  return ''
}
/** 把 5+ 能力探测结果暴露到 window.__PLATFORM__，便于调试（设置页可显示） */
export function exposePlatform() {
  try { if (typeof window !== 'undefined') window.__PLATFORM__ = platformInfo() } catch (e) {}
}

export default {
  isPlusHost, hostKind, platformInfo, isAndroidUA, isIOSUA,
  downloadsAbsRoot, toAbsolute, writeTextFile, writeBlobFile,
  setClipboard, getClipboard, shareText,
  onHardwareBack, emitHardwareBack, exitApp, installPlusBackBehavior, uninstallPlusBackBehavior,
  vibrate, nativeToast, openExternal, statusbarHeight, runtimeVersion, exposePlatform
}
