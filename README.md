# 06_手机APP深度开发（【独立项目】/ 独立 git 仓库）

> 本目录是一个**完全独立、与主仓库解耦**的 HBuilderX **5+App（HTML5+ 托管式）** 移动工程。
> 已 `git init` 成独立仓库：`manifest.json` + 前端资源 `assets/` + 真题数据 `zhenti/` 都在本仓库内，可独自打开/打包，不依赖主仓库 git。
> 说明：5+App = WebView 壳 + 前端资源（非原生 Android Java/Kotlin；如需真原生/uni-app 请另建路线）。

## 目录
| 路径 | 说明 |
|---|---|
| `行测AI助手-APP深度开发/` | HBuilderX 工程（`manifest.json` + `assets/` + `zhenti/`） |
| `README.md` | 本说明 |
| `_重建WEB并同步到APP.ps1` | 从 `01_源码` 重建前端并同步进本工程（**需主仓库存在**；本仓库单独打包不依赖它） |

## 作为独立项目怎么用
1. **独立开发外壳**：HBuilderX → 文件 → 导入 → 选本目录下的 `行测AI助手-APP深度开发`，改 `manifest.json`（图标/启动/权限/云打包）等。
2. **独立版本管理**：本目录已是独立 git 仓库（`git init` 完成），自己 `git add/commit/push` 即可，与主仓库 `kaogong-review-skill-main` 无关。
3. **更新前端**（若你同时在主仓库 `01_源码` 改前端）：在**主仓库根**运行
   `powershell -ExecutionPolicy Bypass -File "06_手机APP深度开发/_重建WEB并同步到APP.ps1"`
   脚本会先找 `..\01_源码`；找不到就提示你手动把 `01_源码/dist` 内容拷进 `行测AI助手-APP深度开发/`。
4. **云打包 APK**：HBuilderX 打开工程 → 发行 → 原生App-云打包 → 选 Android 证书。

## 说明与约定
- 本仓库 `.gitignore` 忽略 `unpackage/`（HBuilderX 构建缓存）、`.hbuilderx/`、`*.log`、`node_modules/`，其余（含 `assets/`、`zhenti/`）入库 → 克隆本仓库即可独立打开打包。
- 主仓库的 `04_安卓/行测AI助手` 仍由 `scripts/sync-dist.ps1` 三端同步，与本独立项目**互不影响**。
- 试用版：如需在本项目做试用 APK，请在主仓库 `01_源码` 单独 `vite build --mode trial --outDir dist-trial` 后把 `dist-trial` 内容同步进本工程，再云打包。
