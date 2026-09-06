# 重建 01_源码 dist，并同步进本 APP 工程（HBuilderX 5+App 的 www 资源）
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$app  = Join-Path $here 'xingce-app-shell'
# 独立项目：优先取“主仓库”的 01_源码；找不到则提示手动
$src  = Join-Path $here '..\01_源码'
if (-not (Test-Path (Join-Path $src 'package.json'))) {
  Write-Host '⚠️  未找到 ../01_源码（说明本目录已脱离主仓库）。'
  Write-Host '   若你已在主仓库跑过 vite build，请把 01_源码/dist 下的内容手动拷到本工程的 assets/、并更新 index.html/sw.js 等。'
  Write-Host '   或把本目录放进主仓库后再运行本脚本。'
  exit 1
}
$dist = Join-Path $src 'dist'
Push-Location $src
try {
  Write-Host '>>> vite build ...'
  & npx.cmd vite build
  if (-not (Test-Path $dist)) { throw 'dist 未生成' }
} finally { Pop-Location }
Write-Host '>>> 同步 dist -> APP 工程 ...'
Copy-Item -LiteralPath (Join-Path $dist '*') -Destination $app -Recurse -Force
Write-Host '✅ 已同步。可用 HBuilderX 打开工程并 云打包。'
