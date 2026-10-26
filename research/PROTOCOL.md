# Samsung Galaxy Watch BLE 配对协议逆向笔记

目标：实现独立 app（非三星手机）直连 Galaxy Watch。

> 2026-10-26 更新：普通应用直连已通过两轮各 45 秒业务观察；不需要系统特权安装。Google Wear MessagePiece.f2 是 `Base64URL(SHA1(inner))`，不是随机请求 ID；旧称 NodeAlloc 的 tag8 是 SyncStart。完整源码与实测依据见 [WEAR-TRANSPORT.md](WEAR-TRANSPORT.md) 和 [LIVE-VERIFICATION.md](LIVE-VERIFICATION.md)。本文件重点记录配网阶段，不能把 GATT 的换机弹窗流程直接当成已配对设备的重连流程。
素材：手机实机拉取的正版 APK（jadx 1.5.5 反编译）。

- Watch Manager `com.samsung.android.app.watchmanager` 2.2.72.26092361
  `research/apks/pulled/com.samsung.android.app.watchmanager/base.apk`
- Watch7 Plugin `com.samsung.wearable.watch7plugin` 2.2.15.26071051
  `research/apks/pulled/com.samsung.wearable.watch7plugin/base.apk`

## 1. 传输层（BLE GATT）

来源：`connectionmanager/manager/BLEDeviceManager.java`、`service/BluetoothLeService.java`

| 用途 | UUID |
|---|---|
| Wear OS 手表 GATT service | `eed6d5cc-c3b2-4d7b-8c6b-7acbf7965bb6` |
| RTOS Ring GATT service | `79d34772-bc93-4360-8244-42b50a4a2b27` |
| 数据写 characteristic（写+notify） | `2fb6a5aa-1219-4129-bc97-dd4df70613cd` |
| CCC descriptor（标准） | `00002902-0000-1000-8000-00805f9b34fb` |
| SAK secure RFCOMM | `0aa62597-f040-42ef-82e4-0314fdfb7478` |
| SAK insecure RFCOMM | `0bb62597-f040-42ef-82e4-0314fdfb7478` |
| 经典 BT "data layer" | `5e8945b0-9525-11e3-a5e2-0800200c9a66` |
| 中国区手表标识 | `1E4D4A34-70A0-0708-6AAC-2343A45596ED`（反向 `ED96...4d1E`） |

连接流程（`BluetoothLeService`）：
1. `connectGatt` → connected 后 `requestMtu(512)`
2. `onMtuChanged` → `discoverServices()`
3. `onServicesDiscovered` → 找到上述 service → 在 characteristics 里找 `2fb6a5aa...` 写通道
4. 订阅 CCC (`ENABLE_NOTIFICATION_VALUE`)
5. `onDescriptorWrite` 成功（onCCCSet）→ 开始 `sendInitialData()`

## 2. 帧格式（BLEDataManager，TLV over GATT）

来源：`connectionmanager/communication/BLEDataManager.java`

```
[0]      dataType (1 byte)
[1..5]   payload length (4 bytes, BIG endian)
[5..]    payload
```

dataType 枚举：
- `0` = 公钥交换
- `1` = 证书链（certificate chain）
- `2` = JSON（Samsung 协议消息）
- `3` = raw byte array
- `12` = 非 Samsung JSON（关键！见下）

分片：每 GATT 包 ≤ `BUFFER_SIZE = 500` 字节；MTU 请求 512。
接收端按 length 字段拼接，`onSentData`（onCharacteristicWrite 回调）后发下一片。

## 3. 加密与证书（SAK/GAK）

来源：`connectionmanager/crypto/CryptoManager.java`、`sak/*`

- 算法提供方：`sakverify/CryptoHelper`（ECDH + 对称加解密）
- 密钥：本地生成 keypair，交换公钥（dataType=0），密钥取 `publicKey[0..32]`
- 证书链：`SAKVerifier.getCertificateChain()`，Android Keystore attestation 风格（`AttestatedCertParser`/`AuthorizationList`/ASN.1）
- **关键路径分叉（`WatchDataManager.sendInitialData`）**：
  - `PlatformUtils.isSamsungDevice()` == true → `sendPublicKey()` 走 SAK 加密通道
  - **非三星手机 → `sendNonSamsungPopupRequest()` → `sendDataTLV(json, dataType=12)`（不加密！）**

即：**非三星手机路径根本不做证书验证，直接明文 JSON 对话**。这是独立 app 的突破口。

## 4. 消息协议（JSON over TLV）

来源：`connectionmanager/communication/WatchDataManager.java`

公共字段：`msgId`（消息名）、`sender`（"phone"/"watch"）、`data`（payload 对象）。

### 4.1 非三星初始握手（dataType=12）

**发出** `msg_popup_request`：
```json
{"msgId":"msg_popup_request","sender":"phone","data":{"category":2}}
```
category: `0=RESET_ONLY, 1=RESET_AND_NEW_PHONE, 2=DEPEND_ON_WATCH_STATUS`

**接收** `msg_popup_response`：`data.result: Boolean, data.category: Int`

### 4.2 状态查询（dataType=2）

**发出** `msg_status_request`：`{"msgId":"msg_status_request","sender":"phone"}`
**接收** `msg_status_response`：
```
data.lock_activation: Boolean       // 激活锁
data.new_phone_activation: Boolean   // 是否允许换机
data.account_login: Boolean         // 手表已登录账号
data.is_china: Boolean               // 中国区手表（不支持此流程）
data.is_kids_mode: Boolean          // 儿童模式（不支持）
```

### 4.3 账号验证

**发出** `msg_account_info`：
```json
{"msgId":"msg_account_info","sender":"phone","data":{"account":["<账号名>"],"account_type":"com.google"}}
```
`account_type`: `com.osp.app.signin`（三星账号）或 `com.google`。无账号时数组为空。

**接收** `msg_account_verify`：
```
data.status: Int        // 0=WATCH_NOT_LOGIN 1=NOT_VERIFIED 2=VERIFIED
data.account_type: String?
data.stored_address: String?   // 上次配对手机的 BT MAC
data.address: String?         // 手表 BR 地址（用于 bond-loss recovery）
```

status==VERIFIED 且 stored_address != 本机 MAC → `onUserVerified`（配对成功）。
status==VERIFIED 且 stored_address == 本机 MAC → bond loss recovery 路径。

### 4.4 换机/重置

**发出** `msg_mode_change_request`：
```json
{"msgId":"msg_mode_change_request","sender":"phone","data":{"category":<0|1|2>,"address":"<本机BT MAC>"}}
```
**接收** `msg_mode_change_response`：`data.result, data.address, data.category`

### 4.5 验证结果（加密路径专用，dataType=2）

`msg_verification_success`：`{"msgId":"msg_verification_success","sender":"phone","data":{"result":<bool>}}`
以及 `msg_feature_exchange`：`{"msgId":"msg_feature_exchange","sender":"phone","data":{"new_phone":true}}`

## 5. Samsung 手表 vs 非 Samsung 流程差异（WatchManager 侧）

1. Samsung 手机：`sendInitialData → sendPublicKey`（dataType=0）→ 手表回公钥 → 发证书链（dataType=1）→ 双向验证 → `msg_verification_success` → `msg_feature_exchange` → …
2. 非三星：`sendInitialData → sendNonSamsungPopupRequest`（dataType=12，明文）→ 弹窗请求 → 状态/账号流程。

注意 `isSamsungDevice()` 的判定与 Spoof：这正是 GalaxyWatchTweaker 已在做的 Build spoof。

## 6. SAK/GAK 配对验证（RFCOMM 通道）

来源：`sak/VerificationManager.java`、`sak/BluetoothInsecureManager.java`、`sak/sakverify/*`、`setupwizard/pairing/PairingFragment.java`

配对入口 `PairingFragment.startCreateBond()`：
- Wear OS 手表 → `VerificationManager.requestSAKVerify(device)`（若 `isSupportSAKVerify()`）
- 否则普通 `createBond()`

`isSupportSAKVerify()` 决策树（**对独立 app 极重要**）：
- **非三星手机**：
  - Android 13+ → **不支持**（"not support for Android T/U OS issue"）→ 走普通 createBond + 非 Samsung 明文路径
  - firstApiLevel < 26 → 不支持
  - Android 8~12 → GAKVerifier（Google attestation）
- **三星手机**：看 `ro.security.keystore.keytype` 属性（含 `sak` → SAK，含 `gak` → GAK）

SAK/GAK 协议（RFCOMM，BUFFER 990 / CHUNK 986）：
1. `createInsecureRfcommSocketToServiceRecord(0bb62597-...)` → connect
2. 帧头 4 字节：`[len(2B)][dataType(1B)][more(1B)]`（注意与 GATT TLV 头不同！）
3. 交换公钥（ECDH，EC P-256）→ 证书链（Android KeyStore attestation 序列化 X509 链）
4. 验证：challenge 比对 + origin==0 (KeyMaster/StrongBox) + 根证书（GAK 用 Google root）
5. 成功后切到 secure RFCOMM `0aa62597-...`，此后数据用 **AES-256-CBC (PKCS5)** 加密，密钥 = ECDH secret 取前 32 字节，IV 取密文前 16 字节（解密侧 `decryptAES256`：`bArr4 = bytes[0..16]` 作为 IV）。

**独立 app 结论**：完全不需要实现 SAK/GAK——在 Android 13+ 非三星手机上官方代码本身就放弃验证，退化为普通 bond + GATT 明文 JSON。

## 7. BLE 广播识别（扫描层）

来源：`scanner/BluetoothLEScanner.java`、`util/ManufacturerUtil.java`、`util/SamsungMFFormat.java`

- 扫描过滤器：**manufacturer ID 117 (0x75)** 与 **29952 (0x7530)**，空掩码（任何内容都算）
- SCAN_MODE_LOW_LATENCY，RSSI 阈值 -70
- Manufacturer Specific Data 解析（SamsungMFFormat）：
  - `MANUFACTURER_DEVICE_ID_WEARABLE_MIN..MAX = 144..255`（wearable 设备 ID 区间）
  - SS-LE 格式：设备类型/图标/状态/account/contactHash/CRC/features
  - 旧格式偏移：serviceId@5, deviceId@7, deviceType@10
- Watch7 Plugin 的 `ConnectionUtil.MANUFACTURE_DATA = {12, -1, 0, 117, 1, 0, 2, 0, 1, 1, -1, 0, 0}`（0x75=117 Samsung 厂商 ID 前缀）

## 8. Watch7 Plugin 架构（数据通道）

W7P **不含自己的 BT 传输实现**。运行时拓扑：

```
[W7P plugin app]
  ├─ Google Wear MessageClient (GMS wearable, paths /wcs_extension/*)  ←→ Play Services datalayer ←→ Watch (Wear OS)
  ├─ Google wear companion library (com.google.android.libraries.wear.companion.*)
  │    └─ CompanionDeviceManager 绑定、bond-loss recovery
  ├─ AIDL: ICompanionServiceInterface ← Galaxy Wearable(WatchManager) 通过
  │    com.samsung.android.app.watchmanager.permission.GALAXY_WEARABLE_PLUGIN 绑定
  └─ DirectMessageWork: RFCOMM server "DirectProfileSecure"
       UUID cee50b63-5f79-4727-8b5d-504eb07e1e22（手机侧 listen，手表主动连）
       明文 JSON: {msgId, sender:"mobile", msgType, data}
       msgId: set_device_activation / connection_feature_exchange / package_update_message /
              set_disconnect_bluetooth_profiles_all / manufacturer_data / cloud_sync_* 等
  AutoSwitchWork: RFCOMM "0E4D4A34-00A0-B708-EAAC-6043A45596ED"（secure/insecure 同 UUID）
```

**GMS Wear message paths（WCS 扩展，JSON payload）**——独立 app 需要实现的核心子集：
- `/wcs_extension/internal/feature` — capability exchange（`feature_exchange`）
- `/wcs_extension/internal/connection` — 连接管理（`/w_module_sec_watch_setting/bluetooth` 子路径 manufacturer data）
- `/wcs_extension/internal/status` — 手表状态/OneNumber
- `/wcs_extension/internal/packages`、`/wcs_extension/internal/p2p`、`/wcs_extension/internal/sim`、`/wcs_extension/internal/reset`
- `/wcs_extension/sysui/notification/bridge`、`.../switch/*`、`.../settings` — 通知桥接
- `/wcs_extension/sysui/apps`、`/sysui/watchface`、`/sysui/widget`、`/sysui/tutorial`、`/sysui/nowbar/*`

**Capability exchange JSON（手机→手表，`/wcs_extension/internal/feature`）**：
```json
{"msgId":"feature_exchange","msgType":2,"deviceNodeID":"<watchId>","btAddr":"<手机BTMAC>",
 "data":{"deviceBtName":"...","screenWidth":"...","screenHeight":"...","deviceName":"...",
   "modelNumber":"SM-S938U","vender":"samsung","isTablet":"false","csc":"XAA",
   "locale":"en_US","timezone":"...","time":"<ms>","sepLiteDevice":"false"}}
```
手表回包字段（`CapabilityExchangeParser`）：modelNumber, androidVersion, softwareVersion, watchBtAddr,
watchWifiAddr, modelName, salesCode, battery*, oneUiVersion, knox*, esimSupport, supportTts, gps, speaker,
sensor*, imei, eid, fwVersion, hwBezel, wearOsVersion, productCode, brandName, manufacturer... （约 80 个字段）

## 9. `isSamsungDevice()` 判定（Spoof 面）

```java
isSamsungDevice() = !isSamsungGEDModel() && "SAMSUNG".equalsIgnoreCase(Build.MANUFACTURER)
isSamsungGEDModel() = Build.MODEL 含 "SM-G900FG" 或 "GT-I9505G"
```
仅看 `Build.MANUFACTURER`。Xposed 一次 hook 即可全局翻转。

## 11. 设备规则表（型号 → 插件映射，`b7/l.java`）

| 手表 | 插件包 | deviceId | 服务 |
|---|---|---|---|
| Galaxy Watch4/4 Classic/FE | `com.samsung.android.waterplugin` | 153 | - |
| Galaxy Watch5/5 Pro | `com.samsung.android.heartplugin` | 154 | - |
| Galaxy Watch6/6 Classic | `com.samsung.wearable.watch6plugin` | 153 | WRCompanionService |
| Galaxy Watch7/7 Pro/Ultra | `com.samsung.wearable.watch7plugin` | 165 | WRCompanionService |
| Galaxy Watch8/8C/9/Ultra2 | `com.samsung.wearable.watchuniteplugin` | 136 | WRCompanionService |
| Galaxy Ring | `com.samsung.android.ringplugin` | 65 | - |
| Gear S2/S3/Sport | `com.samsung.android.gearoplugin` | 74 | - |
| Galaxy Watch3 | `com.samsung.android.gearnplugin` | 93 | - |

（deviceId = 广播 manufacturer data 里的设备 ID，wearable 区间 144–255 之外还有旧型号低值）

## 12. 换机/转移流程（WatchTransferRequesterImpl）

```
start(address) → GATT 连接 → CCC 订阅 → sendInitialData
  → 非三星: msg_popup_request(12) ⇄ msg_popup_response
  → msg_status_request(2) ⇄ msg_status_response
  → msg_account_info(2) ⇄ msg_account_verify
      VERIFIED + stored_addr != 本机 → onUserVerified（弹窗让用户确认）
      → requestToDevice(NEW_PHONE) = msg_mode_change_request{category:1, address:本机MAC}
      → msg_mode_change_response(result) → 移除旧 bond、创建新 bond
```

成功后 `onComplete` → 若失败自动 `removeBond`。

## 13. 独立 App（"任何手机连三星表"）实现路线图

**结论：可行，且比预想简单。** 分层策略：

### 最小可用产品（MVP）——纯 GATT，零依赖
1. BLE 扫描：manufacturer ID `117`/`0x7530`，解析 SamsungMFFormat 得设备型号/ID
2. `connectGatt` → `requestMtu(512)` → 发现服务 → 找 `eed6d5cc...` 服务 + `2fb6a5aa...` 写特征 → 订阅 CCC
3. 走**非三星明文路径**（dataType=12）：`msg_popup_request` → 状态机驱动到 `msg_mode_change_request`
4. Android 系统正常 bond（`createBond`）后手表即认为配对完成
5. 后续基础通信继续走同一 GATT TLV（dataType=2 JSON）

注：Android 13+ 的非三星手机上，三星自己的 App 也走这条路（SAK 验证被官方禁用）。

### 完整功能（通知同步/表盘/应用安装/设置）
需要实现 GMS Wear datalayer 或 DirectMessageWork RFCOMM 通道：
- RFCOMM `cee50b63-5f79-4727-8b5d-504eb07e1e22`（DirectProfileSecure，明文 JSON）
- `/wcs_extension/*` 消息集（能力交换、通知桥、状态、包管理）
- 这部分可在 MVP 验证后逐条消息补充

### 风险/未知
- 手表端是否对 "非 Samsung 手机的 GATT 连接" 有额外限制（需实测）
- `msg_popup_response` 需要用户在手表上点确认（弹窗）
- 换机后手表可能要求重置（若 stored_address 逻辑不满足）
- 完整 WCS 功能依赖 GMS wearable 协议（protobuf over BT），工作量大但非必需

- [ ] ~~Watch7 Plugin 侧反编译~~（已完成：WCS = GMS Wear + AIDL，见 §8）
- [ ] 广播扫描过滤器（已确认：manufacturer ID 117/0x7530，见 §7）
- [ ] SAK 证书链细节（已确认 ECDH P-256 + AES-256-CBC；独立 app 不需要，见 §6）
- [x] 非三星路径 = 明文 dataType 12 JSON（独立 app 的突破口）
- [ ] GMS Wear datalayer 的 BT 传输本身（Play Services 内部 protocol buffer + RFCOMM）——若目标含通知同步/应用安装需深入；仅“配对连接+基本控制”可绕过
- [ ] 实机验证：手机重新连上后抓 HCI snoop log 确认时序

## 14. 剩余待办

- [ ] 实机验证（需重新连接手机）：
  - HCI snoop 抓包确认 GATT 时序与 watch 端响应
  - `adb shell getprop ro.product.first_api_level` 确认本机 GAK 分支
- [ ] GMS Wear datalayer 内部协议（如需完整通知同步）
- [ ] 独立 app MVP 开发（BLE 扫描 → GATT → TLV → JSON 状态机）
- [ ] `SamsungMFFormat` 完整字段解析（型号识别用）

## 15. Wear DataItem / DataMap 编码（RFCOMM tag9 通道）

`WearPath`（inner tag 9）承载 Google Wear 的 DataItem。数值语义已用抓包逐字节核对，
实现见 `watchfree/wcs/WcsFrame.kt` 的 `DataMapDecoder` / `DataMapEncoder`。

### 15.1 帧结构

```
inner f9 {
  1: senderPackage   ("com.samsung.wearable.watch7plugin")
  2: uri             ("wear://<authority>/<path>")
  4: repeated DataItem(DataMap bytes)
  5: seqId           (单调递增)
  6: 0
  7: nodeId
  9: signatureDigest (40 hex → bytes)
  10: timestamp(ms)
}
```

* 手表 → 手机：authority = 手表节点（如 `9940fa14`），f7 = 手表节点，seqId 为手表计数器（数千）。
* 手机 → 手表：authority = **手机节点**（如 `20a3039`），f7 = 手机节点，seqId 必须大于手表已见值
  （实现取毫秒时间戳）。
* 信封 envType：DataItem 走 `3`（DATA_QUEUE），业务消息走 `4`（MESSAGE_QUEUE）。

### 15.2 DataMap proto

```
hqe { 1: repeated hqd }              # DataMap
hqd { 1: string key, 2: hqc }        # Entry
hqc { 1: int32 type, 2: hqb }        # Value
hqb {                                 # 值与 type 字段号一一对应
  1 bytes  2 string  3 double  4 float  5 int64
  6 int32  7 fixed32  8 bool  9 map(DataMap)  10 array(repeated hqc)
}
```

即 **type == hqb 的字段号**：`2=string, 5=int64, 6=int32, 8=bool, 9=DataMap, 10=Array`。
`DataMapEncoder.encode()` 的输出与官方 `putWatchDataItem` 抓包**字节级一致**（有单测锁定）。

### 15.3 设置写入（官方 SettingsCompanion.putWatchDataItem）

```
path  = wear://<phoneNode>/w_module_sec_watch_setting/...
dataMap = { <valueKey>: <value>, "from": "phone", "time": <now int64> }
```

`valueKey` 因设置项而异，且手表上报时会一并给出（`key` 元字段），因此实现直接复用上报值：

| valueKey | 含义 | type |
| --- | --- | --- |
| `booleanIsChecked` / `booleanIsAvailable` | 开关 / 可用性 | 8 |
| `intLevel` | 档位（亮度 0–255、音量） | 6 |
| `intSelectedItem` | 列表选中项 | 6 |
| `intPercent` | 百分比 | 6 |
| `stringName` / `stringAddress` / `stringTime` / `fontNameString` | 字符串 | 2 |
| `array*List` | 列表 | 10 |

手表上报的 `type` 元字段是 Samsung 自己的 ValueType（0=bool 1=int 3=string 5=array），
与 DataMap type 无关，写入时**不需要**带上。

### 15.4 读取设置

* `setting_info`（`/w_module_sec_watch_setting/info/setting_info`）一次给出配置版本 `intVersion`。
* 真正的设置项由手表在连接建立后逐条主动推送（本机实测 91 项），无需逐项请求。
* `requestSettingInfo()` = 发空 `setting_info_req` + `/w_module_sec_watch_setting/feature`。

### 15.5 DeviceCare 指令（Gson JSON over Message）

```
{"msgId":N,"msgType":1,...}   →  手表回 {"msgId":N,"msgType":2,...}
```

| path | msgId | 请求 | 说明 |
| --- | --- | --- | --- |
| `/c_devicecare/battery/psm` | 203 | `extraData: 0/1` | 省电模式开关（实测 1s 内回执） |
| `/c_devicecare/battery/base_info` | 209 | `extraValue: ""` | 电池详情 DataItem(209) |
| `/c_devicecare/battery/refresh` | 202 | `extraValue: ""` | 电池用量 DataItem(202) |
| `/c_devicecare/battery/setting` | 211 | `extraData: [-1,-1,-1]` | 充电保护（本固件返回空） |
| `/c_devicecare/memory/usage` | 301 | — | 内存 DataItem(300) |
| `/c_devicecare/memory/clean` | 303 | `appName: []` | 本固件无回包、无可见效果 |
| `/c_ram/requestRamInfo` | 302 | `extraData: {}` | 旧版 RAM 查询 |

DataItem 的 `dataMapId` 是分流依据：200=电池变化、202=用量、204=耗电异常、205=限制应用、
206=充电保护设置、209=电池详情、300=内存。
### 15.6 通道节流与幂等语义（实测约束）

* **PSM 幂等**：目标状态与当前状态相同 → 手表不回 `msgType=2`。客户端须先判断，
  否则会把「无需变更」误判为超时/失败。
* **节流**：1.5s 内连发多条 DeviceCare 请求（尤其紧接一批设置写入之后）会被静默丢弃。
  推荐单线程串行 + ≥1.5s 间隔，并对关键指令（PSM）做超时补发。

---

## 16. 表盘控制协议（Watchface Protocol）

### 16.1 通信通道

* **Path**: `/wcs_extension/sysui/watchface`
* **Transport**: Wearable Message（Tag 10 inner，type=4 TYPE_RESPONSE，走 GMS MessageClient 路由）
* **手表侧宿主**: `com.samsung.android.wcs.extension`（System priv-app，组件 `WatchfaceApiStub`）

### 16.2 报文格式

请求与响应统一采用 JSON 载荷：

```json
{
  "msgId": "<msgId>",
  "msgType": 0,
  "deviceId": "<watchNode>",
  "reason": 0,
  "result": 0,
  "data": {}
}
```

#### `msgType` 语义定案（实测验证）

| `msgType` | 角色 | 含义 |
| :---: | :---: | :--- |
| `0` | 手机→手表 | **查询请求**（Query Request） |
| `1` | 手机→手表 | **写/设置请求**（Set/Write Request） |
| `3` | 手表→手机 | **执行响应**（Response，带 `result: 0, reason: 0` 及 `data` 详情） |

### 16.3 关键指令集

#### 1. 查询当前活跃表盘（Active Watchface Query）
* **Request**: `msgId = "change_active_watchface"`, `msgType = 0`, `data = {}`
* **Response** (`msgType = 3`):
```json
{
  "msgId": "change_active_watchface",
  "msgType": 3,
  "deviceId": "A0:B0:BD:F8:AB:FD",
  "result": 0,
  "reason": 0,
  "data": {
    "packageName": "com.samsung.android.watch.watchface.ultraanalog",
    "className": "com.samsung.android.watch.watchface.ultraanalog.UltraAnalogWatchFaceService",
    "favoriteId": 1,
    "initialized": true
  }
}
```

#### 2. 查询收藏列表与排序（Watchface Order Query）
* **Request**: `msgId = "watchface_order"`, `msgType = 0`, `data = {}`
* **Response** (`msgType = 3`):
```json
{
  "msgId": "watchface_order",
  "msgType": 3,
  "deviceId": "A0:B0:BD:F8:AB:FD",
  "result": 0,
  "reason": 0,
  "data": {
    "count": 4,
    "favoriteIdOrderList": [5, 1, 4, 3],
    "WatchFaceOrderType": "recent"
  }
}
```

#### 3. 切换当前活跃表盘（Change Active Watchface）
* **Request**: `msgId = "change_active_watchface"`, `msgType = 1`, `data`:
```json
{
  "packageName": "com.samsung.android.watch.watchface.ultrainfoboard",
  "className": "com.samsung.android.watch.watchface.ultrainfoboard.UltraInfoBoardWatchFaceService"
}
```
* **手表行为与回执**:
  1. 手表底层 WallpaperManagerService 立即切换壁纸组件，`dumpsys wallpaper | grep mWallpaperComponent` 确认秒级变更。
  2. 手表回发 `change_active_watchface` (`msgType=3`, `result=0`) 成功回执。
  3. 手表自动将该表盘移至最近使用列表首位，并主动回发 `watchface_order` (`msgType=1` / `msgType=3`) 通知排序变更。
  4. 手表通过 DataItem 通道更新 `/active/data/active_watch_face` 与 `/favorites/data/favorite/<id>/`。

---

## 17. Wear OS GMS 丢帧机制：ReorderingQueue 与 Generation

### 17.1 问题现象
手机与手表断开并重连后，手表 GMS 在底层蓝牙 RFCOMM 能收到全部 `inbound` 帧，但所有消息全部被静默丢弃（没有触发 `receive` 投递给本地系统组件）。

### 17.2 逆向根因（`hay.java` / `hbg.java`）
GMS 的 `ReorderingQueue` 维护了一个消息重排和去重队列：
* 队列 key: `priority:sourceNode`（如 `lo:20a3039`）
* 队列两个关键参数：
  * `generation`（field 10）：代数编号
  * `sequence`（field 1）：消息自增序号
* 队列核心逻辑：
```java
if (this.generation != msg.generation) {
    // 代数变更：重置队列，将期望序列号重置为当前帧的序列号
    reset();
    this.generation = msg.generation;
}
if (msg.sequence < this.expectedSequence) {
    // 过期帧：静默丢弃！
    drop();
} else if (msg.sequence == this.expectedSequence) {
    // 命中期望序号：透传给目标组件，expectedSequence++
    passThrough();
} else {
    // 乱序超前帧：入优先队列等待前面的序列号
    enqueue();
}
```
### 17.3 解决方案
若手机侧在每次连接时将 `sequence` 重置（如从 2 开始），但 `generation` 保持硬编码（如 4），手表端由于 GMS 进程未重启，仍记录着上次会话的最大序列号（如 249），从而将所有新消息作为过期历史帧丢弃。

**正确解法**：在 protobuf Tag 10（`wearableMessage`）的 field 10 中管理 `generation`。每次新连接会话建立（收到 `SyncStart`）时自增 `generation`，触发手表 GMS 重置 `expectedSequence`，确保长连接重连后的首帧及后续消息 100% 立即消费。

## 18. Samsung Health 独立双向同步协议

### 18.1 协议通道与目标组件
* **通信通道**：`/samsung_health/wearable_message`（手表上行可带自增序号后缀，如 `/samsung_health/wearable_message/10114`）
* **手表端服务宿主**：`com.samsung.android.wear.shealth`（主服务 `com.samsung.android.wear.shealth.wearable.syncmanager.WearOsListenerService`）
* **手机端官方宿主**：`com.sec.android.app.shealth`（主服务 `com.samsung.android.app.shealth.wearable.syncmanager`）

### 18.2 GMS OemCompanionVerifier 鉴权白名单机制
手表 GMS 对 `/samsung_health/wearable_message` 通道施加了包名与证书白名单校验（`AppKey` 映射）：
* **发送包名 `senderPkg`**（protobuf field 2）：必须为 `"com.sec.android.app.shealth"`
* **签名指纹 `senderNode`**（protobuf field 3）：必须为官方 Samsung Health 的 SHA-1 证书指纹 `"9741a0f330dc2e8619b76a2597f308c37dbe30a2"`
* 若使用 Galaxy Wearable 插件的签名或包名发送，GMS 判定签名不匹配静默丢弃，手表端健康服务无法被唤醒。

### 18.3 消息格式与双向编解码
传输信封为外层 JSON，内层数据体 `body` 进行 gzip 压缩后再 Base64 编码：
* **外层 JSON 结构**：
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
  "body": "H4sIAAAAAAAA/..."
}
```
* **内层 Body（gunzip 后为 JSON Array）**：
```json
[
  {
    "device_info": [
      {
        "device": "SM-L7050",
        "device_type": 1,
        "version": 4.51,
        "last_sync_time": 1793050474000
      }
    ]
  },
  {
    "message_info": [
      {
        "action": "SYNC_DATA" | "REQUEST_DATA" | "LOG_DATA",
        "result": "SUCCESS",
        "request_module_list": "..."
      }
    ]
  },
  {
    "com.samsung.shealth.tracker.pedometer_step_count": [ ... ],
    "com.samsung.shealth.stress": [ ... ],
    "com.samsung.shealth.tracker.heart_rate": [ ... ]
  }
]
```

### 18.4 同步状态机（三段式握手与游标闭环）
1. **手机发起同步**：手机向手表发送 `REQUEST_DATA`（`message="REQUEST"`, `action="REQUEST_DATA"`）；
2. **手表返回数据包**：手表健康服务验证签名通过，打开本地数据库游标（`Data.HealthDataResolver.getUpdatedDataCursor`），将变更模块打包为 `SYNC_DATA` 发往手机；
3. **手机应答确认**：手机接收后解析数据，并立刻回发 `RESPONSE` + `action="SYNC_DATA"` + `result="SUCCESS"`；
4. **日志记录与游标提交**：手表接收确认后发回 `LOG_DATA`（带健康埋点数据），手机再次确认 `SUCCESS`；
5. **本地游标固化**：手表执行 `Data.HealthDataResolver.insertOrUpdateSync`，更新内部 `_last_modified_time` 完成增量游标同步。


