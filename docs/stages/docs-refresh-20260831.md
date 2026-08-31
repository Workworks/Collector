# 2026-08-31 项目重新加载与文档同步

## 范围

按用户要求重新读取当前 Collector 工作区并更新 docs。仅文档修改：不修改业务代码、版本、签名、构建配置、测试或验收脚本，不提交/推送、不重新发版、不启动或停止公网服务，不读取 WebDav 配置中的凭据。

当前 Git HEAD 仍为 `9b7ed30`，有大量既有未提交与未跟踪文件。Git diff 相对的是已提交 v4.3.0，不能把其中全部差异认定为用户这一次新增；本轮结合当前源码、文件时间和新增联调证据核对。命令级 safe.directory 不改变全局 Git 设置。

## 已确认的当前变化

| 结论 | 文件证据 | 文档处理 |
| --- | --- | --- |
| 三模块 app/desktop/shared；Android 4.3.3/40，桌面 4.3.2；Android 仍是 debug 签名配置 | [settings](../../settings.gradle.kts:24)、[Android 构建配置](../../app/build.gradle.kts:10)、[桌面配置](../../desktop/build.gradle.kts:7) | 当前阶段明确分端版本，不继续声称三端一致 |
| Android 连接探测使用 HEAD，404 代表当前路径可达且尚无备份；上传下载仍是 PUT/GET | [WebDavSyncHelper](../../app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:31) | 说明 HEAD 不是写权限或完整协议验证 |
| 桌面以 JDK HttpClient 发 PROPFIND；旧目录创建逻辑已移除，仍写 Collecter_Backup.json | [DesktopWebDavHelper](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/sync/DesktopWebDavHelper.kt:39) | 架构与操作说明同步协议差异及目录要求 |
| 已新增协议契约和显式启用的公网测试 | [WebDavConnectionTest](../../desktop/src/test/kotlin/com/kfaino/collector/desktop/sync/WebDavConnectionTest.kt:13)、[WebDavLiveTest](../../desktop/src/test/kotlin/com/kfaino/collector/desktop/sync/WebDavLiveTest.kt)、[WebDavLiveDeviceTest](../../app/src/androidTest/java/com/kfaino/diapertracker/WebDavLiveDeviceTest.kt) | 区分本轮本地测试和既有公网测试 |
| 公网认证及双端仓储/附件固定样例已通过；不再是“仅 localhost fixture” | [专项报告](webdav-integration-report.md)、[desktop-live.xml](evidence/webdav-20260831/desktop-live.xml)、[Android 结果](evidence/webdav-20260831/android-live-result.txt) | 给历史报告加后续链接，不覆写原时间/数字/失败日志 |
| 不能推导出物理手机、独立运营商网络、原生 GUI、长期稳定性已验收 | [联调限制](webdav-integration-report.md)、[发布签名报告](../releases/v4.3.3-report.md) | 保留发布与真实使用环境阻塞，测试成功不等于全量交付 |

相邻 WebDav 服务端修改仅依据该专项报告登记，没有把它纳入 Collector 模块，也未另行审计相邻仓库。报告中的公网探测是当时证据，不代表本轮或未来在线状态。

## 修改的文档

- `docs/stage-current.md`、`docs/TODO.md`：收敛重复的 452 状态，移走已完成待办；保留签名、真机、原生安装、固定地址与防误覆盖后续。旧文本完整封存于 `docs/stages/archive-20260831/`。
- `docs/README.md`：增加当前状态、WebDAV 操作、证据和发布阻塞导航；去掉未经本轮证明的“100% 互联”“51 篇全量”等声明。
- `docs/ARCHITECTURE.md`、`docs/USER_MANUAL.md`、新增 `docs/manuals/webdav-backup.md`：同步模块边界、备份操作和错误诊断；强调完整备份覆盖与 P2P 合并不同。
- `docs/BLOCKERS.md`、`docs/product-direction.md`：补原 APK 签名、Native 工具链和固定公网条件，不重复实施已经通过的公网样例。
- `docs/capability-map.html`：保留原布局与交互，只更新状态导航与 WebDAV 卡片文案，修正“增量同步”误称。
- `docs/baseline.md`、Stage 453 与 v4.3.3 历史报告：只新增后续验收导航，保留原始诊断和结果，不回写历史为全绿。

## 本轮验证

共享测试 12 通过；桌面 15 项中 13 通过、2 项公网测试显式跳过，0 失败。使用 `--offline --rerun-tasks`，并清空测试进程的两个 WebDAV 配置环境变量，未重新连接公网备份。见 [原始日志](evidence/docs-refresh-20260831/unit-tests.log)、[计数](evidence/docs-refresh-20260831/test-summary.json)。

本轮 Android assembleRelease 成功、45 项单测通过；selfcheck 退出 1（5/6），唯一失败为既有 Android/README 4.3.3 与 desktop 4.3.2 版本分离。对应 TECH_DEBT_AUDIT P0-2 停止假发版约束，不是本轮文档修改引入；不为消除失败更新桌面版本。共 70 项通过、2 项公网测试跳过，不能报成 72 项全通过。已有公网专项的 15 项通过和 Android 设备 1 项通过只是读取既有证据，不合并为本轮实测数。未运行 GUI/设备/公网测试，未重测旧 APK 签名或 MSVC 可用性；发布阻塞保持最近验收状态，不作已解除推断。

文档检查范围：本次编辑的 Markdown 内本地文件目标（忽略网络 URL、锚点与旧绝对路径），另检查 HTML 新增导航目标；这不是整个仓库所有链接/锚点的 100% 可达验证。刷新前后对 source/config/scripts/tools 和根文件做组合 SHA-256，确认本轮没有改动非 docs 源文件。

## 原始自检输出

桌面状态：已跟进当前 Helper 源码与本地测试；本轮不进行原生 GUI 或公网重测。

```text

=== GEMINI.md §4 交付前自检 ===

[1/6] 编译 assembleRelease ...
[2/6] 单元测试 testReleaseUnitTest ...
[3/6] 检查布局硬编码颜色 ...
[4/6] 检查空 catch ...
[5/6] 检查版本号一致性 ...
[6/6] 检查架构文档覆盖率 ...

--- 以下内容整段复制进汇报，不要改写、不要只贴通过项 ---

#### §4 交付前自检结果

<sub>selfcheck 指纹 `3556D7C69A03` · 若与上次汇报不同，说明验收脚本被改动过，必须在汇报中说明原因</sub>

| # | 检查项 | 实测结果 | 判定 |
| :-: | :--- | :--- | :--- |
| 1 | assembleRelease 编译 | BUILD SUCCESSFUL | ✅ 通过 |
| 2 | 单元测试 testReleaseUnitTest | 45 个用例，0 个失败 | ✅ 通过 |
| 3 | 布局硬编码颜色 | 共 8 处（scanner 相机遮罩 8 处属合理例外，其余 0 处） | ✅ 通过 |
| 4 | 空 catch（含仅注释） | 0 处 | ✅ 通过 |
| 5 | 版本号三处一致 | app=4.3.3 / README=4.3.3 / desktop=4.3.2 | ❌ 未通过 |
| 6 | 模块文档覆盖 | 109 个模块，0 个未记录 | ✅ 通过 |

> ⚠️ 有 **1** 项未通过。按铁律 5，必须在汇报里逐项说明：是本次改动引入的，还是既有欠账（附 TECH_DEBT_AUDIT 编号）。


<!-- ↑ 以上为脚本输出，属客观证据，一个字都不要改 -->

#### 桌面端状态（脚本无法判定，由你在汇报中另起一节填写）

> 脚本不知道你这次改了什么，所以这一节**不在上面的证据块里**，需要你自己写：
> 「已跟进」或「未跟进 + 具体原因」。禁止以修改版本号冒充全平台交付（铁律 2）。

```

范围校验见 [scope-check.json](evidence/docs-refresh-20260831/scope-check.json)，源文件组合哈希前后一致。
