# Stage 452 交付 Spec：跨维双向锚定与全平台深度联动

---

## 1. 目标与背景

在 Stage 450/451 建立并实现了「灵感想法舱」与「智能剪藏知识库」以及自动化采集通道后，数字资产与实体资产目前仍处于并行动态。本阶段（Stage 452）旨在打破“实物”与“想法/文章”之间的孤岛边界，实现**跨维双向锚定（Cross-Dimensional Dual-Linking）**与**全平台深度协同**：
1. **🔗 跨维双向锚定引擎 (`CrossLinkManager`)**：
   - 实体资产可关联多条想法或剪藏文章（如：买了一个单反相机 -> 关联“镜头选购指南”剪藏 + “构图技巧”想法）；
   - 在实物详情与编辑卡片中展示「🧠 关联认知与知识」标签卡；
   - 在灵感舱 / 剪藏知识库中反向穿透展示「📦 关联实体资产」，点击一键唤起实物详情；
2. **🖥️ 桌面端 Spotlight 极速穿透检索悬浮窗 (`DesktopSpotlightDialog`)**：
   - 桌面端支持毫秒级全局检索快捷窗口；
   - 桌面端 Web 控制台（8848 端口）扩展 `/api/v1/search?q=...` 全维穿透搜索接口；
3. **🌐 WebDAV 与 P2P 对撞全量覆盖**：
   - 验证 WebDAV 备份包中的 `ideas.json` 与 `clippings.json` 打包与多端还原；
   - 确保双端局域网对撞无缝融合实物关联关系；
4. **自动化测试与门禁自检**：
   - 编写跨维双向查询与索引测试用例；
   - 保证 `tools/selfcheck.ps1` 门禁 100% 绿灯。

---

## 2. 完成标准 (Acceptance Criteria)

- [x] AC-1: 落地 `CrossLinkManager.kt`（或等价跨维索引工具），支持 `Entry.id` <-> `IdeaRecord` / `ClippingRecord` 双向关联查询；
- [x] AC-2: 在 `EntryDetailDialog` / `AddEntryDialog` 中增加关联想法/剪藏的展示与选择；
- [x] AC-3: 在 `IdeaVaultDialog` / `ClippingVaultDialog` 中增加反向关联实物资产卡片与一键穿透查看；
- [x] AC-4: 桌面端与移动端对齐 `IdeaRecord` / `ClippingRecord` 模型，Web API 联合检索已就绪；
- [x] AC-5: 编写单元测试 `CrossDimensionalLinkTest.kt` 验证双向关联与穿透检索；
- [x] AC-6: 门禁自检 `tools/selfcheck.ps1` 100% 绿灯，架构文档与 TODO 同步更新。

---

## 3. 验收客观证据 (Verification Evidence)

1. **双端单元测试全部通过**：
   - 移动端 `app:testReleaseUnitTest`：31 个用例全部通过（含 `CrossDimensionalLinkTest`、`IdeaClippingVaultTest`、`ScreenshotClipperTest`）；
   - 桌面端 `desktop:test`：全量通过；
2. **`tools/selfcheck.ps1` 门禁 6 项全部绿灯**：
   - `assembleRelease 编译`: BUILD SUCCESSFUL (✅ 通过)
   - `单元测试 testReleaseUnitTest`: 31 个用例，0 个失败 (✅ 通过)
   - `布局硬编码颜色`: 合理例外 8 处，异常 0 处 (✅ 通过)
   - `空 catch（含仅注释）`: 0 处 (✅ 通过)
   - `版本号三处一致`: app=4.3.2 / README=4.3.2 / desktop=4.3.2 (✅ 通过)
   - `模块文档覆盖`: 105 个模块，0 个未记录 (✅ 通过)
