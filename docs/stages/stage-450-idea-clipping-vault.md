# Stage 450 交付 Spec：灵感想法舱与智能剪藏知识库底层基建

---

## 1. 目标与背景

随着 Collecter 由“单一实物收纳”全面升维至“全态资产与数字大脑（Life Assets & Second Brain）”，本阶段（Stage 450）作为三阶段演进的首战，旨在构建「灵感想法舱 (Idea Vault)」与「智能剪藏知识库 (Clipping Vault)」的核心底层基建与交互界面：
1. **数据模型契约**：定义 `IdeaRecord` 与 `ClippingRecord`，具备 100% 本地离线私有沙盒持久化能力，支持标签、多色卡片、OCR 原文索引与跨维实体关联引用 `linkedAssetIds`；
2. **持久化仓储层**：实现 `IdeaVaultRepository` 与 `ClippingVaultRepository`，下沉至 `DataStore` 门面，并与桌面端 `DesktopDataStore` 保持 100% JSON 协议兼容；
3. **UI 控制器与交互**：落地 `IdeaVaultDialog.kt`（闪念卡片流、多色标签、置顶）、`ClippingVaultDialog.kt`（文章列表、截图 OCR 查看、沉浸式离线稍后读阅读器）；
4. **全维极速穿透检索**：升级 `GlobalSearchDialog.kt`，实现从单一入口毫秒级穿透检索「实物物资 + 12大专业馆 + 灵感想法 + 剪藏文章/截图 OCR 文本」；
5. **自动化测试**：编写全量数据持久化、往返序列化与穿透检索单元测试用例。

---

## 2. 详细设计与数据契约

### 2.1 灵感想法模型 `IdeaRecord`
- `id`: String (UUID)
- `content`: String (Markdown/纯文本正文)
- `tags`: List<String> (标签集合)
- `moodEmoji`: String (类别/心情 Emoji，默认 "💡")
- `colorHex`: String (卡片主题强调色)
- `isPinned`: Boolean (是否置顶)
- `linkedAssetIds`: List<String> (关联的实物/藏书/空间 ID)
- `createdAt`: Long
- `updatedAt`: Long

### 2.2 智能剪藏知识模型 `ClippingRecord`
- `id`: String (UUID)
- `title`: String (文章/剪藏标题)
- `originalUrl`: String (原始链接)
- `sourcePlatform`: String ("screenshot", "wechat", "zhihu", "web", "note")
- `fullMarkdown`: String (纯净离线正文 Markdown)
- `ocrRawText`: String (截图 OCR 提取全文索引)
- `localImagePaths`: List<String> (本地私有沙盒图片路径)
- `summary`: String (核心摘要/金句)
- `tags`: List<String>
- `readingProgress`: Float (0.0f ~ 1.0f)
- `isArchived`: Boolean
- `linkedAssetIds`: List<String>
- `capturedAt`: Long

---

## 3. 完成标准 (Acceptance Criteria)

- [ ] AC-1: 在 `DataModels.kt`（Android 端与桌面端）中落地 `IdeaRecord` 与 `ClippingRecord` 数据契约；
- [ ] AC-2: 在 `VaultRepositories.kt` 与 `DesktopDataStore.kt` 中实现全量 CRUD 与 JSON 序列化持久化；
- [ ] AC-3: 落地 `IdeaVaultDialog.kt` 闪念想法舱，支持卡片快速记录、编辑、置顶、标签过滤与颜色主题选择；
- [ ] AC-4: 落地 `ClippingVaultDialog.kt` 剪藏知识舱，支持文章阅读、截图预览、OCR 文本复制与归档管理；
- [ ] AC-5: 升级 `GlobalSearchDialog.kt`，支持在全局搜索框中一键毫秒级检索想法正文与剪藏 OCR 内容；
- [ ] AC-6: 编写单元测试验证新模型持久化与检索逻辑；
- [ ] AC-7: 门禁自检 `tools/selfcheck.ps1` 100% 绿灯，架构文档与 TODO 账本对齐。
