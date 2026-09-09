<#
  .synopsis
    把手机端试用前端打包成 iOS 可直接“添加到主屏幕”的 PWA，
    产物放在仓库 ios-trial/<版本>/ 并由 GitHub jsDelivr 免费分发。
  .usage
    pwsh -File "_打包iOS试用PWA.ps1"
#>
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$fe = Join-Path $here 'frontend'
$dist = Join-Path $fe 'dist'

Push-Location $fe
try {
  & npm.cmd run build -- --mode trial
  if ($LASTEXITCODE -ne 0) { throw '试用 PWA 前端构建失败' }
} finally { Pop-Location }

$m = [regex]::Match((Get-Content -Raw (Join-Path $fe 'src\version.js')), "APP_VERSION\s*=\s*'([^']+)'")
$ver = if ($m.Success) { $m.Groups[1].Value } else { 'dev' }
$out = Join-Path $here ("ios-trial\" + $ver)

if (Test-Path $out) { Remove-Item -LiteralPath $out -Recurse -Force }
New-Item -ItemType Directory -Force -Path $out | Out-Null
Get-ChildItem -LiteralPath $dist -Force | ForEach-Object { Copy-Item -Path $_.FullName -Destination $out -Recurse -Force }

# iOS 网页入口不需要 Service Worker 注册，避免第三方 CDN 上出现缓存混乱
@('registerSW.js', 'sw.js', 'workbox-0bb07689.js') | ForEach-Object {
  $p = Join-Path $out $_
  if (Test-Path $p) { Remove-Item -LiteralPath $p -Force }
}
$idx = Join-Path $out 'index.html'
if (Test-Path $idx) {
  $html = [IO.File]::ReadAllText($idx)
  $html = [regex]::Replace($html, '<script[^>]*registerSW\.js[^>]*></script>', '')
  [IO.File]::WriteAllText($idx, $html, (New-Object System.Text.UTF8Encoding($false)))
}

$root = Join-Path $here 'ios-trial'
New-Item -ItemType File -Force -Path (Join-Path $root '.nojekyll') | Out-Null
$redirect = Join-Path $root 'index.html'
$html = '<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"><meta http-equiv="refresh" content="0;url=./' + $ver + '/index.html"><meta name="viewport" content="width=device-width, initial-scale=1"><title>行测AI·iOS试用</title></head><body style="background:#0f172a;color:#e2e8f0;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0">正在进入 iOS 试用版…</body></html>'
[IO.File]::WriteAllText($redirect, $html, (New-Object System.Text.UTF8Encoding($false)))

Write-Host ('✅ 已生成：' + $out) -ForegroundColor Green
Write-Host ('   分发入口：https://cdn.jsdelivr.net/gh/ly7638714/MobileApp-DeepDev@main/ios-trial/' + $ver + '/index.html') -ForegroundColor Green
