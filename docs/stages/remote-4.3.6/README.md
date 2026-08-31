> 远端 v4.3.6 历史文档快照；其中完成声明不代表本次验收结果。原路径：`docs/README.md`。

# Collecter 文档总目录与阅读指南 (Documentation Index)

欢迎查阅 **Collecter (资产与收纳管家)** 的全景文档体系。
本仓库文档已全面对齐 `capital-agent-system` 的工程治理与架构规范体系。

---

## 🧭 快速导航

```
docs/
├── README.md                           # 本文件：文档总目录与阅读指南
├── codex-skills.md                     # Agent 长期工程规范与门禁
├── stage-current.md                    # 当前 Stage 快照与上下文
├── TODO.md                             # 待办执行账本 (优先级/责任/下一动作)
├── BLOCKERS.md                         # 外部阻塞项清单
│
├── design/                             # 架构与系统设计
│   ├── 01-product-requirements.md      # 业务需求与第一性原理收纳体系
│   ├── 02-system-architecture.md       # 系统总体架构设计
│   ├── 03-data-models-design.md        # 数据模型与仓储设计
│   ├── 04-security-compliance-design.md# 隐私安全与合规设计
│   └── 05-desktop-standalone-architecture.md # 桌面单机版架构立项设计
│
├── development/                        # 开发与测试规程
│   ├── 11-development-guide.md         # 核心开发与编码规范
│   ├── 12-test-plan.md                 # 测试计划与门禁
│   └── 33-spec-driven-development.md   # Spec 驱动开发指南
│
├── stages/                             # 阶段演化与交付报告
│   ├── stage-roadmap.md                # 阶段路线图
│   ├── stage-431.md                    # Stage 431 (v4.3.1) 交付报告
│   └── stage-440-desktop.md            # Stage 440 (v4.4.0) 桌面端立项 Spec
│
├── manuals/                            # 用户手册与全景教程
│   ├── 20-user-guide.md                # 完整功能用户使用手册
│   └── 25-tutorial-dialog-guide.md     # 图文教程与跟手演练指引
│
├── bug/                                # 缺陷追踪账本
│   └── bugList.md                      # 缺陷列表 (现象/根因/修复/验证)
│
└── aq/                                 # 架构问答账本
    └── aq.md                           # 架构问答与决策背景
```

---

## 📖 读者指引

- **Coding Agent (AI 研发助手)**：
  - 启动第一步：读取 [`../AGENTS.md`](../AGENTS.md) 或 [`../GEMINI.md`](../GEMINI.md)；
  - 确定当前任务：读取 [`stage-current.md`](stage-current.md) 与 [`TODO.md`](TODO.md)；
  - 遵循工程约束：读取 [`codex-skills.md`](codex-skills.md)；
  - 交付前验证：执行 `pwsh .\tools\selfcheck.ps1`。
- **开发者 / 架构师**：
  - 了解系统设计：阅读 [`design/02-system-architecture.md`](design/02-system-architecture.md)；
  - 桌面单机版立项：阅读 [`design/05-desktop-standalone-architecture.md`](design/05-desktop-standalone-architecture.md)；
  - 编码规范：阅读 [`development/11-development-guide.md`](development/11-development-guide.md)。
- **终端用户 / 产品维护**：
  - 查阅所有功能：阅读 [`manuals/20-user-guide.md`](manuals/20-user-guide.md)。
