# Zeekr Companion

<img src="docs/branding/companion-icon.svg" alt="Zeekr Companion 标志" width="72" height="72">

为极氪车机提供相机录制、实时相机窗口和车辆工具，主要在车主的极氪 009 上开发和测试。

[**下载**](https://github.com/wysie/zeekr-companion/releases) · [展示网站](https://wysie.github.io/zeekr-companion/) · [English](README.md)

## 功能

- 选择环视、前后道路或座舱相机录制、拍照，分别设置画质与 USB 存储上限。
- 独立选择预览来源、布局和鱼眼校正，不改变录像来源。
- 超级后视镜、转向侧视窗、实验性拼接 360° 视图；HUD 输出取决于车辆支持。
- 照片与视频图库、录像锁定、片段导出及局域网手机分享。
- 浅色与深色主题、英语/马来语/简体中文、悬浮按钮动作及设置备份。

[查看浅色与深色截图](docs/screenshots/gallery.md)。截图为实际应用界面，相机画面是模拟场景。

## 安装

1. 从 [Releases](https://github.com/wysie/zeekr-companion/releases) 下载 `ZeekrCompanion-<版本号>.apk`，通过 App Lab 侧载。安装完成后按**返回**，再单独启动应用；点击安装器的“打开”可能导致安装器卡住。
2. 停车后，在**设置 → 录像与照片**检查相机选择，并在**存储**中选择 USB 盘。出现提示时点击**应用更改**。
3. 手动录制一个短片，再进入**视频**检查回放。启动自动录制和上车自动录制**默认关闭**。
4. 测试版更新需要在**设置 → 更新 → 更新通道**主动选择 **Alpha**；新安装默认仅检查正式版，即使安装的是 Alpha APK。

保留原应用 ID、签名与设置，支持兼容的原地更新。为兼容旧安装，签名密钥是公开的，请仅安装可信来源的版本。

## 已知限制

本分支为实验性个人项目，实车测试有限，其他车型及固件的兼容性尚未确认。
原厂相机功能优先。侧视窗由转向灯触发，**不是障碍物检测**；DIY 360° 对齐仅为近似结果。

## 安全须知

请停车后再操作。画面可能延迟、失真或中断，不可依据其判断车距或进行变道、倒车，
也不可替代原厂安全设备。出现影响原厂功能的异常时请停止使用。
分享录像或诊断报告前，请检查其中的个人信息。本软件按 GPL-3.0 以“现状”提供。

## 文档与致谢

[使用指南（英文）](docs/user-guide.md) · [技术文档](docs/README.md) · [中文更新日志](CHANGELOG.zh-CN.md) · [构建流程](FORK_WORKFLOW.md)

由 [wysie](https://github.com/wysie) 维护，基于
[dts88 的 Zeekr Shortcut](https://github.com/dts88/zeekr-shortcut-car)，源自
[EVCam](https://github.com/suyunkai/EVCam)，并包含
[FerixzTanz](https://github.com/FerixzTanz/CAMCORDER) 等人的贡献。
[完整致谢](NOTICE.md) · [GPL-3.0](LICENSE)。
