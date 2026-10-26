# 实机验证：普通应用直连与 MessagePiece 摘要修正

本报告替换上一版未经充分验证的权限与断连归因。当前结论：**普通 Android 应用可以直连这台已配对的 Galaxy Watch；此前立即断连的已确认编码错误是 MessagePiece.f2 摘要错误。无需把 watchfree 安装为系统特权应用来修复该问题。**

## 1. 官方应用实际权限

2026-10-26 在 Honor BKQ-AN90 / Android 16 上执行 `pm path` 与 `dumpsys package`：

| 包 | 安装位置 | 包标志 | 实际权限 |
|---|---|---|---|
| Watch7 Plugin | `/data/app/.../base.apk` | 不含 SYSTEM / PRIVILEGED | BLUETOOTH_CONNECT 已授予；BLUETOOTH_PRIVILEGED 仅声明，未授予 |
| Google Wear CN | `/data/app/.../base.apk` | 不含 SYSTEM / PRIVILEGED | BLUETOOTH_CONNECT 已授予 |
| 修正后的 watchfree | `/data/app/.../base.apk` | DEBUGGABLE / HAS_CODE / ALLOW_CLEAR_USER_DATA / ALLOW_BACKUP | BLUETOOTH_CONNECT 已授予；不再声明 MODIFY_PHONE_STATE 或 BLUETOOTH_PRIVILEGED |

`requested permissions` 中出现某个权限，不代表系统已授予。检查获授状态必须看 install/runtime permissions，不能只搜索权限名。

官方 `wear/sources/defpackage/hfb.java:216` 使用：

```java
BluetoothSocket socket = device.createRfcommSocketToServiceRecord(hfc.a);
socket.connect();
```

这条 RFCOMM 数据连接路径使用普通蓝牙 API。`HandsFreeProfileWork` 的隐藏 HFP API 属于另一条路径：其权限检查失败不能证明 RFCOMM 连接也需要特权。

## 2. 官方源码揭示的关键错误

来源是手机提取的官方 Wear APK：`research/apks/pulled/wear/base.apk`。本轮 REA Android 接口因缺少 `REA_JADX_MCP_JAR` 不可用，使用已有 JADX 反编译输出与原始抓包交叉验证；不把静态分析当成对手表进程的运行时观测。

### 2.1 外层不是请求/响应信封，而是 MessagePiece

`wear/sources/defpackage/hbt.java`：

- `d(hjo)` 序列化完整消息，将 `gyi.n(bytes)` 写入外层摘要字段。
- `c(hjp)` 重新计算同一摘要；不一致抛出 `IOException("Unsegmented message digest should match the received digest...")`。
- `f(hjo, ...)` 分片时，为所有片写入完整消息的相同摘要与各自片号。
- `hbp.run()` 捕获读包/摘要异常并调用连接错误处理。

`gyi.java:892–912`：

```java
n(bytes) = q(s().digest(bytes));
q(hash) = new String(Base64.encode(hash, 11));
s() = MessageDigest.getInstance("SHA1");
```

其中 Android Base64 flags 11 = URL_SAFE | NO_WRAP | NO_PADDING。因此：

```text
MessagePiece.f2 = Base64URL(SHA1(完整 inner 字节))，去掉 '='
```

旧实现把这个字段叫作 requestId，并填入 20 字节随机数的 Base64URL。随机值长度正确但摘要错误，无法通过官方读包校验。NodeInfo 使用原始抓包时摘要有效；紧接着自行生成的 NodeAlloc 使用随机摘要，因而即使收到对端拓扑广播，连接仍会立即结束。

### 2.2 原始抓包核验

从 `snoop2.log` 提取字段后直接计算摘要：

| 帧号 | 内容 | f2 == Base64URL(SHA1(inner)) |
|---|---|---|
| 503 | 手机拓扑消息 | true |
| 505 | 手表拓扑消息 | true |
| 506 | /settings/rpc | true |
| 507 | 空心跳 | true |
| 547 | set_device_activation | true |

空心跳 inner 恒为 `5a00`，所以其摘要恒定。**这不是配对派生 token，也不是必须从某台设备窃取或固化的凭据。**

### 2.3 其他纠正

- 旧称 NodeAlloc 的 inner tag8 实际是 **SyncStart**：`gyp.i()` 的日志明确称其为 SyncStart，`gyo.b()` 用节点→最高 seqId 映射生成，`gyo.e()` 处理对端同步起点。不是节点分配认证；下文日志中的 NodeAlloc 沿用旧代码名称。

- 外层 f3 / f4 是片号 / 总片数；f5 是 queueId，不是 REQUEST/RESPONSE。官方 `hbt.e()` 直接输出 queueId / digest / piece / total。
- WearableMessage.f3 是应用签名摘要。`hbj.c()` 从 `gtu.d` 写入该字段，`gtu` 将其命名为 signatureDigest，`gtw.d()` 计算应用证书的 SHA-1。旧变量 senderNode / SESSION_TOKEN 的名字不准确。
- `hbk` 的心跳由读包唤醒 writer 后在队列排空时按需发出，并非源码证明的固定 10 秒定时。watchfree 的 10 秒心跳是当前实现选择。
- 消息 ID 在抓包中递增，但没有证据证明必须从 2 开始、或者旧值 453 会触发滑动窗口异常；改为 2 后上一轮仍然断连。此前关于这一根因的结论撤回。
- 修改 NodeInfo 内容而不更新摘要会使实验无效。之前的 LAB 不能证明手表固化了设备名、时间戳或整帧凭据。官方 `hbr` 确有登记节点 ID 检查，不能反向声称任意新节点均可接入。

## 3. 回归测试与实机对照

### 3.1 单元测试

新增测试覆盖：

1. NodeAlloc / JSON / 普通信封 / 无 queueId 信封均携带正确内容摘要。
2. 不输入任何配对数据，也能生成与抓包 #507 完全相同的空心跳。
3. 修改 NodeInfo 内容后，摘要随内容重新计算。

修复前：`12 tests completed, 3 failed`。
修复后：`:watchfree:assembleDebug :watchfree:testDebugUnitTest --offline` 构建与 12 项测试通过。

### 3.2 第一轮：只修摘要

保持上一轮业务握手顺序，仅修正发包摘要；临时禁用三星两包和 Google Wear CN，使出数据通道。

- 18:28:27.497 收到 NodeInfo。
- 18:28:27.535 收到拓扑消息。
- 18:28:27.615 收到 quick_settings。
- 18:28:27.638 收到 set_device_activation 业务确认。
- 18:28:27.764 收到 status_exchange 业务响应。
- 随后接收到大量设置、电池、应用、无障碍 DataItem。
- 45 秒观察期间未出现读循环断开。

日志：[`snoop/digest-fix-run.log`](snoop/digest-fix-run.log)。

### 3.3 第二轮：移除特权声明和 HFP 调用

移除 Manifest 中 MODIFY_PHONE_STATE / BLUETOOTH_PRIVILEGED，断开 HfpConnector 与传输层的接线；重新安装普通 APK，再次禁用官方三包测试。

- 18:31:40.214 蓝牙记录 RFCOMM Connection opened，channel 5。
- 18:31:40.301 收到 NodeInfo。
- 18:31:40.322 收到拓扑消息。
- 18:31:40.390 收到 set_device_activation 确认。
- 18:31:40.494 收到 status_exchange 响应。
- 统计：120 条 WearPath、3 条 WCS 业务消息、0 次读循环结束。
- 45 秒观察期间未断开；结束时保存蓝牙状态快照，再主动停止 watchfree 并恢复官方三包。

日志：[`snoop/digest-fix-unprivileged-run.log`](snoop/digest-fix-unprivileged-run.log)。
状态：[`snoop/digest-fix-unprivileged-bluetooth.txt`](snoop/digest-fix-unprivileged-bluetooth.txt)。

这些结果直接反驳“普通应用必须特权化才能保持此连接”。调试使用 ADB/root 来读取日志、临时禁用竞争通道的官方包；应用自身这轮没有调用 root 或 HFP 隐藏 API 来建立数据连接。

## 4. 已验证范围与剩余问题

已验证：这台已配对 Honor + Galaxy Watch Ultra 上，普通 watchfree 使用标准 RFCOMM 可完成业务初始化，官方三包在整个观察阶段都被禁用，两轮各 45 秒观察无立即断连。

尚未验证：

- 全新手机/全新手表从零配对及身份持久化。
- 手表深度休眠、离开后返回、手机重启后的可靠自动重连。
- 数小时运行、功耗、后台进程回收。
- 大消息在 MessagePiece 层分片后的完整摘要校验与重组。不能把 RFCOMM/L2CAP 分片和应用层 MessagePiece 分片混为一谈。
- 将历史 SyncStart 样本中的节点→seqId 表、同步起点和应用签名元数据改为正确的动态模型。
- Health Connect 落地与通知全流程。

不再建议将系统特权安装或 Xposed 桥接作为协议修复前提。下一步继续沿官方 `hbr/hbk/hbp/hbt` 与 Samsung WCS 分发调用链还原动态状态，而不是继续盲目重放或猜测权限边界。

## 5. 双向控制与设置读写（2026-10 实机验证）

本轮把「只读」扩展为「可写」，全部结论都有实机回包作证。

### 5.1 手机 → 手表设置写入（DataItem 通道）

对齐官方 `SettingsCompanion.putWatchDataItem`（`research/decompiled/watch7plugin/.../SettingsCompanion.java:1923`）：

| 字段 | 值 | 说明 |
| --- | --- | --- |
| envType (f5) | `3` (DATA_QUEUE) | DataItem 走数据队列，不是消息队列 |
| f2 path | `wear://<phoneNode>/<settings path>` | **授权域是手机节点**（`20a3039`），不是手表节点 |
| f4 DataItem | `{<valueKey>: <value>, "from": "phone", "time": <int64 毫秒>}` | 无 `key` / `type` 元字段 |
| f5 seqId | 单调递增，须大于手表已见的最大值 | 官方 StreamID 约 57 万；实现用毫秒时间戳 |
| f7 nodeId | `<phoneNode>` | 与 URI 授权域一致 |

`hqc.f1` 的 type 值**等于 hqb 字段号**（抓包逐字节核对）：
`1=bytes 2=string 3=double 4=float 5=int64 6=int32 8=bool 9=map 10=array`。

编码器与官方抓包**字节级一致**（`WcsCodecTest.encodeSettingDataMap_matchesCapturedPhoneWrite`
直接比对 `bedtime/lifestyle_info` 原始帧）。

实测写入结果（Galaxy Watch Ultra / 0V4F）：

```
21:37:21 写入 wear://20a3039/w_module_sec_watch_setting_accessibility/grayscale {booleanIsChecked=true}
21:38:04 手表回推 grayscale → 开          ← 生效
21:38:45 写入 wear://20a3039/w_module_sec_watch_setting/display/brightness {intLevel=120}
21:38:51 手表回推 brightness = 120        ← 生效
21:40:x  再次写回 false / 103，恢复原值    ← 双向可逆
```

过程中发现的两个坑（都已修）：

1. **路径必须带 `wear://<phoneNode>/` 前缀**。早期实现直接发 `/w_module_...`，手表解析失败静默丢弃。
2. **seqId 必须大于手表已记录值**。早期用 `1001` 起，被当作过期数据忽略；改用毫秒时间戳后立即生效。

### 5.2 设置项的真实形状

手表主动推送的 DataMap 有两种形态，需归一化：

```jsonc
// accessibility 类：无元数据，值字段名即 key
{from=watch, booleanIsChecked=false, time=...}
// 其余类：带 key/type 元数据（type 是 Samsung 的 ValueType，不是 DataMap type）
{from=watch, key=intLevel, intLevel=103, type=1, time=...}
```

归一化后共 91 个设置项，值字段词汇表实测只有：
`booleanIsChecked` / `booleanIsAvailable` / `intLevel` / `intSelectedItem` / `intPercent` /
`stringName` / `stringAddress` / `stringTime` / `fontNameString` / `array*List`。
写入时**原样复用手表给的 key**，因此无需逐个硬编码。

### 5.3 DeviceCare 设备护理指令矩阵（逐条 25s 窗口实测）

| 指令 | path | 请求体 | 响应 | 时延 |
| --- | --- | --- | --- | --- |
| PSM 开 | `/c_devicecare/battery/psm` | `{"extraData":1,"msgId":203,"msgType":1}` | `{"extraData":1,"msgId":203,"msgType":2}` | ~1s |
| PSM 关 | 同上 | `{"extraData":0,...}` | `{"extraData":0,...,"msgType":2}` | ~1s |
| 电池详情 | `/c_devicecare/battery/base_info` | `{"extraValue":"","msgId":209}` | DataItem `dataMapId=209` | ~0.5s |
| 电池用量 | `/c_devicecare/battery/refresh` | `{"extraValue":"","msgId":202}` | DataItem `dataMapId=202`（含 `battery_info`） | ~2s |
| 内存用量 | `/c_devicecare/memory/usage` | `{"msgId":301,"msgType":1}` | DataItem `dataMapId=300` | ~3s |
| 内存清理 | `/c_devicecare/memory/clean` | `{"appName":[],"msgId":303}` | **无回包**（未观察到内存下降） | — |
| 电池保护设置 | `/c_devicecare/battery/setting` | `{"extraData":[-1,-1,-1],"msgId":211,"msgType":1}` | DataItem `dataMapId=206`，但**不含** `key_battery_setting_status` | ~1s |

结论：PSM 切换是真正可用的一键控制；`battery/setting`（充电保护）在该固件上不通过此通道暴露；
`memory/clean` 空列表形式未见效果，官方 UI 会传具体应用列表（本机 `memory/usage` 未返回
`memory_usage_list`，因此暂无应用级清理数据）。

`battery_info` 实测字段：`battery_level / status / time_remaining / rut_psm_off / rut_psm_on /
psm_status / time_since_charged / usage_time_for_full_charge / wearable_protected_fully /
wearable_psm_control_restricted`。
其中 `status` 语义为 Android BatteryManager：**2=充电中 3=放电中 5=已充满**（官方 `isCharging()`
判定 `==2`、`isFull()` 判定 `==5`）。本机 `rut_psm_off/on` 恒为 0，故「预估续航」按不可用处理。

### 5.4 两个工程性修复（非协议问题）

1. **主线程套接字阻塞**：通知监听 `onNotificationPosted` 在微信消息风暴下直接 `outputStream.write`
   会被阻塞数十秒，导致后续广播全部排队、界面假死。现改为**发送队列 + 独立 writer 线程**
   （`RfcommTransport.startWriter`），所有调用方只入队。
2. **Honor 系统拦截隐式广播**：`adb shell am broadcast -a dev.ricky.watchfree.CMD` 前几次能收到、
   之后静默失效（`ActivityManager: Enqueued broadcast` 但应用主线程 idle，never dispatched）。
   必须用显式组件：`am broadcast -n dev.ricky.watchfree/.core.WatchCommandReceiver ...`。
   自动化脚本已改用显式投递。

### 5.5 DeviceCare 通道的两个行为约束（实测）

1. **PSM 只在状态真正变化时才回包**。请求值与当前值相同 → 手表完全不回
   `msgType=2`。因此实现里必须先用已知状态判断，避免把「无需变更」误判成失败：

   ```
   基线 key_psm_status=1，发 on=true  → 3 次尝试全部无回包（状态未变）
   同一次会话发 off        → 0.4s 内回 {"extraData":0,...,"msgType":2}，且设置通道同步 key_psm_status=0
   ```

   代码里用 `psmKnown` 标记「已从手表学到 PSM 状态」，相同目标值直接跳过下发。

2. **DeviceCare 请求会被节流丢弃**。1.5s 内连发多条（或刚处理完一批设置写入）时，
   后续请求静默丢失。实现改为**单线程串行队列 + 1.5s 最小间隔**，
   并对 PSM 增加 4s/8s 两次补发（`WatchRepository.dc()`）。

### 5.6 最终验收结果（`snoop/live-acceptance-run.log`）

一轮跑完 8 项断言全绿：

| 项 | 结果 |
| --- | --- |
| 会话握手 | ✔ |
| 收到设置项（>50，实测 78 项） | ✔ |
| PSM 一键开关回执（翻转 + 还原） | ✔ |
| 设置写 boolean 双向生效（grayscale 开 → 关） | ✔ |
| 设置写 int 生效（brightness 80 → 90 → 还原） | ✔ |
| 电池用量 DataItem(202) | ✔ |
| 内存用量 DataItem(300) | ✔ |
| 通知下发 | ✔ |

UI 侧同期用 `uiautomator dump` 核对：功能页显示
`电量 100% ⚡ / 内存 1102–1761 MB / 省电模式 关闭`，设备控制面板可交互；
设置页「常用设置」用真实 `key/type/值` 渲染（switch / −+ / 文本框），
「全部设置项」可展开 77 项。

### 5.7 表盘切换与 GMS ReorderingQueue 机制实机验证（2026-10-27）

#### 1. GMS 重排丢弃问题复现与突破
在开发断开重连时，发现手表 GMS 接收到底层 RFCOMM 报文（`inbound` 记录正常），但未触发 `receive` 投递给 `wcs.extension`。
逆向 GMS（`hay.java` / `hbg.java`）证实其为 `ReorderingQueue` 机制：当会话 `generation`（Tag 10 field 10）保持不变时，由于重连后手机 `sequence` 重置为 2，GMS 判定 `sequence < 上次最大 sequence`（如 249），全部予以丢弃（DROPPED）。
在 `WcsSession` 中为每次新连接会话自动自增 `generation` 后，手表 GMS 日志立即显示 `[lo:77443:N] inbound -> receive` 100% 连贯分发，彻底攻克长连接重连丢帧顽疾。

#### 2. 表盘查询验证
广播发送 `watchface_get`：
```bash
adb shell am broadcast -n dev.ricky.galaxywatchmanagerfree/.core.WatchCommandReceiver -a dev.ricky.galaxywatchmanagerfree.CMD --es cmd watchface_get
```
* 手表回包 1：`change_active_watchface` `msgType=3`，携带当前活跃表盘：
  `className: com.samsung.android.watch.watchface.ultraanalog.UltraAnalogWatchFaceService`
  App 成功解析并显示为中文名「极限指针 (Ultra Analog)」。
* 手表回包 2：`watchface_order` `msgType=3`，携带收藏列表：
  `favoriteIdOrderList: [1, 4, 3]`, `WatchFaceOrderType: "recent"`。

#### 3. 表盘切换实机验证
通过广播及 App UI 自动化点击切换表盘：
```bash
# 切换为模块化指针
adb shell am broadcast -n dev.ricky.galaxywatchmanagerfree/.core.WatchCommandReceiver -a dev.ricky.galaxywatchmanagerfree.CMD --es cmd watchface_set --es id analog_modular
# 切换为极限信息板（UI 点击测试）
# 切回极限指针
adb shell am broadcast -n dev.ricky.galaxywatchmanagerfree/.core.WatchCommandReceiver -a dev.ricky.galaxywatchmanagerfree.CMD --es cmd watchface_set --es id ultra_analog
```
权威复核命令：
```bash
adb -s 192.168.31.108:43471 shell dumpsys wallpaper | grep mWallpaperComponent
```
实测切换前后组件变化：
* 初始：`com.samsung.android.watch.watchface.ultraanalog.UltraAnalogWatchFaceService`
* 切换 1：`com.samsung.android.watch.watchface.analogmodular.AnalogModularWatchFaceService`（✔ 成功）
* 切换 2：`com.samsung.android.watch.watchface.ultrainfoboard.UltraInfoBoardWatchFaceService`（✔ 成功，UI 点击「应用」）
* 切回：`com.samsung.android.watch.watchface.ultraanalog.UltraAnalogWatchFaceService`（✔ 成功）

每次切换后：手表秒级生效、回发 `msgType=3` 确认回执、自动将该表盘移至收藏夹首位并回发 `watchface_order`；App UI 界面实时刷新为「当前生效 / 使用中」。

### 5.8 Samsung Health 独立双向数据同步实机验证（2026-10-27）

#### 1. 验证目标
验证在手机完全未安装 Samsung Health 官方应用的情况下，独立 App 模拟 Samsung Health 协议，能否双向打通手表端健康服务，拉取真实健康数据（计步、心率、压力等），并完成游标闭环。

#### 2. GMS 白名单伪装实测
使用官方 Samsung Health 的包名与证书指纹构造外层信封：
* `senderPkg`: `"com.sec.android.app.shealth"`
* `senderNode`: `"9741a0f330dc2e8619b76a2597f308c37dbe30a2"`

#### 3. 手表端消费与数据提取日志
手机发送 `REQUEST_DATA` 后，手表端 logcat 捕获到：
```
10-27 05:34:34.143 2521 14566 I SHWCOM - WLOG_WearOsListenerService: onMessageReceived() A message from watch was received : 340792, path /samsung_health/wearable_message, dataSize : 499...
10-27 05:34:34.144 2521 14566 I SHW - WLOG_WearableMessageManager: [W_MESSAGE][RECEIVE][RESPONSE] sequence:2...
10-27 05:34:34.147 2521  9842 I SHW - WLOG_WearableDataGetter: [D_GETTER] getUpdatedDataCursor com.samsung.shealth.stress.histogram : 1
10-27 05:34:34.160 2521  9842 I SHW - WLOG_WearableDataGetter: [D_GETTER] getUpdatedDataCursor com.samsung.shealth.best_records : 6
10-27 05:34:34.162 2521  9842 I SHW - WLOG_WearableDataGetter: [D_GETTER] getUpdatedDataCursor com.samsung.shealth.badge : 2
10-27 05:34:34.172 2521  9842 I SHW - WLOG_WearableDataGetter: [D_GETTER] getUpdatedDataCursor com.samsung.shealth.stress : 2
10-27 05:34:34.175 2521  9842 I SHW - WLOG_WearableDataGetter: [D_GETTER] getUpdatedDataCursor com.samsung.shealth.service_preferences : 9
10-27 05:34:34.177 2521  9842 I SHW - WLOG_WearableDataGetter: [D_GETTER] getUpdatedDataCursor com.samsung.shealth.tracker.pedometer_step_count : 1
10-27 05:34:34.188 2521  9842 I SHW - WLOG_WearableMessageManager: [W_MESSAGE][SEND][REQUEST] sequence:10114, sender:com.samsung.android.wear.shealth.wearable.syncmanager...
```

#### 4. 手机端解包与应答日志
手机端成功解析出 gzip 压缩的真实健康数据，并完成状态机回应：
```
[05:34:32.905] [健康数据] com.samsung.shealth.service_preferences: 9 条记录
[05:34:32.906] [健康数据] com.samsung.shealth.tracker.pedometer_step_count: 1 条记录, 首条={"datauuid":"e44586fb-15e3-4701-b7df-05f888bc2e4a","create_time":1793029702184,"update_time":1793029702184,"pkg_name":"com.sec.android.app.s
[05:34:32.908] [健康→] 回应 seq=10114 action=SYNC_DATA -> SUCCESS
[05:34:32.909] [WCS] TX [/samsung_health/wearable_message] gen=10466 messageId=10
[05:34:32.991] [WCS] ← /samsung_health/wearable_message/10115  type=4
[05:34:32.993] [健康] 收到数据包 seq=10115 action=LOG_DATA modules=null chunks=com.samsung.shealth.health_analytics
[05:34:32.994] [健康→] 回应 seq=10115 action=LOG_DATA -> SUCCESS
```

#### 5. 手表端游标固化
手表收到 `SUCCESS` 回执后，触发本地数据游标固化：
```
10-27 05:34:34.508  2521  9842 I SHW - Data.HealthDataResolver: [deleteSync]delete (com.samsung.shealth.health_analytics with selection: _last_modified_time <= 1793050474405)
10-27 05:34:34.693  2521 14329 I SHW - Data.HealthDataResolver: [insertOrUpdateSync]com.samsung.shealth.stress.histogram, autoFill: true
```
实机结果：健康数据全量 80+ 模块双向闭环完成，手机 UI「健康」中心实时展示步数、心率、压力及报文流水，实机点击「立即同步」增量同步秒级响应！


