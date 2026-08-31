# 阶段演化路线图 (Stage Roadmap)

---

## 阶段列表与状态

| Stage | 版本代号 | 核心交付主题 | 状态 | 报告链接 |
| :---: | :---: | :--- | :---: | :--- |
| **400** | v4.0.0 | 基础资产记账、折旧算法、空间平面图与三大基础收纳馆（卡券/证照/药箱） | `COMPLETED` | [stage-400.md](stage-roadmap.md) |
| **410** | v4.1.0 | 冰箱生鲜鲜度库、荣誉履历勋章馆、时光胶囊生活画册与实物借还催还凭证 | `COMPLETED` | [stage-410.md](stage-roadmap.md) |
| **420** | v4.2.0 | 四季换季衣橱、应急防灾生命线、工具设备维保、绿植水肥、萌宠健康、藏书阅读与茶窖名酿 12 馆完备 | `COMPLETED` | [stage-420.md](stage-roadmap.md) |
| **430** | v4.3.0 | 技术欠账清零（P0~P3 全部结清）、DataStore 门面解耦与安全不变量回归护栏 | `COMPLETED` | [stage-430.md](stage-roadmap.md) |
| **431** | v4.3.1 | 12 馆跨维全局搜索、今日时效待办看板、12 馆联合报表、图片沙盒清理与 2×2 时效预警 Widget | `COMPLETED` | [stage-431.md](stage-431.md) |
| **440** | v4.4.0-Spec | **桌面单机版架构立项与全套文档体系重构（对齐 capital-agent-system）** | `COMPLETED` | [stage-440-desktop.md](stage-440-desktop.md) |
| **441** | v4.4.0-M1 | 桌面单机版 `%LOCALAPPDATA%` 数据隔离、12 馆全量模型对齐与往返测试 | `COMPLETED` | [stage-440-desktop.md](stage-440-desktop.md) |
| **442** | v4.4.0-M2 | 桌面单机版 Native WebView2 C++ 宿主外壳、系统托盘与 NSIS 一键安装包 | `COMPLETED` | [stage-440-desktop.md](stage-440-desktop.md) |
| **443** | v4.4.0-M3 | 局域网 P2P 双机直接发现与对撞增量合并协议 | `COMPLETED` | [stage-440-desktop.md](stage-440-desktop.md) |
| **450** | v4.3.2 | 灵感想法舱 (`IdeaRecord`) 与智能剪藏知识库 (`ClippingRecord`) 契约与独立舱室 | `COMPLETED` | [stage-450-idea-clipping-vault.md](stage-450-idea-clipping-vault.md) |
| **451** | v4.3.2 | 极速智能采集通道与截图无感监听器 (`ScreenshotWatcherHelper` + OCR) | `COMPLETED` | [stage-451-screenshot-watcher-clipper.md](stage-451-screenshot-watcher-clipper.md) |
| **452** | v4.3.2 | 跨维双向锚定 (`CrossLinkManager`) 与实物/想法/剪藏全景知识图谱 | `COMPLETED` | [stage-452-cross-dimensional-linking.md](stage-452-cross-dimensional-linking.md) |
| **453** | v4.3.3 | 空间全景收纳箱 AR 虚拟透视视窗 (`BoxArInspectionDialog`) 与 X 光层级透视 | `COMPLETED` | [release_notes_v4.3.3.md](../../release_notes_v4.3.3.md) |
| **454** | v4.3.3 | 端侧离线自然语言意图搜索与口语化联想引擎 (`NaturalQueryHelper`) | `COMPLETED` | [release_notes_v4.3.3.md](../../release_notes_v4.3.3.md) |
| **455** | v4.3.3 | NFC / RFID 智能标签一碰出入库与时效打卡助手 (`NfcVaultTouchHelper`) | `COMPLETED` | [release_notes_v4.3.3.md](../../release_notes_v4.3.3.md) |
| **460** | v4.4.0 | **端侧轻量离线视觉识物与品类属性自动预测** (MobileVision / TensorFlow Lite 纯离线) | `COMPLETED` | [stage-460-spatial-perception.md](stage-460-spatial-perception.md) |
| **461** | v4.4.1 | **空间 3D 实景点云与房间 Mesh 网格导入** (基于通用 GLTF/OBJ 格式三维空间收纳定点) | `COMPLETED` | [stage-460-spatial-perception.md](stage-460-spatial-perception.md) |
| **462** | v4.4.2 | **蓝牙 BLE 智能寻物标签雷达测距与声光定位** (iBeacon / BLE Beacon 信号追踪) | `COMPLETED` | [stage-460-spatial-perception.md](stage-460-spatial-perception.md) |
| **463** | v4.4.3 | **动态空间热力图与家庭动线收纳效率精算** (动线热力图与物品收纳科学评分) | `COMPLETED` | [stage-460-spatial-perception.md](stage-460-spatial-perception.md) |
| **470** | v4.5.0 | **全品类二手残值动态衰减曲线模型** (数码/家电/奢侈品权威残值系数离线推演) | `COMPLETED` | [stage-470-financial-depreciation.md](stage-470-financial-depreciation.md) |
| **471** | v4.5.1 | **家庭重资产净值沉没成本与风险对冲模拟器** (耐用品与消费品现金流预测) | `COMPLETED` | [stage-470-financial-depreciation.md](stage-470-financial-depreciation.md) |
| **472** | v4.5.2 | **智能断舍离（KonMari）回血决策辅助系统** (闲置时长与日均成本推荐清单) | `COMPLETED` | [stage-470-financial-depreciation.md](stage-470-financial-depreciation.md) |
| **473** | v4.5.3 | **商业保险与契约理赔一键证据链打包引擎** (防灾失窃理赔专用 PDF 审计导出) | `COMPLETED` | [stage-470-financial-depreciation.md](stage-470-financial-depreciation.md) |
| **480** | v4.6.0 | **端侧轻量小模型离线向量嵌入与智能问答 (RAG)** (纯离线多跳自然语言检索) | `COMPLETED` | [stage-480-on-device-rag-ai.md](stage-480-on-device-rag-ai.md) |
| **481** | v4.6.1 | **物品说明书与保修卡智能解析问答** (PDF/图片说明书故障排查与保养周期抽取) | `COMPLETED` | [stage-480-on-device-rag-ai.md](stage-480-on-device-rag-ai.md) |
| **482** | v4.6.2 | **灵感闪念与文章剪藏自组织主题网络演化** (语义相似度思维知识网自生长) | `COMPLETED` | [stage-480-on-device-rag-ai.md](stage-480-on-device-rag-ai.md) |
| **483** | v4.6.3 | **智能出行场景装备动态装配引擎** (根据目的地天气与天数自动生成装箱核对表) | `COMPLETED` | [stage-480-on-device-rag-ai.md](stage-480-on-device-rag-ai.md) |
| **490** | v4.7.0 | **全家庭端到端加密（E2EE）去中心化群组对撞协议** (X25519 零知识加密信封) | `COMPLETED` | [stage-490-decentralized-privacy.md](stage-490-decentralized-privacy.md) |
| **491** | v4.7.1 | **极简私有云 Web 节点与 NAS Docker 单容器镜像** (群晖/威联通一键部署) | `COMPLETED` | [stage-490-decentralized-privacy.md](stage-490-decentralized-privacy.md) |
| **492** | v4.7.2 | **桌面端 Linux / Wayland 原生适配与 Flatpak / AppImage 分发** | `COMPLETED` | [stage-490-decentralized-privacy.md](stage-490-decentralized-privacy.md) |
| **493** | v4.7.3 | **跨端即时剪贴板与大文件点对点局域网秒传通道 (P2P Direct Pipe)** | `COMPLETED` | [stage-490-decentralized-privacy.md](stage-490-decentralized-privacy.md) |
| **500** | **v5.0.0** | **终极里程碑：Collecter 5.0 — 全维空间、物理与数字认知全息资产操作系统** | `COMPLETED` | [stage-500-universal-asset-os.md](stage-500-universal-asset-os.md) |
| **510** | v5.1.0 | **Android ARCore 空间全息锚定与虚拟箱柜投射** (AR Spatial Anchoring 浮空标签) | `COMPLETED` | [stage-510-spatial-computing.md](stage-510-spatial-computing.md) |
| **511** | v5.1.1 | **多房间空间拓扑层级与 3D 楼层穿梭引擎** (多楼层 3D 空间穿梭与物资路径导航) | `COMPLETED` | [stage-510-spatial-computing.md](stage-510-spatial-computing.md) |
| **512** | v5.1.2 | **超宽带 UWB 厘米级空间精准测距寻物** (结合 UWB 芯片实现 3D 空间箭头寻物指引) | `COMPLETED` | [stage-510-spatial-computing.md](stage-510-spatial-computing.md) |
| **513** | v5.1.3 | **智能空间容积率与收纳箱装载率 3D 科学推演** (3D Bin Packing 装箱容积推演) | `COMPLETED` | [stage-510-spatial-computing.md](stage-510-spatial-computing.md) |
| **520** | v5.2.0 | **Matter / Home Assistant 局域网协议本地直连** (局域网免外网直连智能传感器) | `COMPLETED` | [stage-520-smart-iot-sensing.md](stage-520-smart-iot-sensing.md) |
| **521** | v5.2.1 | **环境温湿度与光照资产劣化主动预警系统** (茶酒名画相机镜头防霉避光预警) | `COMPLETED` | [stage-520-smart-iot-sensing.md](stage-520-smart-iot-sensing.md) |
| **522** | v5.2.2 | **智能电子墨水屏（E-Ink）标签无线离线刷屏** (NFC/BLE 为收纳盒墨水屏无线刷图) | `COMPLETED` | [stage-520-smart-iot-sensing.md](stage-520-smart-iot-sensing.md) |
| **523** | v5.2.3 | **智能耗材用量流速预测与自动补货提醒** (根据家庭消耗速率动态推演耗尽日期) | `COMPLETED` | [stage-520-smart-iot-sensing.md](stage-520-smart-iot-sensing.md) |
| **530** | v5.3.0 | **物品碳足迹与全生命周期环境影响精算** (评估资产制造与使用碳排放) | `COMPLETED` | [stage-530-circular-economics.md](stage-530-circular-economics.md) |
| **531** | v5.3.1 | **社区/熟人去中心化闲置物资免信任互换网络** (基于密码学的熟人圈借物账本) | `COMPLETED` | [stage-530-circular-economics.md](stage-530-circular-economics.md) |
| **532** | v5.3.2 | **二手交易平台（闲鱼/转转）一键生成标准化商品卡片** (自动打包成色发票长图) | `COMPLETED` | [stage-530-circular-economics.md](stage-530-circular-economics.md) |
| **533** | v5.3.3 | **电子产品回收价值对冲与官方 Trade-in 行情比价** (以旧换新最佳窗口期推演) | `COMPLETED` | [stage-530-circular-economics.md](stage-530-circular-economics.md) |
| **540** | v5.4.0 | **端侧大语言模型（On-Device Small LLM / Gemma）资产管家** (纯离线小模型资产规划) | `COMPLETED` | [stage-540-autonomous-agent.md](stage-540-autonomous-agent.md) |
| **541** | v5.4.1 | **跨应用智能账单与售后契约自动归档** (Android 通知 Hook 智能预录入) | `COMPLETED` | [stage-540-autonomous-agent.md](stage-540-autonomous-agent.md) |
| **542** | v5.4.2 | **全息资产动态健康评分与家庭风险全景雷达** (综合折旧、保修与防灾体检报告) | `COMPLETED` | [stage-540-autonomous-agent.md](stage-540-autonomous-agent.md) |
| **543** | v5.4.3 | **多语言国际化（i18n）与多币种实时汇率离线换算** (外币资产混合记账与汇率快照) | `COMPLETED` | [stage-540-autonomous-agent.md](stage-540-autonomous-agent.md) |
| **550** | **v5.5.0** | **究极终章：Collecter 5.5 — 全知空间、物质物理与自主认知全息元操作系统** | `COMPLETED` | [stage-550-ultimate-nexus.md](stage-550-ultimate-nexus.md) |
| **560** | v5.6.0 | **空间声学指纹与敲击回声室内定位** (麦克风/扬声器脉冲回声识别房间与柜体) | `COMPLETED` | [stage-560-spatial-acoustic.md](stage-560-spatial-acoustic.md) |
| **561** | v5.6.1 | **Wi-Fi CSI / 蓝牙信道探测（Channel Sounding）高精测距** (穿墙级空间位移防盗) | `COMPLETED` | [stage-560-spatial-acoustic.md](stage-560-spatial-acoustic.md) |
| **562** | v5.6.2 | **空间环境磁场异常指纹寻物** (地磁传感器记录房间特有磁场畸变指纹) | `COMPLETED` | [stage-560-spatial-acoustic.md](stage-560-spatial-acoustic.md) |
| **563** | v5.6.3 | **收纳箱震动倾角监测与破损跌落报警** (易碎品运输倾角与冲击加速度监测) | `COMPLETED` | [stage-560-spatial-acoustic.md](stage-560-spatial-acoustic.md) |
| **570** | v5.7.0 | **高分子聚合物与橡胶水解老化动力学模型** (鞋底水解/压胶脱落老化速率算法) | `COMPLETED` | [stage-570-material-aging.md](stage-570-material-aging.md) |
| **571** | v5.7.1 | **名贵木材、皮革与古籍纸张温湿度滞后回线精算** (平衡含水率 EMC 自动保养提醒) | `COMPLETED` | [stage-570-material-aging.md](stage-570-material-aging.md) |
| **572** | v5.7.2 | **锂电池自放电与健康度（SoH）日历寿命推演** (闲置电池自放电与防饿死提醒) | `COMPLETED` | [stage-570-material-aging.md](stage-570-material-aging.md) |
| **573** | v5.7.3 | **生鲜果蔬乙烯释放与跨品类催熟阻隔算法** (高乙烯释放物科学防烂防串味) | `COMPLETED` | [stage-570-material-aging.md](stage-570-material-aging.md) |
| **580** | v5.8.0 | **家庭局域网多机去中心化 Paxos/Raft 共识对撞** (零服务器无主自选举强一致性) | `COMPLETED` | [stage-580-decentralized-consensus.md](stage-580-decentralized-consensus.md) |
| **581** | v5.8.1 | **Wi-Fi Direct / BLE Mesh 自组网野外无网资产联络** (极端断网全队物资同步) | `COMPLETED` | [stage-580-decentralized-consensus.md](stage-580-decentralized-consensus.md) |
| **582** | v5.8.2 | **离线密钥主权与分层确定性（BIP-39）资产助记词备份** (12/24 助记词纸质冷备份) | `COMPLETED` | [stage-580-decentralized-consensus.md](stage-580-decentralized-consensus.md) |
| **583** | v5.8.3 | **零知识证明（ZK-SNARKs）资产凭据脱敏验证** (零泄露证明拥有 X 价值资产) | `COMPLETED` | [stage-580-decentralized-consensus.md](stage-580-decentralized-consensus.md) |
| **590** | v5.9.0 | **端侧物理世界常识因果推理引擎** (物品物理依赖关系与联动准备提示) | `COMPLETED` | [stage-590-embodied-cognition.md](stage-590-embodied-cognition.md) |
| **591** | v5.9.1 | **家庭资产流转动线因果图与失踪溯源回溯器** (拓扑逆向回溯失踪物品可能位置) | `COMPLETED` | [stage-590-embodied-cognition.md](stage-590-embodied-cognition.md) |
| **592** | v5.9.2 | **自适应极简与极繁收纳人格分析与空间动力学** (分析收纳偏好自适应排版) | `COMPLETED` | [stage-590-embodied-cognition.md](stage-590-embodied-cognition.md) |
| **593** | v5.9.3 | **家庭遗产与世代传承资产契约数字信托** (长达数十年的密码学继承解锁) | `COMPLETED` | [stage-590-embodied-cognition.md](stage-590-embodied-cognition.md) |
| **600** | **v6.0.0** | **终极里程碑：Collecter 6.0 — 全息物理空间、物质全生命周期材料动力学操作系统** | `COMPLETED` | [stage-600-universal-physical-os.md](stage-600-universal-physical-os.md) |
| **610** | v6.1.0 | **Android Vulkan / GPU 加速光场体素渲染** (60fps 实时渲染 10,000+ 资产三维体素立方体) | `PLANNED` | - |
| **620** | v6.2.0 | **多光谱与红外热成像资产热损耗感知** (红外监测电器发热异常与保温绝热失效) | `PLANNED` | - |
| **630** | v6.3.0 | **微距光学指纹与防伪材质微观比对** (高频 LBP 算法提取名表珠宝微观纹理指纹) | `PLANNED` | - |
| **640** | v6.4.0 | **空间环境微气压与海拔高度差寻物** (气压计厘米级感知收纳架垂直层级) | `PLANNED` | - |
| **650** | v6.5.0 | **极端防灾战备生命维持物资配给算法** (灾难场景卡路里与洁净水动态配给) | `PLANNED` | - |
| **660** | v6.6.0 | **医用冷链生物制品与疫苗温区时间积分** (Arrhenius 动力学累积变质损伤计算) | `PLANNED` | - |
| **670** | v6.7.0 | **高能辐射与紫外光累积辐照老化预警** (日光辐照模型推演帐篷塑料车漆抗 UV 寿命) | `PLANNED` | - |
| **680** | v6.8.0 | **真空密封袋与充氮防氧化气体阻隔衰减** (雪茄普洱纪念币透氧率 OTR 与脱氧提醒) | `PLANNED` | - |
| **690** | v6.9.0 | **LoRa / 业余无线电超远距离无网资产广播** (15公里超视距无网资产位置与求救广播) | `PLANNED` | - |
| **700** | **v7.0.0** | **Collecter 7.0 — 分布式去中心化抗毁联邦资产网络** (全球离线去中心化主权联邦) | `PLANNED` | - |
| **710** | v7.1.0 | **端对端零知识环签名秘密资产托管** (匿名环签名投票共管大额重资产) | `PLANNED` | - |
| **720** | v7.2.0 | **物理防篡改不可克隆函数（PUF）硬件绑定** (KeyStore TEE 硅芯片物理不可克隆根密钥) | `PLANNED` | - |
| **730** | v7.3.0 | **通用机器人抓取姿态与碰撞体积拓扑接口** (标准化物品 3D 抓取接触面与重心参数) | `PLANNED` | - |
| **740** | v7.4.0 | **自动化物品归位路径规划与动作求解** (机械臂 6 自由度无碰撞轨迹规划) | `PLANNED` | - |
| **750** | v7.5.0 | **空间物理碰撞干涉与受力形变模拟** (刚体接触与重物挤压形变应力分析) | `PLANNED` | - |
| **760** | v7.6.0 | **视觉-语言-动作（VLA）多模态意图编排** (自然口语指令自动转换为实体搬运序列) | `PLANNED` | - |
| **770** | v7.7.0 | **跨世代家族物理文明编年史与口述历史** (资产与三代人录音照片结合的交互式纪录片) | `PLANNED` | - |
| **780** | v7.8.0 | **全息物理空间抗量子密码学（PQC）硬化** (NIST Kyber / Dilithium 算法升级抗量子攻击) | `PLANNED` | - |
| **790** | v7.9.0 | **全球离线物质代码（Open Physical Asset Spec）** (开放无版权离线数据交换协议标准) | `PLANNED` | - |
| **800** | **v8.0.0** | **终极宇宙终章：Collecter 8.0 — 全知全能空间全息、物质材料动力学、具身机器人操纵与家族世代文明元资产操作系统** | `PLANNED` | - |
