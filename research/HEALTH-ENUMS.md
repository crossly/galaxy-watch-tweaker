# Samsung Health 协议枚举全集（静态提取）

来源：`com.sec.android.app.shealth`（337 MB）反编译源码 + jadx fallback 反汇编
提取方式：`jadx --single-class <FQN> -m fallback`（规避 jadx 方法体过大跳过）

---

## 1. `MessageInfoType` — body 内消息类型

`wearable/base/WearableInternalConstants$MessageInfoType`

```
DEFAULT
UNKNOWN_FLOW
CHECK_STATUS
REQUEST_DATA               ← 请求同步（实测 seq 10028）
REQUEST_OF_WEARABLE        ← 手表发起请求
SYNC_DATA                  ← 数据同步（实测 seq 46 / 48）
SYNC_DATA_FOR_ONE_WAY      ← 单向同步（会附加 request_module_list）
LOG_DATA                   ← 日志上报
```

对应 JSON：`{"message_info":[{"action":"…"}]}` 里的 `action`。

---

## 2. `RequestModule` — `request_module_list` 取值（47 项）

`wearable/sync/RequestResult.java`，完整表见 `health-modules.txt`。

关键值：

| 值 | 名称 | 说明 |
|---|---|---|
| 703200 | `WEARABLE_REQUEST_DATA` | 请求手表全量数据（实测使用） |
| 701300 | `WEIGHT_MANAGEMENT` | 体重管理（实测使用） |
| 700300 | `PEDOMETER` | 计步 |
| 700400 | `EXERCISE` | 运动 |
| 700500 | `HEART_RATE_MONITOR` | 心率 |
| 701200 | `SLEEP` | 睡眠 |
| 701400 | `STRESS` | 压力 |
| 701900 | `CYCLE` | 周期 |
| 702300 | `MEDICATION` | 用药 |
| 702500 | `VITALITY` | 活力指数 |
| 702900 | `ANTIOXIDANT` | 抗氧化指数 |
| 703300 | `VITALS` | 生命体征汇总 |
| 703400 | `HEART_HEALTH` | 心脏健康 |
| 703500 | `TRAINING_LOAD` | 训练负荷 |
| 703600 | `BP_TREND` | 血压趋势 |

---

## 3. `ResultCode` / `FinishCode` — 结果与错误

### 3.1 `RequestResult.ResultCode`（响应里的顶层结果）

```
SUCCESS            FAIL              THROTTLE
SYNC_START         SYNCING           SYNCING_WILL_START
SYNC_THROTTLED     SCHEDULER_STARTED SCHEDULER_WORKING
ERROR_CONNECTION_FAIL                ERROR_UNKNOWN
EXCEPTION_OOBE_NEEDED
EXCEPTION_DEVICE_IS_NULL
EXCEPTION_DEVICE_POWER_SAVING_MODE
EXCEPTION_ILLEGAL_REQUEST_MODULE
EXCEPTION_ILLEGAL_DEVICE
EXCEPTION_ILLEGAL_NODE
EXCEPTION_ILLEGAL_STATE
EXCEPTION_ILLEGAL_SIGNATURE
EXCEPTION_UNKNOWN
```

### 3.2 `WearableSyncUtilForBackward$FinishCode`（内部终止原因，37 项）

```
SUCCESS                    ALREADY_SEND_COMPLETE      DATA_NULL
EXCEPTION_CAPABILITY_NOT_SUPPORT   EXCEPTION_CONNECT
EXCEPTION_DATE_NULL                EXCEPTION_DEVICE_NULL
EXCEPTION_HANDLER_NULL             EXCEPTION_ILLEGAL
EXCEPTION_INSTANCE_NULL            EXCEPTION_INVALID_PARAM
EXCEPTION_MARKING_TABLE            EXCEPTION_NETWORK
EXCEPTION_NO_RESPONS               EXCEPTION_OOBE
EXCEPTION_PARSING                  EXCEPTION_PEER_REJECT
EXCEPTION_PROTOCOL_NOT_SUPPORT     EXCEPTION_QUERY_JSON
EXCEPTION_QUERY_MAINSYNC           EXCEPTION_QUERY_SCORE
EXCEPTION_QUERY_TABLE_NULL         EXCEPTION_QUERY_TIME
EXCEPTION_RECEIVEBODY_NULL         EXCEPTION_REMOTE
EXCEPTION_RESPONSE_UNKNOWN         EXCEPTION_SECURITY
EXCEPTION_SEQUENCE_NUM             EXCEPTION_SOCKET_FAIL
EXCEPTION_TIME_OUT                 EXCEPTION_UNBIND
EXCEPTION_UNKNOWN                  EXCEPTION_WEARABLE_PROCESS_ERROR
SEND_PACE_SETTER_FAIL              SEND_PACE_SETTER_SUCCESS
```

> `EXCEPTION_SEQUENCE_NUM` 说明 **`sequence_num` 会被校验**——乱序会被拒。
> `EXCEPTION_SECURITY` / `EXCEPTION_ILLEGAL_SIGNATURE` 说明存在签名校验分支。

### 3.3 `SyncRequestResultListener.ResultCode`

```
NOT_DEFINE   SUCCESS   FAIL   THROTTLE
```

---

## 4. `SyncStages` — 同步流程阶段

`wearable/sync/WearableSyncFlowManager$Companion$SyncStages`

```
0 REQUEST_DATA_SEND
1 REQUEST_DATA_RECEIVE
2 M_DATA_SYNC_SEND          (M = Mobile)
3 M_DATA_SYNC_RECEIVE
4 W_DATA_SYNC_RECEIVE       (W = Wearable)
5 W_DATA_SYNC_SEND
6 FINISH
```

## 4.1 `SyncRequestType`

```
TWO_WAY       双向同步
ONE_WAY       单向（会走 SYNC_DATA_FOR_ONE_WAY 分支）
INVALID_VALUE
```

## 4.2 `SyncType` / `SyncStatus` / `DataStatus`

```
SyncType:   UNKNOWN  WEARABLE  SHEALTH  DIRECT_WEARABLE  DAY4_SYNC
SyncStatus: IDLE  SYNCING
DataStatus: WAITING  DATA_QUERYING  DATA_INSERTING  DATA_TRANSLATING
            WAITING_RESPONSE  DATA_OPERATION_FINISHED
```

---

## 5. `ShealthRowDataInfo` — 数据行级流标签（17 项）

`wearable/util/WearableSyncInformation$ShealthRowDataInfo`

```
QUERY_DATA                     INSERT_DATA
SEND_DATA_TO_W_MESSAGE         RECEIVE_DATA_FROM_W_MESSAGE
RECEIVE_QUERY_RESULT           RECEIVE_INSERT_RESULT
RECEIVE_SYNC_RESULT
REQUEST_SHEALTH_DATA
SEND_PRESSED / RECEIVE_PRESSED
SEND_UNPRESSED / RECEIVE_UNPRESSED
SHEALTH / WEARABLE / W_MANAGER / GMT / NONE
```

`SEND_PRESSED` / `SEND_UNPRESSED` = 同步期间的"按键/佩戴"中断占位，说明同步支持打断。

---

## 6. 连接与设备状态

```
CpConnectionStatus:  CONNECTED  DISCONNECTED  UNPAIRED
NodeListSelectValue: WEARABLE_MESSAGE_SUPPORT  DATA_SYNC_SUPPORT
                     NODE_LIST_WITH_CATEGORY   INVALID_VALUE
```

## 6.1 `SupportManager`（被支持的宿主管理器）

```
GEAR_MANAGER  GEARFIT_MANAGER  GEARFIT_MANAGER_OLD
RISE_GEAR_MANAGER  AT_MANAGER  CHARM_MANAGER
```

内嵌的 base64 常量解码后：

| base64 | 解码 |
|---|---|
| `Y29tLnNhbXN1bmcuYW5kcm9pZC5hcHAuc2NoYXJt` | `com.samsung.android.app.scharm` |
| `Y29tLnNhbXN1bmcuYW5kcm9pZC5hcHAud2F0Y2htYW5hZ2Vy` | `com.samsung.android.app.watchmanager` |
| `Y29udGVudDovL2NvbS5zYW1zdW5nLmFuZHJvaWQuYXBwLnNjaGFybS5oZWFsdGguU0hlYWx0aFByb3ZpZGVy` | `content://com.samsung.android.app.scharm.health.SHealthProvider` |
| `Y29udGVudDovL2NvbS5zYW1zdW5nLmFuZHJvaWQud21zL3NldHRpbmdz` | `content://com.samsung.android.wms/settings` |

> 这就是 Samsung Health 判断"谁在管理手表"的方式：**读 `scharm` / `watchmanager` 的 ContentProvider**。
> 独立 App 若要被识别为"管理器"，需要提供同名 ContentProvider 或走 WCS 直连。

---

## 7. `request_module_list` 的注入规则（静态确认）

`sync/m.java → addRequestModuleList()`

```java
// 仅这些 device_type 需要把 request_module_list 写进 device_info / message_info
Set<Integer> NEW_DEVICE_TYPES = {10032, 10035, 10038, 10040, 10041};

if (messageInfoType == SYNC_DATA_FOR_ONE_WAY && moduleList != null && !moduleList.isEmpty()
        && NEW_DEVICE_TYPES.contains(device.getDeviceType())) {
    device_info[0].put("request_module_list", moduleList);
    message_info[0].put("request_module_list", moduleList);
}
```

**两处都要写**（`device_info` 和 `message_info`），这是实测之外的补充规则。

---

## 8. 数据操作状态 `DataOperationStatus`

```
STATUS_SUCCESSFUL  STATUS_INSERTED  STATUS_UPDATED
STATUS_FAILED      STATUS_CANCELED
STATUS_OUT_OF_SPACE  STATUS_INVALID_INPUT_DATA  STATUS_UNKNOWN
```

---

## 9. 关键数值常量

| 常量 | 值 | 出处 |
|---|---|---|
| 能力协议版本上限 | `4.51` | `WearableDeviceCapability.t()` |
| 消息协议版本上限 | `5.03` | 同上 |
| 有 manifest 时的同步窗口 | `2190` | `fta0.java:230` |
| body 内模块协议版本 | `2000` | 实测 `sport` / `programbasesync` |
| 手机侧起始 seq | `42` | 实测 |
| 手表侧起始 seq | `10027` | 实测 |
| 步数分桶粒度 | 10 分钟 / 144 桶每天 | 实测 `binning_data.index` |

---

## 10. 单位枚举 `JWearableConstants`

```java
// 距离
KILLOMETERS = 170001   MILES = 170003   YARDS = 170002   NOT_DEFINED = -1
// 性别
FEMALE = 190006        MALE = 190005
// 长度
CM = 150001            FEET = 150002
// 重量
KILOGRAM = 130001      LBS = 130002
```
