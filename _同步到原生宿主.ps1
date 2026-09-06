# _同步到原生宿主.ps1 —— 把 5+App 壳产物(或 frontend/dist)拷入 android-host assets/www（Vue 前端不变）
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$src = Join-Path $here 'xingce-app-shell'
$hostWww = Join-Path $here 'android-host\app\src\main\assets\www'
if (-not (Test-Path (Join-Path $src 'index.html'))) { Write-Host '缺少 xingce-app-shell/index.html（先跑 _重建WEB并同步到APP.ps1 或放回已同步产物）'; exit 1 }
if (Test-Path $hostWww) { Remove-Item $hostWww -Recurse -Force }
New-Item -ItemType Directory -Force -Path $hostWww | Out-Null
Get-ChildItem -LiteralPath $src -Force | Where-Object { $_.Name -notin @('unpackage','manifest.json','README.md','icon.svg') } | ForEach-Object { Copy-Item -Path $_.FullName -Destination $hostWww -Recurse -Force }
@('registerSW.js','sw.js','workbox-0bb07689.js','manifest.webmanifest') | ForEach-Object { $p = Join-Path $hostWww $_; if (Test-Path $p) { Remove-Item $p -Force } }
$idx = Join-Path $hostWww 'index.html'
$html = [IO.File]::ReadAllText($idx)
$html = [regex]::Replace($html, '<script[^>]*registerSW\.js[^>]*></script>', '')
[IO.File]::WriteAllText($idx, $html, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ('✅ 已同步到 android-host assets/www（大小 {0:N1} MB）' -f ((Get-ChildItem $hostWww -Recurse -File | Measure-Object Length -Sum).Sum/1MB))