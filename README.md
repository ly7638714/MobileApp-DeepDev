# 行测 AI 小助理 · 安卓端

这个仓库负责安卓 App 的深度开发与打包。界面仍是 Vue3 前端，但安卓侧不是简单套壳：文件选择、PDF、分享、返回键、剪贴板、状态栏都走原生桥，尽量让前端只关心业务。

## 目录

```text
frontend/           Vue3 前端源码，和网页端是同一套业务代码
xingce-app-shell/   HBuilderX 5+App 壳工程（云打包用）
android-host/       自建原生 WebView 宿主（本地 Gradle 打 debug APK）
APK/                打包产物，仅本机分发，不进 git
```

正式网页源码在上级目录的 `01_源码/`，那是另一个仓库；前端功能如需两端同步，我会把共有改动分别提交到两个仓库。

## 快速开始

```bash
cd frontend
npm install
npm test
npm run dev
```

## 两种 APK

开发收尾后统一用一条命令出两个安装包：

```powershell
pwsh -File _打包正式与试用APK.ps1
```

产物在 `APK/`：

- `行测AI小助手-v3.8.270-debug.apk`：正式版，包名 `com.xingce.ai`
- `行测AI小助手-7天试用-v3.8.270-debug.apk`：试用版，包名 `com.xingce.ai.trial`

两个包可以同时装在同一个手机，各自保存各自的数据。

试用版规则：

1. 打开先看到邀请码页。
2. 邀请码正确后，本机才开始算 7 天。
3. 到期自动锁定，重新打开只显示“本次体验已结束”。
4. 邀请码、名额、天数在 `frontend/.env.trial` 里改，模板是 `frontend/.env.trial.example`。

`.env.trial` 与 APK 都不进 git，避免邀请码泄漏。

## 构建脚本

- `_重建WEB并同步到APP.ps1`：把 `frontend/dist` 同步进 HBuilderX 壳。
- `_同步到原生宿主.ps1`：把壳内资源同步进 `android-host` 的 assets。
- `_打包正式与试用APK.ps1`：先构建正式前端、再构建试用前端，分别打出两个独立包名 APK。
- `_真题PDF入库.ps1`：把本地真题 PDF 打进 App，供 PDF 卷库离线使用。

本机正式构建需要：

- JDK 21：`E:\DevTools\jdk-21.0.2`
- Android SDK：`E:\AndroidSdk`
- Gradle 8.9：`E:\DevTools\gradle-8.9\bin\gradle.bat`

路径都写死在脚本顶部，换了电脑直接改脚本头部即可。

## 不要提交什么

- `frontend/node_modules/`、`frontend/dist/`
- `android-host/app/src/main/assets/`、`android-host/app/src/trialAssets/`（构建生成）
- `APK/`、`*.keystore`、`*.jks`、`.env*`（模板除外）
- HBuilderX 的 `unpackage/`、模拟器截图 `.shots/`

签名证书和 API Key 只存在于本机，我上传仓库前也会再检查一遍是否出现在 diff 里。
