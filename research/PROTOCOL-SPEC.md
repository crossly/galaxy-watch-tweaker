# Samsung Galaxy Watch 独立直连协议全景技术规范 (Protocol Specification)

> **版本**：v1.0 (2026-10)  
> **适用设备**：Samsung Galaxy Watch 全系列（Watch4 / Watch5 / Watch6 / Watch7 / Watch Ultra 等，Wear OS 4 / 5，One UI 5 / 6 Watch）  
> **对端环境**：任意标准 Android 8.0+ 手机（无需 Root、无需 LSPosed、无需安装三星官方组件）  
> **维护规则**：**本文档为该项目协议的唯一定义源（Single Source of Truth）。后续逆向解析出新子系统、通信通道、字段定义或握手时序变更时，必须第一时间同步维护并更新本文档。**

---

## 目录
1. [体系架构与协议栈全景](#1-体系架构与协议栈全景)
2. [物理与传输链路层规范](#2-物理与传输链路层规范)
3. [消息信封与帧序列化协议](#3-消息信封与帧序列化协议)
4. [会话生命周期与核心握手状态机](#4-会话生命周期与核心握手状态机)
5. [三大业务通信通道](#5-三大业务通信通道)
   - [5.1 WCS JSON RPC 通道](#51-wcs-json-rpc-通道)
   - [5.2 Wear OS DataItem 状态同步通道](#52-wear-os-dataitem-状态同步通道)
   - [5.3 Samsung Health 双向健康数据通道](#53-samsung-health-双向健康数据通道)
6. [核心业务子系统协议详解](#6-核心业务子系统协议详解)
   - [6.1 系统与基础状态交换 (Internal / Status)](#61-系统与基础状态交换)
   - [6.2 设备护理与电池管理 (DeviceCare / Battery)](#62-设备护理与电池管理)
   - [6.3 官方表盘控制与排序协议 (Watchface)](#63-官方表盘控制与排序协议)
   - [6.4 通知桥接与即时下发协议 (Notification Bridger)](#64-通知桥接与即时下发协议)
   - [6.5 辅助功能与系统设置 (Settings / Accessibility)](#65-辅助功能与系统设置)
   - [6.6 Samsung Health 80+ 模块字典与数据结构](#66-samsung-health-80-模块字典与数据结构)
   - [6.7 媒体控制与桥接协议 (Media Bridging)](#67-媒体控制与桥接协议)
   - [6.8 查找手表协议 (Find My Watch)](#68-查找手表协议)
7. [全局常量、UUID、包名与白名单速查表](#7-全局常量uuid包名与白名单速查表)
8. [调试命令与权威复核诊断手册](#8-调试命令与权威复核诊断手册)
9. [协议演进与维护规范](#9-协议演进与维护规范)

---

## 1. 体系架构与协议栈全景

### 1.1 协议分层模型

```
┌────────────────────────────────────────────────────────────────────────┐
│                          业务子系统 (Subsystems)                        │
│  表盘切换 │ 设备护理/PSM │ 80+健康同步 │ 通知桥接 │ 系统设置 │ 查找手表  │
├────────────────────────────────────────────────────────────────────────┤
│                       多路复用信道 (Multiplexing)                       │
│  WCS JSON RPC (/wcs_extension/...) │ DataItem (Tag 9) │ Health (GZIP) │
├────────────────────────────────────────────────────────────────────────┤
│                    Wearable RPC 信封层 (Protobuf Tag 10)               │
│  Generation 代数重排队列 │ SHA-1 Base64URL 摘要校验 │ 节点与包名鉴权     │
├────────────────────────────────────────────────────────────────────────┤
│                     帧分界层 (Framing & Delimitation)                  │
│               [4-byte Big-Endian Length] + [Protobuf Payload]          │
├────────────────────────────────────────────────────────────────────────┤
│                     传输层 (Bluetooth Transport Layer)                 │
│              Classic Bluetooth (BR/EDR) RFCOMM SCN 5/8 ( ch8 )         │
└────────────────────────────────────────────────────────────────────────┘
```

### 1.2 关键架构定论
1. **纯标准 API 直连**：手机端仅需调用标准 `BluetoothDevice.createRfcommSocketToServiceRecord(UUID)`，无需系统签名、无需 Root、无需厂商私有权限（如 HFP AT 命令或三星自定义蓝牙驱动）。
2. **单 SCN 互斥约束**：Android 系统的 BlueDroid / Fluoride 蓝牙协议栈限制同一个远程 SCN（Server Channel Number，本固件通常为 channel 8）只能存在一个活跃的 Client 客户端连接。因此，手机端官方三星三件套（`com.samsung.wearable.watch7plugin`、`com.samsung.android.app.watchmanager`、`com.google.android.wearable.app`）必须被冻结或卸载，否则会导致 `Connection refused`。
3. **两端节点身份拓扑**：
   - **手机节点 ID (`phoneNode`)**：当前手机在 Wear OS 拓扑中的唯一 7~8 位十六进制 ID（如 `20a3039`）。
   - **手表节点 ID (`watchNode`)**：手表端分配的十六进制节点 ID（如 `9940fa14`）。
   - **会话 Token (`sessionToken`)**：手表与手机配对绑定的 40 位 SHA-1 Hex 认证凭证（如 `9ca5170f381919dfe0446fcdab18b19a143b3163`）。

---

## 2. 物理与传输链路层规范

### 2.1 蓝牙服务定义
* **服务 UUID**：`5e8945b0-9525-11e3-a5e2-0800200c9a66`（Google Wear OS 原生 RFCOMM 服务标识）。
* **物理协议**：Bluetooth BR/EDR (Classic Bluetooth) RFCOMM。
* **信道号 (SCN)**：手表 SDP 记录中广播的动态通道，默认实测为 **Channel 8**（部分固件偶见 Channel 5）。

### 2.2 链路帧分界 (Framing)
所有通过 RFCOMM 套接字发送和接收的数据单元均使用**显式长度前缀**分界：
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                  4-Byte Big-Endian Payload Length             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                      Protobuf MessagePiece                    |
|                        (Length bytes)                         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

---

## 3. 消息信封与帧序列化协议

### 3.1 外层信封 (Protobuf `MessagePiece`)
外层 Protocol Buffer 协议结构定义如下：

| Tag 序号 | Wire Type | 字段含义 | 说明 |
|:---:|:---:|:---|:---|
| **1** | Varint | `code / sequence` | 本帧唯一序列号，自增计数 |
| **2** | Length-delimited | `digest` | 内层载荷的摘要签名（**不可省略**） |
| **5** | Varint | `queueId` | 传输队列优先级，默认固定为 `0` |
| **9** | Length-delimited | `dataItem` | Wear OS 系统级 DataItem 同步载荷 |
| **10** | Length-delimited | `wearableMessage` | WCS 及健康消息的核心 RPC 消息体 |

### 3.2 Field 2 摘要签名算法（权威公式）
手表端的 WearableService 会严格校验 Field 2 的校验和。若校验失败，帧会被底层直接丢弃，不予分发：
$$\text{Digest} = \text{Base64URL\_NoPadding}\Big(\text{SHA-1}\big(\text{InnerMessageBytes}\big)\Big)$$
* 取内层序列化后的字节数组 `inner`。
* 计算标准 SHA-1 哈希（20 字节）。
* 执行 **Base64URL** 编码（替换 `+` 为 `-`，`/` 为 `_`，并省略末尾的 `=` 填充符）。

### 3.3 内层信封 (Protobuf `WearableMessage` - Tag 10)
Tag 10 内部包含路由与调度元数据：

| Tag 序号 | Wire Type | 字段名 | 含义与取值 |
|:---:|:---:|:---|:---|
| **1** | Varint | `code` | 消息序号，对应内层 MessageId（每次发送自增） |
| **2** | Length-delimited | `senderPkg` | 发送端包名（WCS 默认 `com.samsung.wearable.watch7plugin`；健康同步必须为 `com.sec.android.app.shealth`） |
| **3** | Length-delimited | `senderNode` | 发送端凭证（WCS 默认传 40 位 `sessionToken`；健康同步必须为官方证书签名 `9741a0f330dc...`） |
| **4** | Length-delimited | `sourceNode` | 手机节点 ID（`phoneNode`，如 `20a3039`） |
| **5** | Length-delimited | `destNode` | 手表节点 ID（`watchNode`，如 `9940fa14`） |
| **6** | Varint | `type` | 传输方向：`3` = Request（手机发给手表）；`4` = Response（回执） |
| **8** | Length-delimited | `path` | 业务路径 URI（如 `/wcs_extension/sysui/watchface`） |
| **9** | Length-delimited | `payload` | 业务数据体（通常为 UTF-8 JSON 文本或二进制数据） |
| **10** | Varint | `generation` | **会话代数（重要）**，用于 GMS 乱序与重连排队机制 |

### 3.4 GMS `ReorderingQueue` 与 `Generation` 代数机制
* **丢帧根因分析**：Wear OS GMS 的 `hay.java` / `hbg.java` 为每个 peerNode 维护一个按 `(generation, sequence)` 重排的优先级队列。当发生蓝牙断开重连时，如果客户端的 `sequence` 重置（从 2 开始计数），但 `generation` 保持不变，GMS 会判定所有 sequence 小于上一次会话最大序列号的帧均为**过期历史帧**并执行 `DROPPED` 静默丢弃。
* **规约要求**：
  1. 手机端连接建立（收到 `SyncStart` 时），必须令 `generation = generation + 1`。
  2. 初始 generation 可基于当前时间戳生成：`((System.currentTimeMillis() / 1000) % 80000) + 10`。
  3. GMS 收到新 generation 后，日志会输出 `generation changed, resetting`，重置 `expectedSequence`，确保后续指令秒级执行。

---

## 4. 会话生命周期与核心握手状态机

### 4.1 状态机模型

```
[DISCONNECTED]
      │  BluetoothSocket.connect()
      ▼
[RFCOMM_ESTABLISHED]
      │  TX: Connect Frame (Tag 10, path="/connect")
      ▼
[CONNECT_SENT]
      │  RX: Connect Response (Learning watchNode & phoneNode)
      │  TX: SyncStart Frame (携带自增 Session Generation)
      ▼
[SYNC_STARTED]
      │  RX: SyncStart Ack & DataItem Dump
      │  TX: Feature Exchange & Set Device Activation
      ▼
[SESSION_READY] (双向可用，心跳维持，每 15s 活跃刷新)
```

### 4.2 握手消息时序
1. **Connect 请求**：手机连接 RFCOMM 成功后，立即发送 `/connect` 消息，声明自身能力版本（Version=2）。
2. **学习 Node ID**：从手表返回的 Connect 响应中，提取出动态分配的 `sourceNode`（手机 ID，如 `20a3039`）和 `destNode`（手表 ID，如 `9940fa14`）。
3. **SyncStart 触发**：手机发送 `SyncStart` 帧。**必须在此步骤完成 `generation.incrementAndGet()`**。
4. **激活宣告 (Activation)**：发送 `/wcs_extension/internal/connection`，`msgId="set_device_activation"`，宣告手机端准备就绪，手表端开始启动 WCS 全量子系统分发。

---

## 5. 三大业务通信通道

### 5.1 WCS JSON RPC 通道
* **路径特征**：`/wcs_extension/...`
* **目标组件**：`com.samsung.android.wcs.extension`（手表端常驻常开服务）。
* **信封格式 (JSON)**：
```json
{
  "msgId": "string",
  "msgType": 0,
  "deviceId": "9940fa14",
  "sender": "phone",
  "reason": 0,
  "result": 0,
  "data": {}
}
```
* **`msgType` 规范**：
  * `0`：**Query / Get**（查询请求，如获取表盘列表、电量详情）
  * `1`：**Set / Command**（配置写入，如切换表盘、设置灰度）
  * `2`：**Event / Broadcast**（单向通知事件）
  * `3`：**Response / Ack**（应答回执，手表处理完毕后返回）
  * `4`：**State Notice**（系统状态主动推送）

### 5.2 Wear OS DataItem 状态同步通道
* **协议位置**：Protobuf Tag 9（`dataItem`）。
* **URI 规范**：`wear://<phoneNode>/<subsystem>/<item_key>`
* **数据格式**：包含 DataItem 路径、操作类型（`1` = Put / Sync，`2` = Delete）、时间戳以及二进制 DataMap 键值序列。
* **签名凭据要求**：针对特定受保护系统配置（如辅助功能），必须在 DataItem 内层包含正确的签名上下文，否则手表端 `ExclusiveTaskManager` 拒绝应用。

### 5.3 Samsung Health 双向健康数据通道
* **路径**：`/samsung_health/wearable_message`（手表上行常带 `/{seq}` 路由后缀）。
* **GMS 白名单鉴权规则**：
  * **发送包名 (`senderPkg`)**：`com.sec.android.app.shealth`
  * **签名指纹 (`senderNode`)**：`9741a0f330dc2e8619b76a2597f308c37dbe30a2`
* **外层信封结构**：
```json
{
  "message": "REQUEST" | "RESPONSE",
  "type": "DATA",
  "sender": "com.samsung.android.app.shealth.wearable.syncmanager",
  "receiver": "com.samsung.android.wear.shealth.wearable.syncmanager",
  "version": 5.03,
  "sequence_num": 10114,
  "source_node": "com.sec.android.app.shealth",
  "destination_node": "9940fa14",
  "body": "<Base64 encoded gzip JSON>"
}
```
* **解密算法**：`Base64.decode(body) -> GZIPInputStream -> UTF-8 String -> JSON Array`。
* **三段式握手与游标闭环**：
  1. 手机发起：`message="REQUEST"`, `action="REQUEST_DATA"`。
  2. 手表回传：`message="REQUEST"`, `action="SYNC_DATA"`, sequence=N，携带具体模块数据变更。
  3. 手机应答：`message="RESPONSE"`, `action="SYNC_DATA"`, `result="SUCCESS"`, sequence=N。
  4. 手表上报：`message="REQUEST"`, `action="LOG_DATA"`, sequence=M。
  5. 手机应答：`message="RESPONSE"`, `action="LOG_DATA"`, `result="SUCCESS"`, sequence=M。
  6. 手表本地固化：执行 `HealthDataResolver.insertOrUpdateSync`，提交 `_last_modified_time` 游标。

#### 5.3.1 `device_info` 字段定义
```jsonc
{"device": "BKQ-AN90",        // 手机侧填写本机 Build.MODEL
 "device_type": 10062,         // 10062 = Wear OS 手表能力档（手机侧声明对端能力）
 "version": 4.51,              // 能力协议版本
 "last_sync_time": 1793052221768,
 "start_time": 0,              // 本次同步窗口起点
 "end_time": 1793052221768,    // 本次同步窗口终点
 "is_last_chunk": true}        // 分块传输的末块标记
```

| `device_type` | 设备 | 能力协议版本 |
|:---:|:---|:---|
| 10019 / 10020 / 10022 / 10023 | Gear Fit / Gear1 / Gear2 / Activity Tracker | 1.01 |
| 10024 / 10030 | Tizen Gear1 / Gear S | 2.01 / 3.01 |
| **10062** | **Wear OS 手表（Watch4 及以上）** | **4.51** |
| 0 | 未分类 / 手机侧默认 | — |

#### 5.3.2 游标语义（实测定论）
* 同步游标由**手表端**持有（存于手表 Shealth DB 的 `_last_modified_time`），手机侧无法直接复位。
* 实测把 `start_time` 置 `0`、`last_sync_time` 置当前时间，**并不会**触发手表回补全量历史；手表仍只下发“自上次成功固化以来有变更”的行。
* 因此客户端必须自行持久化最近一次同步到的指标值（步数 / 心率 / 压力），否则重启后会丢失展示数据。
* 少数“活体”表（如 `com.samsung.shealth.stress.histogram`、`com.samsung.health.movement`）每次同步都会因其 `update_time` 被刷新而重发；其 `histogram` / `binning_data` 字段本身是 **Base64(gzip(二进制分箱数组))**。

---

## 6. 核心业务子系统协议详解

### 6.1 系统与基础状态交换
* **`/wcs_extension/internal/connection`**
  * 指令：`set_device_activation`（`msgType=1`）
  * 作用：宣告手机应用已在线，激活手表侧所有子系统的 EventListener。
* **`/wcs_extension/internal/feature`**
  * 指令：`feature_exchange`（`msgType=0` 查询，`msgType=3` 响应）
  * 作用：交换双方固件支持的特性集（包含 736 项系统能力掩码）。
* **`/wcs_extension/internal/status`**
  * 指令：`status_exchange`
  * 作用：上报手表国家码（`countryCode`，如 `"CN"`）、eSIM 识别码（`eid`）、Google Play Services 版本等关键系统指纹。
* **`/wcs_extension/internal/p2p`**
  * 指令：`wifi_p2p_feature`
  * 作用：Wi-Fi Direct 高速直连信道协商。

### 6.2 设备护理与电池管理
* **`/c_devicecare/battery/base`**
  * 下发查询：`msgType=0`
  * 响应字段：
    * `level` / `battery_level` (int)：当前电量百分比（0~100）。
    * `status` (int)：充电状态（1=充电中, 2=放电中, 3=已充满）。
    * `time_remaining` (long)：充至 100% 预计剩余毫秒数。
    * `rut_psm_off` (int)：常规模式下电池预估可用分钟数。
    * `rut_psm_on` (int)：省电模式开启下预估可用分钟数。
* **`/c_devicecare/battery/psm` (省电模式切换)**
  * 指令：`msgType=1`，`data={"enable": true|false}`
  * 异步回执通道：`/c_devicecare/battery/psm`，`extraData=1` (已开启) 或 `0` (已关闭)。
  * 注意事项：PSM 切换涉及手表多核调频与显示重载，回执通常在 1~25 秒内异步返回。
* **`/c_devicecare/memory/usage` (运行内存)**
  * 响应字段：
    * `mem_total`：RAM 总量字节数（如 1,846,980,608 B ≈ 1.76 GB）。
    * `mem_available`：可用内存字节数。

### 6.3 官方表盘控制与排序协议
* **通信路径**：`/wcs_extension/sysui/watchface`
* **手表端处理宿主**：`com.samsung.android.wcs.extension.application.gms.WatchfaceApiStub`

#### 6.3.1 核心指令与格式

1. **查询当前表盘与收藏列表 (`watchface_order`)**
```json
// TX: 手机请求
{"msgId":"watchface_order","msgType":0,"deviceId":"9940fa14","data":{}}

// RX: 手表应答 (msgType=3)
{
  "msgId": "watchface_order",
  "msgType": 3,
  "result": 0,
  "data": {
    "WatchFaceOrderType": "recent",
    "favoriteIdOrderList": [5, 1, 4, 3]
  }
}
```

2. **切换活跃表盘 (`change_active_watchface`)**
```json
// TX: 手机写入请求 (msgType=1)
{
  "msgId": "change_active_watchface",
  "msgType": 1,
  "deviceId": "9940fa14",
  "data": {
    "packageName": "com.samsung.android.watch.watchface.ultrainfoboard",
    "className": "com.samsung.android.watch.watchface.ultrainfoboard.UltraInfoBoardWatchFaceService"
  }
}

// RX: 手表应答 (msgType=3)
{"msgId":"change_active_watchface","msgType":3,"result":0,"data":{}}
```

#### 6.3.2 官方表盘库映射字典 (Watch Face Presets)

| 表盘名称 (中文) | 表盘预设 ID | 包名 (`packageName`) | Service 类名 (`className`) |
|:---|:---|:---|:---|
| **极限信息板** | `ultra_info_board` | `com.samsung.android.watch.watchface.ultrainfoboard` | `...ultrainfoboard.UltraInfoBoardWatchFaceService` |
| **极限指针** | `ultra_analog` | `com.samsung.android.watch.watchface.ultraanalog` | `...ultraanalog.UltraAnalogWatchFaceService` |
| **模块化指针** | `analog_modular` | `com.samsung.android.watch.watchface.analogmodular` | `...analogmodular.AnalogModularWatchFaceService` |
| **经典指针** | `classic_adventurer` | `com.samsung.android.watch.watchface.classicadventurer` | `...classicadventurer.ClassicAdventurerWatchFaceService` |
| **数字画板** | `digital_board` | `com.samsung.android.watch.watchface.digitalboard` | `...digitalboard.DigitalBoardWatchFaceService` |
| **信息指针** | `info_modular` | `com.samsung.android.watch.watchface.infomodular` | `...infomodular.InfoModularWatchFaceService` |
| **运动健康面板** | `activity_tracker` | `com.samsung.android.watch.watchface.activitytracker` | `...activitytracker.ActivityTrackerWatchFaceService` |
| **极简数字** | `simple_classic` | `com.samsung.android.watch.watchface.simpleclassic` | `...simpleclassic.SimpleClassicWatchFaceService` |
| **经典高级** | `premium_analog` | `com.samsung.android.watch.watchface.premiumanalog` | `...premiumanalog.PremiumAnalogWatchFaceService` |
| **动态天气** | `weather_center` | `com.samsung.android.watch.watchface.weathercenter` | `...weathercenter.WeatherCenterWatchFaceService` |
| **世界时钟** | `world_clock` | `com.samsung.android.watch.watchface.worldclock` | `...worldclock.WorldClockWatchFaceService` |
| **呼吸与舒缓** | `breathing` | `com.samsung.android.watch.watchface.breathing` | `...breathing.BreathingWatchFaceService` |
| **渐变流光** | `gradient_modular` | `com.samsung.android.watch.watchface.gradientmodular` | `...gradientmodular.GradientModularWatchFaceService` |
| **活力条纹** | `stripe_digital` | `com.samsung.android.watch.watchface.stripedigital` | `...stripedigital.StripeDigitalWatchFaceService` |
| **大字时间** | `large_digits` | `com.samsung.android.watch.watchface.largedigits` | `...largedigits.LargeDigitsWatchFaceService` |

#### 6.3.3 推荐表盘推送通道（手表 → 手机）
* **路径**：`/w_watchface_home/watchface/msg`
* **方向**：手表主动下发（手机侧 `type=4` 收到）
* **载荷**：
```jsonc
{
  "msgId": "watchface_recommend_list_send",
  "watchface_recommend_list": ["com.samsung.android.watch.watchface.ultrainfoboard_0"]
}
```
* 该通道用于推送三星运营位推荐的表盘（末尾 `_N` 为数据层里的配置序号），可与 `addable_watchface_info` 配合做「推荐表盘」展示。

#### 6.3.4 表盘效果图的获取方式（重要限制）
* 手表 Display 带 **`FLAG_SECURE`**，`adb shell screencap` 只能拿到黑色/纯色画面（实测 UI 层级里有真实内容但截图为空），**无法用截屏生成表盘预览图**。
* 可靠来源：Galaxy Wearable 插件（`com.samsung.wearable.watch7plugin`）内置的官方表盘效果图：
  `assets/Category_watchface/<packageName>.<ServiceSimpleName>.webp`（360×360 RGBA）。
* 由此还能反推出每个表盘的准确 Service 类名 —— 文件名前缀即包名，后缀即类名。
* 本项目用 `scripts/gen_watchface_assets.py` 把这些 webp 转成 `drawable-nodpi/wf_<id>.webp` 并自动生成 `WatchFacePreset.kt`。

#### 6.3.5 表盘排序方式写入（已实机验证）
* **msgId**：`watchface_order_type`，**msgType=1 为写请求**、`3` 为回执。
* **载荷映射**（对应手表端 `WatchfaceApiStub.setWatchfaceOrderTypeWithSync(type)`）：

| `type` | `WatchFaceOrderType` | `isWatchSortByRecentsEnabled` | 含义 |
|:---:|:---|:---:|:---|
| 0 | `custom` | `false` | 自定义顺序 |
| 1 | `recent` | `true` | 最近使用优先 |
| 2 | `alphabetic` | `false` | 字母序 |

* 实机回执：`{"msgId":"watchface_order_type","msgType":3,"result":0,"reason":0,"data":{}}`，
  随后 `watchface_order` 的 `favoriteIdOrderList` 会按新模式重排。

#### 6.3.5.1 表盘「数据位 / 复杂功能」配置能力边界（重要结论）

**结论：三星 WCS 直连协议不提供表盘数据位的远程读/写能力。**

证据一 —— 手表端只接受 5 个 msgId（`WatchfaceApiStub.onMsgRecvListener` 反编译，`default` 分支直接抛 `unknown msg id`）：

| 常量 | msgId | 可选 msgType |
|:---|:---|:---|
| `MSG_ID_WATCHFACE_ORDER` | `watchface_order` | 0 查询 / 1 变更通知 |
| `MSG_ID_WATCHFACE_ORDER_TYPE` | `watchface_order_type` | 0 查询 / 1 写入 |
| `MSG_ID_WATCHFACE_ACTIVE` | `change_active_watchface` | 0 查询 / 1 写入 |
| `MSG_ID_ADDABLE_WATCHFACE_INFO` | `addable_watchface_info` | 0 查询 / 1 推送 |
| `MSG_ID_WATCHFACE_LAUNCH` | `watchface_launch` | **仅 3（回执）** |

证据二 —— `watchface_setting` 只是**手表本地 SharedPreferences 的名字**（`WatchfaceSettingProvider`，authority `com.samsung.android.wcs.extension.provider.watchface`，唯一 path `order_type`），不是远程配置通道。

证据三 —— `watchface_launch` 实测：手机 → 手表发 `msgType=1` 直接失败：
```json
{"msgId":"watchface_launch","msgType":3,"result":-1,"reason":-1,"data":{}}
```
因为 `handleWatchfaceLaunchMessage` 只处理 `msgType==3`，说明它的真实方向是 **手表 → 手机**：
用户在手表上点「在手机上编辑表盘」时，手表发 `watchface_launch` 请求，手机侧伴侣 App 打开编辑器后回 `msgType=3`。

证据四 —— `addable_watchface_info` 实测只返回角标：
```json
{"msgId":"addable_watchface_info","msgType":1,"data":{"watchfaceBadgeList":"[]"}}
```
不含数据位定义。

**真正的数据位编辑链路在哪？** 在 Google 的 Wear OS 伴侣 SDK（Galaxy Wearable 插件内嵌）：
* `com.google.android.libraries.wear.companion.watchfaces.editor.LiveWatchFaceEditor`
  （`startEditing` / `getComplicationSlots` / `setComplication` / `setStyle` / `saveChanges` / `getPreview`）
* 实现体在 GMS 私有包 `com.google.android.gms.internal.wear_companion.*`，走独立的 GMS Wearable RPC
  （相关路径串：`/wearservices/watchface/resync`、`/wearservices/sync/available_watchface`），
  而不是三星 WCS 通道；GMS 侧按调用方包名做门控。

因此本项目**不再尝试伪造该链路**，而是：
1. 表盘选择 / 收藏排序 / 排序方式 走 WCS（已全部打通）；
2. 数据位与配色样式引导用户在手表上「长按表盘 → 编辑」完成；
3. 监听手表侧 `watchface_launch` 请求并给出对应引导。

> 若要继续攻这条线，下一步应做：在装有官方 Galaxy Wearable 的手机上抓 HCI Snoop，过滤
> `/wearservices/watchface/`，反解 `startEditing`/`setComplication` 的 protobuf 字段号，
> 再用本项目 RFCOMM 通道复刻 —— 属于独立的大工程，需单独立项。

### 6.4 通知桥接与即时下发协议
* **通道规范**：Wear OS 原生 Bridger DataItem
* **URI 格式**：`wear://<phoneNode>/bridger/stream_item:<notification_key_hash>`
* **数据结构 (DataMap Payload)**：
  * `title` (String)：通知标题。
  * `content` / `text` (String)：通知正文内容。
  * `package_name` (String)：发起源应用包名（如 `com.tencent.mm`）。
  * `app_name` (String)：应用友好显示名（如 `"微信"`）。
  * `post_time` (long)：发信时间戳。
  * `id` (int)：通知 ID。
  * `icon` (byte[])：应用图标 PNG/WebP 压缩二进制字节流。
  * `vibrate` (boolean)：是否触发手表马达震动。
  * `actions` (DataMap[])：快速回复或操作按钮列表。

### 6.5 辅助功能与系统设置
* **灰度模式 (Grayscale / Daltonizer)**
  * 通道：Tag 9 DataItem
  * Key：`accessibility_display_daltonizer_enabled`
  * 取值：`0` = 关闭（彩色显示）；`1` = 开启（全局黑白灰度显示）。
  * 生效验证：手表底层 SurfaceFlinger 与 Dalvik ExclusiveTaskManager 实时套用灰度着色矩阵。
* **高对比度文字 (High Contrast Text)**
  * Key：`high_contrast_font` (0/1)。
* **亮度调节 (Brightness)**
  * 路径：`/w_module_sec_watch_setting/screen_brightness`
  * 取值：`0 ~ 255`。
* **息屏显示 (AOD)**
  * Key：`aod_mode` (boolean)。

### 6.6 Samsung Health 80+ 模块字典与数据结构

手表端 `com.samsung.android.wear.shealth` 导出的完整健康数据模块索引表（均支持增量游标同步）：

| 序号 | 模块名称 (Module Key) | 业务含义 | 关键数据字段 |
|:---:|:---|:---|:---|
| 1 | `com.samsung.shealth.tracker.pedometer_step_count` | 全天计步实时数据 | `count`, `calorie`, `distance`, `speed`, `walk_step`, `run_step` |
| 2 | `com.samsung.shealth.tracker.heart_rate` | 连续心率测量 | `heart_rate`, `max_heart_rate`, `min_heart_rate`, `status` |
| 3 | `com.samsung.shealth.stress` | 压力测量 | `score` (0-100), `status`, `start_time`, `end_time` |
| 4 | `com.samsung.shealth.stress.histogram` | 压力区间柱状分布 | `histogram_data`, `binning_data` |
| 5 | `com.samsung.shealth.oxygen_saturation` | 实时与睡眠血氧饱和度 | `spo2` (百分比), `heart_rate` |
| 6 | `com.samsung.shealth.skin_temperature` | 手腕皮肤温度监测 | `temperature` (摄氏度) |
| 7 | `com.samsung.shealth.respiratory_rate` | 呼吸频率监测 | `breath_rate` (次/分) |
| 8 | `com.samsung.shealth.sleep` | 睡眠记录主表 | `start_time`, `end_time`, `quality_score`, `duration` |
| 9 | `com.samsung.shealth.sleep_stage` | 睡眠分期详情 | `stage` (清醒/浅睡/深睡/REM), `start_time` |
| 10 | `com.samsung.shealth.exercise` | 运动健身记录 | `exercise_type`, `duration`, `calorie`, `distance` |
| 11 | `com.samsung.shealth.floors_climbed` | 爬楼层数统计 | `floor_count`, `calorie` |
| 12 | `com.samsung.shealth.best_records` | 运动最佳成就 | `record_type`, `record_value`, `achieve_time` |
| 13 | `com.samsung.shealth.badge` | 获得勋章与里程碑 | `badge_id`, `badge_type`, `issue_time` |
| 14 | `com.samsung.shealth.service_preferences` | 健康服务用户偏好 | `target_steps`, `unit_system`, `alert_configs` |
| 15 | `com.samsung.health.hrv` | 心率变异性 | `rmssd`, `sdnn` |
| 16 | `com.samsung.shealth.body_composition_goal` | 身体成分目标 (体脂/骨骼肌) | `target_fat_mass`, `target_skeletal_muscle` |

### 6.7 媒体控制与桥接协议
* **通信路径**：`/adaptive_media_bridging/rpc/media_update`
* **协议格式**：Protobuf 二进制查询。
* **特性**：手机可查询手表端正在播放的音频元数据（歌名、艺术家、播放状态、进度条），亦可向手表分发手机端的 MediaSession 控制事件（上一曲、下一曲、暂停、播放、音量）。

### 6.8 查找手表协议
* **通信路径**：`/wcs_extension/internal/findmywatch`
* **指令**：`start_find_watch` / `stop_find_watch`
* **行为**：手表立刻开启最高音量响铃、屏幕最大亮度闪烁、线性马达最大震动，直到手机端发送 `stop` 或手表屏幕被滑动确认。

---

## 7. 全局常量、UUID、包名与白名单速查表

### 7.1 蓝牙与服务 UUID
* **Wear OS RFCOMM UUID**：`5e8945b0-9525-11e3-a5e2-0800200c9a66`
* **默认信道 (SCN)**：`8`

### 7.2 包名常量表
* 官方手表插件：`com.samsung.wearable.watch7plugin`
* 官方手表管理：`com.samsung.android.app.watchmanager`
* 官方健康手机端：`com.sec.android.app.shealth`
* 官方健康手表端：`com.samsung.android.wear.shealth`
* 手表 WCS 扩展服务：`com.samsung.android.wcs.extension`
* 手表 SystemUI 宿主：`com.samsung.android.wearable.sysui`
* 手表设置管理：`com.google.android.apps.wearable.settings`
* 本独立直连管理应用：`dev.ricky.galaxywatchmanagerfree`

### 7.3 证书指纹白名单 (SHA-1)
* **Samsung Health 官方证书指纹**：`9741a0f330dc2e8619b76a2597f308c37dbe30a2`
* **Galaxy Watch 官方平台签名指纹**：`9ca5170f381919dfe0446fcdab18b19a143b3163`

---

## 8. 调试命令与权威复核诊断手册

在开发调试与实机验证过程中，可使用以下 Shell 命令进行协议与系统状态的权威复核：

### 8.1 表盘切换生效复核 (手表端)
```bash
adb -s <WATCH_IP>:<PORT> shell dumpsys wallpaper | grep mWallpaperComponent
# 期望输出：
# mWallpaperComponent=ComponentInfo{com.samsung.android.watch.watchface.<id>/...WatchFaceService}
```

### 8.2 GMS Wearable 消息流转监控 (手表端)
```bash
adb -s <WATCH_IP>:<PORT> shell dumpsys activity service com.google.android.gms/.wearable.service.WearableService
# 检查：
# 1. Inbound 接收流：[lo:<generation>:<seq>] 格式
# 2. ReorderingQueue 状态：确认 generation changed 时触发 reset
# 3. receive 行：确认目标系统组件已真正消费本帧
```

### 8.3 Samsung Health 数据游标监控 (手表端)
```bash
adb -s <WATCH_IP>:<PORT> logcat -v time | grep -iE "SHWCOM|SHW -|HealthDataResolver"
# 观察：
# 1. onMessageReceived(): path /samsung_health/wearable_message
# 2. getUpdatedDataCursor: 打开指定健康表的游标
# 3. insertOrUpdateSync: 手机回执 SUCCESS 后本地更新时间戳
```

### 8.4 手机端调试广播指令集
```bash
# 查询当前表盘
adb shell am broadcast -n dev.ricky.galaxywatchmanagerfree/.core.WatchCommandReceiver -a dev.ricky.galaxywatchmanagerfree.CMD --es cmd watchface_get

# 切换为指定预设表盘
adb shell am broadcast -n dev.ricky.galaxywatchmanagerfree/.core.WatchCommandReceiver -a dev.ricky.galaxywatchmanagerfree.CMD --es cmd watchface_set --es id ultra_analog

# 发起全量健康数据增量同步
adb shell am broadcast -n dev.ricky.galaxywatchmanagerfree/.core.WatchCommandReceiver -a dev.ricky.galaxywatchmanagerfree.CMD --es cmd health

# 触发省电模式 (PSM) 切换
adb shell am broadcast -n dev.ricky.galaxywatchmanagerfree/.core.WatchCommandReceiver -a dev.ricky.galaxywatchmanagerfree.CMD --es cmd psm --ei state 1
```

---

## 9. 协议演进与维护规范

为保证项目的长期工业级可靠性，所有参与本项目的开发者与协作 AI 代理必须严格遵守以下维护准则：

1. **唯一真相源 (SSOT)**：本项目所有与三星手表蓝牙通信、信封打包、编解码算法、业务路径、字段定义有关的代码，均以本文档为规范基准。
2. **变更实时同步**：
   - 当通过抓包（HCI Snoop / Wireshark）、反编译（JADX）或代码调试解析出新的 WCS 路径时，必须在本文档对应章节增补路径格式、请求/响应载荷示例。
   - 当发现新表盘、新健康模块或新的 DataItem 键时，必须同步追加到字典速查表中。
3. **实机验证前置**：
   - 任何协议条目在标记为“可用/已打通”之前，必须经过双设备实机验证（真机日志 + dumpsys 交叉复核）。未经验证或失败的路径必须明确标记限制边界。
4. **版本同步维护**：
   - 当协议发生重大扩充（如新增运动模式控制、心电 ECG / 血压通道破解等），文档版本号按 `v1.x` 向上递增，并在修订记录中详细陈述变更日志。

---
*(End of Specification - Galaxy Watch Manager Free Protocol Engineering Team)*
