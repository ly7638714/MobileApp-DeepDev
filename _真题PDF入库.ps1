# _真题PDF入库.ps1 —— 把本地整理好的历年真题PDF打进 App（不入 Git）
# 用法: powershell -ExecutionPolicy Bypass -File _真题PDF入库.ps1 -Src <真题文件夹> [-OnlySync]
param([string]$Src = '', [switch]$OnlySync)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
if (-not $Src) { $Src = Join-Path $here '真题PDF源' }
$pub = Join-Path $here 'frontend\public\zhenti-pdf'
$shellZ = Join-Path $here 'xingce-app-shell\zhenti-pdf'
$wwwZ = Join-Path $here 'android-host\app\src\main\assets\www\zhenti-pdf'

function New-Manifest($dir) {
  $groups = @()
  foreach ($g in (Get-ChildItem $dir -Directory | Sort-Object Name)) {
    $files = Get-ChildItem $g.FullName -Filter *.pdf -File -Recurse | Sort-Object Name | ForEach-Object { $_.FullName.Substring($g.FullName.Length + 1).Replace('\','/') }
    if ($files.Count) { $groups += [pscustomobject]@{ name = $g.Name; files = @($files) } }
  }
  # 根目录散落 PDF → 归入 '其他/未分类'
  $loose = Get-ChildItem $dir -Filter *.pdf -File | ForEach-Object { $_.Name }
  if ($loose.Count) { $groups += [pscustomobject]@{ name = '未分类'; files = @($loose) } }
  $out = [pscustomobject]@{ generatedAt = (Get-Date -Format 'yyyy-MM-dd'); total = ($groups | ForEach-Object { $_.files.Count } | Measure-Object -Sum).Sum; groups = @($groups) }
  return ($out | ConvertTo-Json -Depth 6 -Compress)
}

if (-not (Test-Path $Src)) { Write-Host ('找不到源文件夹：' + $Src); exit 1 }

if (-not $OnlySync) {
  if (Test-Path $pub) { Remove-Item $pub -Recurse -Force }
  New-Item -ItemType Directory -Force -Path $pub | Out-Null
  Copy-Item -Path (Join-Path $Src '*') -Destination $pub -Recurse -Force
  Write-Host '已拷贝 PDF 到 frontend/public/zhenti-pdf'
}
if (-not (Test-Path $pub)) { Write-Host '缺少 frontend/public/zhenti-pdf，先去掉 -OnlySync 入库'; exit 1 }
$manifest = New-Manifest $pub
[IO.File]::WriteAllText((Join-Path $pub 'index.json'), $manifest, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ('清单：总 PDF ' + (($manifest | ConvertFrom-Json).total) + ' 份')

foreach ($dst in @($shellZ, $wwwZ)) {
  if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
  New-Item -ItemType Directory -Force -Path $dst | Out-Null
  Copy-Item -Path (Join-Path $pub '*') -Destination $dst -Recurse -Force
}
$mb = [math]::Round(((Get-ChildItem $pub -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
Write-Host ('✅ 完成：随包 PDF 共 ' + $mb + ' MB（已将 zhenti-pdf 同步进 5+壳 与 原生宿主 www）')
Write-Host '下一步：npm run build 后重打包 APK；PDF 不入 Git。'