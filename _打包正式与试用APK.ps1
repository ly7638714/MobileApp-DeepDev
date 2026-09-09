<#
  .synopsis
    一次产出两个可同时安装的 APK：
      行测AI小助手-vX.X.XXX-debug.apk           正式版（com.xingce.ai）
      行测AI小助手-7天试用-vX.X.XXX-debug.apk   试用版（com.xingce.ai.trial）

    试用版首次输入正确邀请码后计时 7 天，到期自动锁定；
    两个 applicationId 不同，可装在同一个手机上互不覆盖。
  .usage
    pwsh -File "_打包正式与试用APK.ps1"
#>
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$fe = Join-Path $here 'frontend'
$dist = Join-Path $fe 'dist'
$officialAssets = Join-Path $here 'android-host\app\src\main\assets'
$trialAssets = Join-Path $here 'android-host\app\src\trialAssets'
$javaHome = 'E:\DevTools\jdk-21.0.2'
$androidHome = 'E:\AndroidSdk'
$gradle = 'E:\DevTools\gradle-8.9\bin\gradle.bat'

if (-not (Test-Path $gradle)) { Write-Error "找不到 Gradle：$gradle" }
if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) { Write-Error "找不到 JDK：$javaHome" }

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidHome

function Sync-WebTo($src, $dst) {
  if (Test-Path $dst) { Remove-Item -LiteralPath $dst -Recurse -Force }
  New-Item -ItemType Directory -Force -Path $dst | Out-Null
  Get-ChildItem -LiteralPath $src -Force | ForEach-Object { Copy-Item -Path $_.FullName -Destination $dst -Recurse -Force }
  @('registerSW.js', 'sw.js', 'workbox-0bb07689.js', 'manifest.webmanifest') | ForEach-Object {
    $p = Join-Path $dst $_
    if (Test-Path $p) { Remove-Item -LiteralPath $p -Force }
  }
  $idx = Join-Path $dst 'index.html'
  if (Test-Path $idx) {
    $html = [IO.File]::ReadAllText($idx)
    $html = [regex]::Replace($html, '<script[^>]*registerSW\.js[^>]*></script>', '')
    $html = [regex]::Replace($html, '<link[^>]*rel="manifest"[^>]*>', '')
    [IO.File]::WriteAllText($idx, $html, (New-Object System.Text.UTF8Encoding($false)))
  }
}

function Get-Ver {
  $m = [regex]::Match((Get-Content -Raw (Join-Path $fe 'src\version.js')), "APP_VERSION\s*=\s*'([^']+)'")
  if ($m.Success) { return $m.Groups[1].Value }
  return 'dev'
}

$ver = Get-Ver
$outDir = Join-Path $here 'APK'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# 1) 正式版
Write-Host '>>> 构建正式版前端 ...' -ForegroundColor Cyan
Push-Location $fe
try { & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw '正式版前端构建失败' } }
finally { Pop-Location }
Sync-WebTo $dist $officialAssets
Write-Host '>>> 编译正式版 APK ...' -ForegroundColor Cyan
& $gradle -p (Join-Path $here 'android-host') :app:assembleDebug --no-daemon "-PassetsDir=src/main/assets" "-PappId=com.xingce.ai" "-PappLabel=行测名师AI小助理" "-PverName=$ver-nh"
if ($LASTEXITCODE -ne 0) { throw '正式版 APK 编译失败' }
$officialApk = Join-Path $here 'android-host\app\build\outputs\apk\debug\app-debug.apk'
$officialDest = Join-Path $outDir "行测AI小助手-v$ver-debug.apk"
Copy-Item -LiteralPath $officialApk -Destination $officialDest -Force

# 原生宿主 main assets 保留给正式版；打试用版前先暂存正式资源，避免 Gradle 把两套同名 assets 合并
$officialCache = Join-Path $here 'android-host\app\src\_officialAssetsCache'
if (Test-Path $officialCache) { Remove-Item -LiteralPath $officialCache -Recurse -Force }
Copy-Item -LiteralPath $officialAssets -Destination $officialCache -Recurse -Force
Remove-Item -LiteralPath $officialAssets -Recurse -Force
try {

# 2) 试用版
Write-Host '>>> 构建试用版前端（邀请码门禁） ...' -ForegroundColor Cyan
Push-Location $fe
try { & npm.cmd run build -- --mode trial; if ($LASTEXITCODE -ne 0) { throw '试用版前端构建失败' } }
finally { Pop-Location }
Sync-WebTo $dist $trialAssets
Write-Host '>>> 编译试用版 APK（独立 applicationId） ...' -ForegroundColor Cyan
& $gradle -p (Join-Path $here 'android-host') :app:assembleDebug --no-daemon "-PassetsDir=src/trialAssets" "-PappId=com.xingce.ai.trial" "-PappLabel=行测名师AI小助理·试用版" "-PverName=$ver-trial"
if ($LASTEXITCODE -ne 0) { throw '试用版 APK 编译失败' }
$trialApk = Join-Path $here 'android-host\app\build\outputs\apk\debug\app-debug.apk'
$trialDest = Join-Path $outDir "行测AI小助手-7天试用-v$ver-debug.apk"
Copy-Item -LiteralPath $trialApk -Destination $trialDest -Force
} finally {
  # 无论成功失败都恢复正式版 assets，方便随后继续打正式版或直接安装旧 APK
  if (Test-Path $officialAssets) { Remove-Item -LiteralPath $officialAssets -Recurse -Force }
  Copy-Item -LiteralPath $officialCache -Destination $officialAssets -Recurse -Force
  Remove-Item -LiteralPath $officialCache -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ''
Write-Host ('✅ 正式版：' + $officialDest) -ForegroundColor Green
Write-Host ('✅ 试用版：' + $trialDest) -ForegroundColor Green
Write-Host ('   正式版大小：{0:N2} MB' -f ((Get-Item $officialDest).Length / 1MB)) -ForegroundColor Green
Write-Host ('   试用版大小：{0:N2} MB' -f ((Get-Item $trialDest).Length / 1MB)) -ForegroundColor Green
