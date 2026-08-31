# 📦 Collecter v4.3.4 发布说明 (Release Notes)

> **发布日期**：2026-08-30  
> **版本号**：4.3.4 (Android ersionCode: 41 / Desktop 4.3.4)  
> **构建状态**：Android Release APK + Desktop Universal Jar + Native C++ WebView2 宿主

---

## 🌟 本次版本核心亮点 (Stage 510 ~ Stage 550 究极进化)

### 1. 🥽 空间计算与全息数字孪生 (Spatial Computing & Digital Twin)
- **ARCore 空间全息锚定** (ArSpatialAnchorHelper)：在真实物理空间定点虚拟收纳箱浮空卡片与透视；
- **多楼层 3D 穿梭导航** (MultiFloor3dNavigator)：复式/多层空间三维路径推演与寻物路径指引；
- **UWB 厘米级空间罗盘** (UwbSpatialCompassHelper)：空间三维距离与方位角精准导向；
- **3D Bin Packing 容积率推演** (BinPacking3dOptimizer)：装箱体积、超载检测与空间利用率科学分析。

### 2. 🔬 物联网（IoT）与主动环境感知 (Smart IoT & Ambient Sensing)
- **Matter / Home Assistant 局域网桥接** (LocalMatterBridge)：纯局域网免外网直连温湿度与智能传感器；
- **资产劣化主动预警** (AmbientDegradationMonitor)：实时监控相机镜头防霉(>60% 湿度)、茶酒陈化与药品高温变质；
- **电子墨水屏无线刷屏** (EInkLabelSyncHelper)：NFC/BLE 为收纳箱墨水屏一键下发清单图；
- **耗材用量流速预测** (ConsumptionVelocityPredictor)：根据消耗速率动态推演滤芯/猫粮耗尽日期。

### 3. 🧬 物质全生命周期与循环经济 (Circular Economics)
- **碳足迹计算器** (CarbonFootprintCalculator)：精算全家资产制造与使用碳排放与绿色评分；
- **熟人圈免信任互换** (P2pBarterRingProtocol)：去中心化借物账本与闲置以物易物；
- **标准化二手卡片** (ResaleCardGenerator)：一键生成转转/闲鱼验机长图卡片；
- **官方 Trade-in 比价** (TradeinValuationMatrix)：以旧换新官方折抵行情比价。

### 4. 🧠 自主认知代理与智能执行 (Autonomous Cognitive Agent)
- **端侧大模型资产管家** (OnDeviceLlmPlanner)：纯离线小模型资产规划与空间改造建议；
- **智能物流通知 Hook** (SmartNotificationHook)：电商物流通知解析与自动入库；
- **全息资产健康雷达** (AssetHealthRadar)：保修、时效、防灾与环境综合健康评分；
- **多币种实时换算** (MultiCurrencyConverter)：外币离线记账与汇率快照折算。

---

## 🛡️ 质量保证与测试门禁

- **单元测试**：Android 端 39 个用例全部通过，桌面端持久化与对撞测试 100% 绿灯；
- **交付门禁**：	ools/selfcheck.ps1 6 项检查（编译、单测、颜色、空 catch、版本一致性、架构文档覆盖率）全绿。
