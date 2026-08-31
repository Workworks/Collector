# 🩺 技术欠账索引 (Tech Debt Index)

> **这是索引，不是任务书。**
> 每条欠账的现状、做法与验收标准都在 `docs/debt/<编号>.md` 里，一条一卡。
>
> **执行某条任务时，只打开那一张卡** —— 一张卡几百 tokens，整份报告一万 tokens。
> 省下的上下文留给代码。

| 编号 | 标题 | 状态 | 卡片 |
| :--- | :--- | :--- | :--- |
| **P0-1** | 桌面端与移动端数据格式不通，代码里有假声明 | ✅ 已完成（采纳方案 A） | [P0-1.md](debt/P0-1.md) |
| **P0-2** | 停止假发版 | 🟢 持续约束守则 | [P0-2.md](debt/P0-2.md) |
| **P0-3** | 消除静默吞异常 | ✅ 已完成 | [P0-3.md](debt/P0-3.md) |
| **P1-1** | README 版本号与特性对齐 | ✅ 已完成 | [P1-1.md](debt/P1-1.md) |
| **P1-2** | ARCHITECTURE.md 模块补录 | ✅ 已完成（76/76 100% 覆盖） | [P1-2.md](debt/P1-2.md) |
| **P1-3** | 更新架构图 | ✅ 已完成（Mermaid 六层架构图） | [P1-3.md](debt/P1-3.md) |
| **P2-1** | 清理布局硬编码颜色 | ✅ 已完成（85 处硬编码清零） | [P2-1.md](debt/P2-1.md) |
| **P2-2** | 抽象 8 个雷同的收纳馆弹窗 | ✅ 已完成（VaultUiHelper 抽象下沉） | [P2-2.md](debt/P2-2.md) |
| **P3-1** | 超长文件治理 | ✅ 已完成（全工程 0 个超 800 行文件） | [P3-1.md](debt/P3-1.md) |
| **P3-2** | DataStore 上帝对象拆分 | ✅ 全部 5 阶段已完成（降至 325 行） | [P3-2.md](debt/P3-2.md) |
| **P3-3** | 业务逻辑测试覆盖 | ✅ 已完成（21 个用例全绿） | [P3-3.md](debt/P3-3.md) |


---

## 已由审计直接修复（不要回退）

3 项高危安全缺陷，附 11 个回归测试（`app/src/test/`）：

| 缺陷 | 修复 |
| :--- | :--- |
| 第三方 CDN 代理优先于官方源（5 处） | 新增 `UpdateSource.kt`：官方直连永远第一，代理仅作 fallback |
| 动态 dex 无验签即加载 | `verifyDexSignature()` SHA256withRSA，**fail-closed** |
| 解压可路径穿越（Zip Slip） | 抽出 `PatchArchive.safeUnzip()`，加路径校验 + 条目数/体积上限 |

已做变异验证：摘掉 Zip Slip 校验后 2 个用例立即变红 —— 护栏有效，不是摆设。

> ⚠️ `PATCH_PUBLIC_KEY_B64` 默认为空 = **拒绝一切带 dex 的补丁**。
> 本仓库不存在生成 `patch.dex` 的脚本或 Gradle 任务，该通道从未投产，不影响现有功能。

---

## 当前健康度

跑 `.\tools\selfcheck.ps1` 看实时结果 —— **不要引用这里的数字，它会过期。**

规则与流程见 [`GEMINI.md`](../GEMINI.md)；每条规则的由来见 [`GEMINI_RATIONALE.md`](GEMINI_RATIONALE.md)。
# 2026-08-31 验收状态补充

Stage 454 selfcheck 5/6：Android/README 为 4.3.3，desktop 为 4.3.2，版本一致项未通过。关联 P0-2“停止假发版”：不为通过检查而把未完成 Native 安装验收的桌面包冒充新版本；这是既有约束，不是本轮修改验收脚本造成。最新证据见 [阶段报告](stages/stage-454-report.md)。
