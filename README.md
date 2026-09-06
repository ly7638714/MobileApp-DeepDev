# MobileApp-DeepDev · 行测AI助手（安卓深度适配独立项目）

> 个人自用的「行测复盘 + 智能答疑」AI 助手安卓版。**非原生**：UI 由 Vue3 前端渲染，安卓侧用 HBuilderX 5+App（WebView 壳）承载；策略是**前端零重写、把安卓框架适配层做深**。

本仓库自 2026-09-06 起**自包含**：前端源码 + 壳工程 + 构建脚本 + 全套文档都在这里，可独立构建与云打包。

## 仓库布局

    frontend/            ★ 前端唯一活跃源码（Vue3 + Vite + PWA + 200+ 单测）
    xingce-app-shell/    ★ HBuilderX 5+App 壳工程（manifest + assets + zhenti，可云打包）
    开发说明.md           ★ 详细开发文档：架构/适配方案/构建/打包/Android 15 实测矩阵
    _交接与安卓模拟器适配指南.md   模拟器安装/运行/适配检查清单
    _重建WEB并同步到APP.ps1        前端构建→同步壳工程→剥离壳内 PWA（唯一同步入口）

## 快速开始

1. 阅读 开发说明.md（架构与路线）→ _交接与安卓模拟器适配指南.md（模拟器执行清单）。
2. 改前端：进入 frontend/，npm install 后 npm run dev / npm test。
3. 同步与打包：仓库根运行 powershell -ExecutionPolicy Bypass -File "_重建WEB并同步到APP.ps1"，再用 HBuilderX 打开 xingce-app-shell 做「原生App-云打包」。
4. 验证：安装到 Android 15 模拟器/真机，按 开发说明.md 第 6 节实测矩阵逐项打勾。

## 架构一句话

    业务(100+ 组件) → utils/storage.js(持久化) → src/utils/platform.js(★宿主桥) → 安卓宿主

所有壳能力（剪贴板/分享/返回键/文件/震动/状态栏）统一收口在 src/utils/platform.js；业务代码禁止直接写 plus.*，宿主将来可从 5+App 平滑切换到自建 WebView 宿主。

## 红线

- 前端逻辑只改 frontend/；壳内 assets 是构建快照，由同步脚本刷新。
- unpackage/、*.keystore、certdata、.env* 一律不入库不上传。
- 本仓库为独立 git（origin = github.com/ly7638714/MobileApp-DeepDev），与主仓库 kaogong-review-skill-main 解耦、互不自动同步。