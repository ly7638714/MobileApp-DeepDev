<#
  .synopsis
    编译 Android 原生 debug APK，并复制到本地 APK 分发目录：
    D:\File_Owner\LY\kaogong-review-skill-main\06_MobileApp-DeepDev\APK

  用法（仓库根目录 06_MobileApp-DeepDev 下）：
    powershell -ExecutionPolicy Bypass -File "_打包APK分发.ps1"

  .notes
    每次写完代码后都运行一次，方便直接发到手机/微信给真机用户测试。
    输出文件使用 frontend/src/version.js 里的版本号，文件名形如：
    行测AI小助手-v3.8.231-debug.apk
#>
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot

$javaHome = 'E:\DevTools\jdk-21.0.2'
$androidHome = 'E:\AndroidSdk'
$gradle = 'E:\DevTools\gradle-8.9\bin\gradle.bat'

if (-not (Test-Path $gradle)) {
  Write-Error "找不到 Gradle：$gradle"
}
if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
  Write-Error "找不到 JDK：$javaHome"
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidHome

Write-Host '>>> 编译 debug APK ...' -ForegroundColor Cyan
& $gradle -p (Join-Path $here 'android-host') :app:assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'APK 编译失败' }

$srcApk = Join-Path $here 'android-host\app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $srcApk)) { throw "APK 未生成：$srcApk" }

$verFile = Join-Path $here 'frontend\src\version.js'
$ver = 'dev'
try {
  $m = [regex]::Match((Get-Content -Raw $verFile), "APP_VERSION\s*=\s*'([^']+)'")
  if ($m.Success) { $ver = $m.Groups[1].Value }
} catch {}

$outDir = Join-Path $here 'APK'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$dest = Join-Path $outDir ("行测AI小助手-v" + $ver + "-debug.apk")
Copy-Item -LiteralPath $srcApk -Destination $dest -Force

Write-Host ('✅ 已复制到：' + $dest) -ForegroundColor Green
Write-Host ('   大小：' + ('{0:N2} MB' -f ((Get-Item $dest).Length / 1MB))) -ForegroundColor Green
