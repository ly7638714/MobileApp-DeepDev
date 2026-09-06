# 行测 AI 问答助手 Android 项目

这是个人自用的 HBuilderX 5+App 项目。`index.html` 已内置，API Key 不会写入源码或 APK；首次使用时请在 App 设置里填写。

## 打包 APK（推荐：HBuilderX 云打包）

1. 安装并打开 HBuilderX。
2. 选择“文件 - 打开目录”，选择本目录。
3. 双击 `manifest.json`，在可视化界面确认 App 名称、图标与 Android 权限；相机权限须保持启用。
4. 选择“发行 - 原生 App - 云打包”。
5. 选择 Android，个人测试可选公共测试证书；点击打包后下载 APK。
6. 将 APK 传到手机，允许“安装未知应用”，安装后打开即可。

## 手机内首次配置

1. 打开 App，点击右上角设置。
2. 选择接口：普通文字题可用 DeepSeek；图推视觉识别请选择 OpenAI 或其他兼容 OpenAI Chat Completions 的视觉接口。
3. 填入自己的 API Key、接口地址和模型名称，保存。
4. 图推题选择“视觉模式”，上传或拍摄完整题目；原图会以 `image_url` 发送给视觉模型。

Key 仅保存在这台手机的 App 本地存储中。清除 App 数据、卸载 App 或换手机后需要重新填写；不要把 Key 发给他人或写进项目文件。

## 权限说明

- 网络：调用 AI API、加载在线 OCR 资源。
- 相机：拍摄题目图片。也可以不授权，改为从相册选择。

`manifest.json` 使用 5+App 的 Android 配置；若 HBuilderX 的界面提示配置已迁移，以可视化权限配置为准后重新云打包。
