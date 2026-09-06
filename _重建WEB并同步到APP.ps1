# 重建前端 (frontend/) 并同步进 5+App 壳工程 (xingce-app-shell/)，并剥离壳内无意义的 PWA/ServiceWorker
# 用法:
#   powershell -ExecutionPolicy Bypass -File "_重建WEB并同步到APP.ps1"             # 构建并同步（正式）
#   powershell -ExecutionPolicy Bypass -File "_重建WEB并同步到APP.ps1" -SkipBuild # 仅用已有 frontend/dist 同步
# 说明（MobileApp-DeepDev 自包含后）：
#   - 前端唯一活跃源码在本仓库 frontend/（npm install 后 npm run build 出 dist）；
#   - 云打包用 xingce-app-shell/（HBuilderX 打开该目录 → 发行 → 原生App-云打包）。
param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$fe  = Join-Path $here 'frontend'
$app = Join-Path $here 'xingce-app-shell'
$dist = Join-Path $fe 'dist'
if (-not (Test-Path (Join-Path $fe 'package.json'))) {
  Write-Host '⚠️  缺少 frontend/package.json —— 请先确认本仓库完整（frontend/ 未裁剪）。'
  exit 1
}
Push-Location $fe
try {
  if ($SkipBuild) {
    if (-not (Test-Path $dist)) { throw 'SkipBuild 已指定，但 frontend/dist 不存在' }
    Write-Host '>>> 跳过构建，直接同步现有 dist ...'
  } else {
    Write-Host '>>> npm run build (frontend/) ...'
    & npm.cmd run build
    if (-not (Test-Path $dist)) { throw 'dist 未生成，构建失败' }
  }
} finally { Pop-Location }
Write-Host '>>> 同步 dist -> xingce-app-shell/ ...'
Copy-Item -LiteralPath (Join-Path $dist '*') -Destination $app -Recurse -Force
# 剥离壳内 PWA/ServiceWorker 残留（网页端 PWA 保留在 frontend；壳内 file:// 不生效，纯浪费）
Write-Host '>>> 剥离壳内 ServiceWorker 残留 ...'
@('registerSW.js','sw.js','workbox-0bb07689.js','manifest.webmanifest') | ForEach-Object {
  $p = Join-Path $app $_
  if (Test-Path $p) { Remove-Item $p -Force }
}
$idx = Join-Path $app 'index.html'
if (Test-Path $idx) {
  $html = [IO.File]::ReadAllText($idx)
  $html = [regex]::Replace($html, '<script[^>]*registerSW\.js[^>]*></script>', '')
  $html = [regex]::Replace($html, '<link[^>]*rel="manifest"[^>]*>', '')
  [IO.File]::WriteAllText($idx, $html, (New-Object System.Text.UTF8Encoding($false)))
}
Write-Host '✅ 完成。可用 HBuilderX 打开 xingce-app-shell 云打包。'
