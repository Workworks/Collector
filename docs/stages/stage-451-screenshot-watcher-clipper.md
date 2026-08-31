# Stage 451 交付 Spec：极速智能采集通道与系统截图无感监听器

---

## 1. 目标与背景

在 Stage 450 完成「灵感想法舱」与「剪藏知识库」底层模型与 UI 之后，本阶段（Stage 451）旨在消除“收藏与记录”的摩擦力，构建全自动化、无感知的智能采集通道：
1. **📸 系统截图无感监听器 (`ScreenshotWatcherHelper`)**：
   - 监听系统媒体库 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` 中的新增截图事件（过滤 `/Screenshots/` 路径）；
   - 支持开启/关闭截图监听开关，防重复处理（基于文件路径与时间戳去重）；
2. **🧠 端侧 ML Kit 离线 OCR 全文提取与自动归档 (`ScreenshotOcrProcessor`)**：
   - 自动将截图复制至应用私有沙盒（防止系统相册清理导致原图丢失）；
   - 调用端侧 Google ML Kit Text Recognition 提取截图全文字符；
   - 智能分析文本生成摘要与分类标签（如 "电商购物", "技术资料", "聊天备忘", "生活食谱"）；
   - 自动生成 `ClippingRecord` 并保存至沙盒知识库，发送状态栏通知或静默入库；
3. **🔗 网页与社交文章深度剪藏引擎 (`WebClipperEngine`)**：
   - 针对网页/知乎/微信/小红书分享链接，提取纯净正文文本并格式化为离线 Markdown；
   - 自动抓取文章标题并缓存离线快照；
4. **自动化测试与门禁**：
   - 编写 OCR 提取逻辑与网页清洗剪藏单元测试；
   - 门禁 `tools/selfcheck.ps1` 100% 绿灯。

---

## 2. 关键设计契约

### 2.1 截图监听与处理流程
```
[用户在系统任意 App 截屏] 
       │
       ▼ (ContentObserver 监听到 MediaStore 新增图片)
[ScreenshotWatcherHelper 过滤判断] (是否在 /Screenshots/ 目录且未被处理)
       │
       ▼ (后台协程/线程池执行)
[复制原图至 item_vault/ 沙盒] ──➔ [ML Kit 离线 OCR 识别提取全文]
       │
       ▼
[智能生成 ClippingRecord (title, ocrRawText, tags, localImagePaths)]
       │
       ▼
[存入 DataStore] ──➔ [发送前台通知/微提示: 📸 已自动收纳截图并完成 OCR 索引]
```

### 2.2 网页正文清洗引擎 `WebClipperEngine`
- `clipUrl(url: String, callback: (ClippingRecord?) -> Unit)`
- 抓取 HTML -> 剔除 `<script>`, `<style>`, `<nav>`, `<footer>` 等杂质 -> 提取 `<title>`, `<h1>..<h6>`, `<p>` -> 转换为结构化 Markdown 正文。

---

## 3. 完成标准 (Acceptance Criteria)

- [x] AC-1: 实现 `ScreenshotWatcherHelper.kt`，支持在 Android 8.0~14 上可靠监听系统截图事件并去重；
- [x] AC-2: 实现 `ScreenshotOcrProcessor.kt`，集成 ML Kit 离线识别，自动将截图生成 `ClippingRecord` 存入知识库；
- [x] AC-3: 实现 `WebClipperEngine.kt`，支持 URL 纯净正文提取与 Markdown 转换；
- [x] AC-4: 在 `SettingsStore` / UI 中增加「📸 截图无感自动收纳」配置开关；
- [x] AC-5: 编写单元测试验证网页正文提取与智能打标逻辑 (`ScreenshotClipperTest.kt` 5项用例全过)；
- [x] AC-6: 门禁自检 `tools/selfcheck.ps1` 100% 绿灯（45 个测试全部通过，架构文档覆盖 104 个模块）。
