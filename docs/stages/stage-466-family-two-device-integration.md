# Stage 466：家庭协作双端联调

日期：2026-09-04。

## 目标

以桌面端家庭服务作为设备 A、Android 运行环境作为设备 B，真实经过 TCP 接口验证家庭成员的查看、编辑、共享隔离和撤销行为。

## 边界

- 联调数据仅写入 `desktop/build/family-interop/`，不得读写正式桌面数据。
- 成员密钥只写入构建目录中的临时会话文件，夹具退出后撤销并删除。
- Android 测试从 instrumentation 参数读取地址和密钥，不写入备份或持久配置。
- 本机没有实体 Android 设备时允许使用模拟器完成双端进程联调，但不得称为两台物理设备验收。

## 完成标准

- [x] AC-01：Android 端使用查看者密钥只读到明确共享且非敏感的记录。
- [x] AC-02：查看者写入被拒绝，编辑者可修改共享记录的位置。
- [x] AC-03：编辑者不能修改未共享记录，也不能解除敏感标记。
- [x] AC-04：成员撤销后再次访问被拒绝。
- [x] AC-05：桌面和 Android 测试、Release 构建及 selfcheck 通过。

## 验收证据

- 设备 A：隔离桌面 JVM 进程，`EmbeddedWebServer(allowLan=true)` 监听 8848。
- 设备 B：Android API 34 模拟器，通过 `http://10.0.2.2:8848/api/v1/family` 发起真实 TCP 请求。
- 活跃凭据阶段：[evidence-466-android-active.txt](evidence-466-android-active.txt)，1 个设备测试通过。
- 撤销凭据阶段：[evidence-466-android-revoked.txt](evidence-466-android-revoked.txt)，1 个设备测试通过并确认 HTTP 401。
- 桌面夹具正常退出并递归删除临时数据及会话密钥；正式数据目录未使用。
- 自动化回归：Android 80 个 JVM 单测、桌面测试、签名 Release 构建和 selfcheck 6/6 通过。
- 限制：`adb devices -l` 未发现实体手机，因此本次是两个真实进程之间的桌面端—Android 模拟器联调，不是两台物理手机验收。
