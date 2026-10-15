# 来源、致谢与许可证说明

本文件说明极氪即刻（车机版）（Zeekr Shortcut (Car Version)）的每一部分从哪里来、
受什么许可约束。

---

## 1. EVCam —— 代码基座（GPL-3.0）

- **项目**：<https://github.com/suyunkai/EVCam>
- **作者**：suyunkai
- **许可证**：GNU General Public License v3.0
- **导入版本**：commit `0876b976bff554ef6bad2c65513cf0149f451d81`

### 用了什么

本仓库的第一个 commit 就是 EVCam 在上述版本的完整工作树。相机管线、分段录制、存储管理、
回放、保活、补盲等功能的起点都来自 EVCam，著作权归 suyunkai 及 EVCam 的其他贡献者所有。

### 导入时排除的内容

`.git`、`.claude`、`.cursor`、`app/release`（预编译产物）、`logcat_debug.txt`、`release.bat`。

### 本项目做了哪些改动

按 GPL-3.0 第 5(a) 条要求，改动一律以 diff 形式可见：第一个 commit 之后的全部提交就是完整的
改动记录，每个版本的要点见 [`CHANGELOG.md`](CHANGELOG.md)。

大的方向：

- **新增，本项目独立实现**：极氪合成流的识别、拆分与四宫格渲染（`app/src/main/java/com/kooo/evcam/zeekr/`），
  视频流配置与配置编辑器（`app/src/main/java/com/kooo/evcam/profile/`），电子后视镜，发送到手机。
- **删除**：钉钉 / 飞书 / Telegram 远程查看、心跳推图、MJPEG / RTP 推流，连同对应的 SDK 依赖。
- **重做**：录制参数的来源（改由视频流配置按路决定）、设置界面、主界面与悬浮按钮、视频回看。
- **包名与构建**：applicationId 改为 `io.github.dts88.zeekrshortcut`，签名可由环境变量覆盖，
  CI 构建与发布见 `.github/`。

平台事实笔记见 [`docs/zeekr-platform-notes.md`](docs/zeekr-platform-notes.md)，
其中逐条标注了来源与未验证项。

上游原始文档保留在 [`docs/`](docs/) 目录：
`upstream-EVCam-README.md`、`upstream-EVCam-CLAUDE.md`、`upstream-EVCam-AGENTS.md`、
`upstream-EVCam-免责声明.md`。

### 许可影响

GPL-3.0 是传染性（copyleft）许可证。因此**本项目整体以 GPL-3.0 发布**，
分发时必须提供对应源代码、保留版权与许可声明，且不得添加额外限制。

---

## 2. 第三方依赖

以下组件由 Gradle 在构建时拉取，随 APK 分发，各自受其上游许可证约束：

| 组件 | 许可证 |
|------|--------|
| AndroidX（AppCompat、Activity、ConstraintLayout、RecyclerView、CardView、Preference、SlidingPaneLayout、WorkManager） | Apache License 2.0 |
| Material Components | Apache License 2.0 |
| Gson | Apache License 2.0 |
| Glide | BSD / MIT / Apache 2.0（见其仓库 LICENSE） |
| NanoHTTPD | BSD 3-Clause |
| ZXing Core | Apache License 2.0 |

`docs/evcam-upstream-douyin.jpg`、`docs/evcam-upstream-douyin2.png` 来自 EVCam，
是其补盲功能作者（抖音 @星星舰见）的反馈群二维码，随上游一同保留以维持署名。
应用里没有用到它们，2.0 之后从 `assets/` 挪到 `docs/`，不再打进 APK。

---

## 3. 与极氪（ZEEKR）的关系

**没有任何关系。** 本项目是独立的第三方实验性软件，与浙江极氪智能科技有限公司
及其关联方无隶属、合作或背书关系。「极氪」「ZEEKR」「7X」等名称仅用于说明本应用
适用于哪种车机环境，相关商标权归其各自所有者所有。

## Automatic blind spot camera (turn-signal side view)

The turn-signal side-view feature and projection tests were contributed by
[FerixzTanz in PR #4](https://github.com/dts88/zeekr-shortcut-car/pull/4), then adapted
to the current camera scheduler. They remain covered by the repository's GPL-3.0 license.

The historical Ferix source repository was `FerixzTanz/CAMCORDER`. On 2026-10-15
that URL was unavailable; the currently public
[FerixzTanz/zeekr-shortcut-car](https://github.com/FerixzTanz/zeekr-shortcut-car)
is an older upstream fork. Historical source hashes and links below are retained
for provenance; the imported code and original credit remain in this repository.

## Phone sharing improvements

Ported and adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER).
Original source authorship is retained in the port commit. Source commits: [1539ead](https://github.com/FerixzTanz/CAMCORDER/commit/1539ead497575565cd1830860af9daa5feea0fb6), [f00788a](https://github.com/FerixzTanz/CAMCORDER/commit/f00788a67d1a84074fb606ea684429e5575bd69b), [78461a2](https://github.com/FerixzTanz/CAMCORDER/commit/78461a20feabf05c131a32967d6f9c0b670d58d9).

## Vehicle signal scanner

Ported and adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER).
Original source authorship is retained in the port commit. Source commits: [85bc7a3](https://github.com/FerixzTanz/CAMCORDER/commit/85bc7a3427f1e35262a97583b44a6f2f577d7ecb), [7bdde73](https://github.com/FerixzTanz/CAMCORDER/commit/7bdde73df5a11810f906f7f599bda9f02722472d), [ade63cf](https://github.com/FerixzTanz/CAMCORDER/commit/ade63cf563e324f3d0419225a7d19b098085582c).

## Hard-braking footage lock

Ported and adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER).
Original source authorship is retained in the port commit. Source commits: [75d8b85](https://github.com/FerixzTanz/CAMCORDER/commit/75d8b854cc5929dd8f67a4a06984189db1604e9c), [a2c7143](https://github.com/FerixzTanz/CAMCORDER/commit/a2c7143cb8d85db325ed2e17c053291a0cfd9f98), [978ba15](https://github.com/FerixzTanz/CAMCORDER/commit/978ba15c5b8c08e15113bb29ae4246c08f2ddd6c).

## Custom clip export

Ported and adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER).
Original source authorship is retained in the port commit. Source commits: [c611deb](https://github.com/FerixzTanz/CAMCORDER/commit/c611deb135acf89bcfde523162ef9eb695475a32), [d7dc998](https://github.com/FerixzTanz/CAMCORDER/commit/d7dc998b19f9377b49050246415b3db03c2c9385), [a2c7143](https://github.com/FerixzTanz/CAMCORDER/commit/a2c7143cb8d85db325ed2e17c053291a0cfd9f98).

## Recording follows the driver

Ported and adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER).
Original source authorship is retained in the port commit. Source commits: [289d66c](https://github.com/FerixzTanz/CAMCORDER/commit/289d66c35e7ee65b0bda5b5675ff17c33c4075e2), [657dcbd](https://github.com/FerixzTanz/CAMCORDER/commit/657dcbdddf55c9de1187de1db0e749c7596cca24), [f41028c](https://github.com/FerixzTanz/CAMCORDER/commit/f41028c89b1a3467c1352de3ed518d22f7f93396).

## Turn arrows on side cameras

Ported and adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER).
Original source authorship is retained in the port commit. Source commits: [45e07ae](https://github.com/FerixzTanz/CAMCORDER/commit/45e07ae7da636dea4fe11e9a193217d66410bc33), [2415bac](https://github.com/FerixzTanz/CAMCORDER/commit/2415bac9437b5304ef805da28e08fb237e096ddf).

## Night theme and floating record button

Ported and adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER).
Original source authorship is retained in the port commit. Source commits: [c91739e](https://github.com/FerixzTanz/CAMCORDER/commit/c91739e1dd317f16971e0a9a9296d80c29e63a3d), [beacea7](https://github.com/FerixzTanz/CAMCORDER/commit/beacea7fbb0407ece159a95fe6b4ccddb4716f24), [f931d11](https://github.com/FerixzTanz/CAMCORDER/commit/f931d11cb179d6584c0e747bc31ffe539bdd9711), [19986ac](https://github.com/FerixzTanz/CAMCORDER/commit/19986ac50ea698de0faf4bc5a92a93a2be2f19f1).

## Side-camera mirroring and HUD

The screen mirroring preference follows [FerixzTanz/CAMCORDER PR #14](https://github.com/FerixzTanz/CAMCORDER/pull/14), adapted to our independent popup and temporary Super mirror view. Original HUD source: [becbfe1](https://github.com/FerixzTanz/CAMCORDER/commit/becbfe10c6c9c1ebd1840245d68791c051c96c6a).

HUD Presentation, layout and lifecycle are ported from that PR with original source authorship retained. Adaptations use our current scheduler and stricter display selection for the unverified 009; diagnostic correction [426c5fa](https://github.com/FerixzTanz/CAMCORDER/commit/426c5fa3e97bc588d8bee8674c812fadc29a76b8) is included.

## In-app fork identification — 2026-10-14

The combined fork is maintained by **wysie**, primarily for personal use on a ZEEKR
009. Its About page identifies dts88’s original application and more thoroughly
tested upstream releases, preserves EVCam/GPL attribution, and acknowledges
FerixzTanz (Felix Tan)/CAMCORDER feature contributions. The retained optional
PayNow number **9271 6786** is explicitly attributed to **dts88**, not the fork
maintainer. Upstream 2.11.4-alpha (`e4d0e0d`) is merged without rewriting upstream
or existing contribution history.
