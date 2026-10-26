# Google Wear 传输层与 Samsung WCS：按官方源码还原

本文件已纠正早期仅凭抓包推断的字段名。实机结果与权限核验见 [LIVE-VERIFICATION.md](LIVE-VERIFICATION.md)。

## 1. 实际调用链

```text
Watch7 Plugin：CommonMessageSenderImpl.sendMessageToNode
    → J.d(context).j(nodeId, path, jsonBytes)（Wear MessageClient）
    → Google Wear：hbj.c（组装消息、包名、应用签名摘要、目的节点）
    → hbk / hau（传输队列、消息分片）
    → hbt.f 或 hbt.d（计算完整消息摘要，生成 MessagePiece）
    → hbt.a（4B 大端长度 + protobuf）
    → BluetoothSocket OutputStream
```

经典蓝牙连接由 Wear 的 `hfb.run()` 使用 `createRfcommSocketToServiceRecord(hfc.a).connect()` 建立；`hfp` 还实现同 UUID 的服务端监听。两种角色均存在，不能凭某次服务端监听日志就断定所有数据连接都是手表主动连入。

Samsung Plugin 还包含 DirectProfile / Flow / HFP 等独立路径；因此不能笼统地说“插件从不开任何 socket”。Samsung WCS 的普通 MessageClient 消息走上述 Google Wear 数据链路。

官方 Plugin 和 Wear CN 在本次 Honor 实机上均为非 SYSTEM、非 PRIVILEGED 的普通 `/data/app` 应用。`BLUETOOTH_PRIVILEGED` 的 manifest 声明不等于实际授予；数据 RFCOMM 通路不要求应用直接发送 HCI 命令。

## 2. UUID 与角色

| 通道 | UUID | 观察/源码 |
|---|---|---|
| WearableBt / DATA_LAYER | `5e8945b0-9525-11e3-a5e2-0800200c9a66` | hfb 客户端、hfp 服务端；本机数据直连观察到 ch5 / ch8 |
| DirectProfileSecure | `cee50b63-5f79-4727-8b5d-504eb07e1e22` | 三星直连控制通道；手机服务端 |
| Flow | `fafbdd20-83f0-4389-addf-917ac9dae5b2` | 文件传输；手机服务端 |
| SAK secure | `0aa62597-f040-42ef-82e4-0314fdfb7478` | 配网安全路径 |
| SAK insecure | `0bb62597-f040-42ef-82e4-0314fdfb7478` | 配网安全路径 |

RFCOMM channel 是 SDP 记录中的动态值，不是 UUID 的固定属性。本机试图在相同设备/SCN 上建立第二条客户端数据连接会被拒绝；当前调试临时禁用官方包以释放通道。

## 3. 外层 MessagePiece

来源：`wear/sources/defpackage/{hjp,hbt,hbp}.java`。

```text
[4B big-endian serialized MessagePiece length][MessagePiece protobuf]
```

```proto
// 根据官方代码恢复的说明性字段名
message MessagePiece {
  bytes  messageBytes = 1; // inner（单片时为完整 hjo 消息）
  string digest       = 2; // Base64URL(SHA1(完整 inner 字节))，无 '='
  int32  pieceIndex   = 3; // 从 1 开始
  int32  pieceCount   = 4;
  int32  queueId      = 5; // 可省略；3/4 等是队列 ID，不是 REQUEST/RESPONSE
}
```

编码：`hbt.d()` / `hbt.f()`；校验：`hbt.c()` / `hbp.run()`；摘要算法：`gyi.n/q/s()`。

```python
digest = base64.urlsafe_b64encode(hashlib.sha1(inner).digest()).decode().rstrip("=")
```

旧代码中的 `requestId`、`envType`、`TYPE_REQUEST/TYPE_RESPONSE` 是历史误命名。Kotlin / Python 暂保留部分旧接口名以兼容调用，正常编码已按内容计算摘要；`TYPE_*` 常量现为 `QUEUE_*` 的兼容别名。

### 3.1 分片边界

`hbt.f()` 默认每片消息数据上限由调用方给出，`hbk` 当前设置为 12288B。多片必须携带同一完整消息摘要，并按 queueId、pieceIndex/pieceCount 重组后再校验。

RFCOMM MTU 990B 对应用透明；一个 2313B 的应用消息跨多个 HCI/L2CAP/RFCOMM 包，不代表应用层有 26 个 MessagePiece。双向读写本身不会因为插入反向消息破坏流的顺序；此前“逐帧 ACK 导致物理分片冲突”没有证据，撤回。

### 3.2 心跳

inner `5a00`（tag11 空消息）内容恒定，因此摘要恒定。它是公开算法对固定输入的结果，不是秘密 token。

官方 `hbp` 读到非心跳后设置 writer 标志并唤醒 `hbk`；writer 排空消息队列后可发送空心跳。源码没有支持“必须逐消息回显摘要”或“固定每 10 秒心跳”的结论。watchfree 目前保留 10 秒定时发送作为实现选择。

## 4. inner 消息与应用头

| inner tag | 官方类/角色 | 说明 |
|---|---|---|
| 7 | hiv / Connect 消息 | 旧称 NodeInfo；hbr 负责连接身份/版本校验 |
| 8 | hkb / SyncStart | **旧称 NodeAlloc，现已纠正**；gyo.b/e、gyp.i 负责初始 DataItem 同步 |
| 9 | hjx / DataItem 相关消息 | 旧称 WearPath；含 wear:// 路径与数据/版本元信息 |
| 10 | hjv / 应用消息 | 旧称 WearableMessage；应用路径 + payload |
| 11 | hjh / Heartbeat | 空消息 |

`gyo.b()` 将持久化的节点→最高 seqId 映射放入 SyncStart；f1 来自该映射中对端节点的 seqId（无记录时 -1），f2 是各节点及对应 seqId，f3 在本版代码中为 2。它不是“为当前会话分配节点”的请求，不能把收到对端 SyncStart 当作对我们消息的认证确认。

`hbj.c()` 与 `hjv` 描述符恢复的应用消息头：

| tag | 含义 |
|---|---|
| 1 | 消息 ID（历史变量 code）；抓包常见递增，未证明必须从某个值开始 |
| 2 | 应用包名 |
| 3 | 应用签名摘要（gtu.signatureDigest），不是节点 ID/会话 token |
| 4 | 目的 nodeId |
| 6 | 路径 |
| 7 | payload 字节（可以是 JSON 或 protobuf） |
| 8 | 源 nodeId |
| 10 | 消息处理参数；详细含义继续追调用方，不与外层 queueId 混淆 |
| 13 | MessageOptions 对应枚举值 |

`gtw.d()` 对应用签名证书字节计算 SHA-1；`gtu` 组合包名与签名摘要为 AppKey。抓包中的 Google Wear 与 Samsung Plugin 各有不同 40-hex 值，不能替换为手机 nodeId。

## 5. 连接校验与早期误判

官方 `hbr` 先发本地 Connect，再读对端 Connect，通过 `hbt.c()` 验证摘要后检查节点 ID、迁移标志、版本等。其代码包含已登记 clientNodeId 的一致性检查。

早期实验修改设备名/时间字段或随机化所谓 requestId，却没有同步重算摘要。这些实验不能证明整个 Connect 消息是静态配对凭据。原样重放可用，只能说明该样本的摘要与节点身份适合这台已配对设备。

45 秒业务观察证明的是已配对设备的普通应用直连；尚未完成零配对初始化、可靠冷启动重连、完整 DataItem 持久化和分片重组。当前 Kotlin `nodeAllocFrame` 仍使用历史同步表样本值，后续必须按 `gyo` 的持久化 seqId 逻辑替换。

## 6. 实机结果与入口

2026-10-26 两轮：

- 首轮只修摘要：收到激活确认、状态响应、quick_settings 与大量 DataItem，45 秒观察未断开。
- 次轮移除 MODIFY_PHONE_STATE / BLUETOOTH_PRIVILEGED 声明、停用 HFP 隐藏调用：收到激活/状态响应、120 条 WearPath，45 秒观察未断开。
- 观察期间 Samsung 两包和 Wear CN 均处于 disabled-user；结束后主动停止 watchfree，恢复官方三包。

详细时间、日志与剩余工作见 [LIVE-VERIFICATION.md](LIVE-VERIFICATION.md)。抓包编解码工具为 [`snoop/wcs_codec.py`](snoop/wcs_codec.py)，协议回归测试在 `watchfree/src/test/java/dev/ricky/watchfree/wcs/WcsCodecTest.kt`。
