# logo助手（LogoHelper）

读取 E听说 App 缓存目录中的 `content.json`，解析题目与标准答案，并通过悬浮窗显示，方便学生对照答案。

- 应用显示名：**logo助手**
- applicationId：`com.example.logohelper`（dev 版后缀 `.dev`）
- 最低支持：Android 8.0（API 26），target SDK 34
- 技术栈：Kotlin + Jetpack Compose（Material 3）+ 前台 Service 悬浮窗

## 功能概览

1. 扫描 `/storage/emulated/0/Android/data/com.ets100.secondary/files/Download/ETS_secondary/resource/` 下包含 `content.json` 的作业文件夹，按修改时间降序识别当前作业。
2. 解析两种题型：
   - `collector.role`（对话/听力选择题）：答案取 `std` 中去标签后最短的一项；
   - `collector.picture`（思维导图转述题）：答案取 `std[0].value`，题目显示 `topic`。
3. 悬浮窗（TYPE_APPLICATION_OVERLAY）显示题目与答案，支持拖动、折叠为小圆点、关闭。
4. 权限分三档自适应：Android 8-9 运行时存储权限、Android 10 `requestLegacyExternalStorage`、Android 11+ 所有文件访问权限；悬浮窗权限跳系统设置。

## 使用步骤

1. 先打开 **E听说 App 清除缓存**（我的/设置 → 清除缓存），重新打开目标作业并等待下载完成——否则可能扫描到旧作业。
2. 首次启动 logo助手：按界面卡片依次授予「存储/所有文件访问权限」「悬浮窗权限」。
3. 点「刷新列表」扫描作业，默认选中修改时间最新的文件夹，可手动切换。
4. 「解析预览」Tab 查看题目与最短答案；点「显示悬浮窗」在其他应用上方对照答案。

## 本地构建

```bash
./gradlew assembleDev          # dev 版 APK（debug 签名，可直接安装）
./gradlew assembleDebug        # debug 版
./gradlew assembleRelease      # release 版（未配置签名）
```

产物：`app/build/outputs/apk/dev/app-dev-*.apk`

## GitHub Actions 自动打包 dev 版

工作流文件：`.github/workflows/build-dev-apk.yml`

触发条件：push 到 `main`/`master`/`develop`、PR、手动 `workflow_dispatch`。
构建命令 `./gradlew assembleDev`（JDK 17），产物 artifact 名 `logo-helper-dev-apk`，保留 30 天。

推送到 GitHub 的流程：

```bash
git init
git add .
git commit -m "Initial commit: logo助手 Android app"
git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git push -u origin main
```

推送后在仓库 Actions 页面等待构建完成，在 Runs 页面底部 `logo-helper-dev-apk` 下载 APK 安装即可。

## 注意事项

- dev/debug 版使用 debug 签名，仅用于自用安装。
- Android 12+ 部分厂商 ROM 需要在系统设置中额外允许本应用「后台弹出界面/显示在其他应用上层」。
- E听说缓存路径若因版本变化，请确认 `resource/` 目录结构仍然有效。
