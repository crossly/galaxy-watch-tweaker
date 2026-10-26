# Samsung Health 手表同步协议（静态逆向 + 实机验证）

对端 APK：`com.sec.android.app.shealth`（337 MB，`research/apks/pulled/com.sec.android.app.shealth/shealth.apk`）
SHA256 `88b0359eaaea6061466df4fdb15754ca8408caf68e1eb796ef58b24374efe312`
反编译产物：`research/decompiled/shealth/`（68 482 个 .java 文件）

承载层：WCS `/samsung_health/*` 路径（RFCOMM ch8，见 `LIVE-VERIFICATION.md`）
本文标注 **[实测]** 的消息来自 `snoop/messages_*.json`，其余为源码静态分析。

---

## 1. 传输信封

```
RFCOMM ch8 → protobuf Envelope(f10 = WearableMessage)
  f6 path    = "/samsung_health/wearable_message"            (手机→手表)
               "/samsung_health/wearable_message/{seq}"      (手表→手机)
  f7 payload = UTF-8 JSON
```

```jsonc
{
  "message": "REQUEST" | "RESPONSE" | "ERROR",
  "type":    "DATA" | "MESSAGE",
  "sender":  "com.samsung.android.app.shealth.wearable.syncmanager",
  "receiver":"com.samsung.android.wear.shealth.wearable.syncmanager",
  "version": 5.03,
  "sequence_num": 46,
  "source_node": "com.sec.android.app.shealth",   // 手机侧固定
  "destination_node": "9940fa14",                  // 手表 nodeId
  "body": "<Base64(gzip(JSON))>"                   // NO_WRAP
}
```

### body 编解码（源码对应）

`wearable/wearablecomm/wearablemessage/WearableMessageLibUtil.java`

```java
String body = Base64.encodeToString(
        gzip(json.getBytes(UTF_8)), Base64.NO_WRAP);
String json = new String(gunzip(Base64.decode(body, Base64.NO_WRAP)), UTF_8);
```

**body 有两种形态**（两者都实测到）：

| type | body 形态 | 例子 |
|---|---|---|
| `DATA` | JSON **数组** | `[{message_info},{device_info},{com.samsung.*:[]}...]` |
| `MESSAGE` | JSON **对象** | `{"message":"Exercising_Status","status":"stopped","version":2000}` |

---

## 2. 序列号分配（实测）

| 方向 | 起始值 | 说明 |
|---|---|---|
| 手机 → 手表 | 42 | 递增 |
| 手表 → 手机 | 10027 | 递增（另一段空间，避免冲突） |

手表回包时**复用请求的 seq**；`/samsung_health/wearable_message/{seq}` 的路径后缀 = seq。

---

## 3. 模块地址全表

源码：`WearableConstants.Message.URI_*`、`WearableMessageLibConstants`

| 模块 | 手机地址 | 手表地址 |
|---|---|---|
| **同步管理** | `com.samsung.android.app.shealth.wearable.syncmanager` | `com.samsung.android.wear.shealth.wearable.syncmanager` |
| **运动** | `com.samsung.mobile.app.shealth.sport` | `com.samsung.tizengear.app.shealth.sport` |
| **计步** | `com.samsung.mobile.app.shealth.pedometer` | `com.samsung.tizengear.app.shealth.pedometer` |
| **计步(tracker)** | `com.samsung.android.app.shealth.tracker.pedometer` | `com.samsung.tizengear.app.shealth.tracker.pedometer` |
| **训练计划** | `com.samsung.android.app.shealth.program.programbasesync` | `com.samsung.tizengear.app.shealth.serviceframework.programsync` |
| 睡眠 | `com.samsung.mobile.app.shealth.sleep` | `com.samsung.tizengear.app.shealth.sleep` |
| 心率 | `com.samsung.mobile.app.shealth.tracker.heartrate` | — |
| 压力 | `com.samsung.mobile.app.shealth.tracker.stress` | — |
| 体重 | `com.samsung.mobile.app.shealth.tracker.weight` | — |
| 饮水 | `com.samsung.mobile.app.shealth.tracker.water` | — |
| 饮食 | `com.samsung.mobile.app.shealth.tracker.food` | — |
| 用药 | `com.samsung.mobile.app.shealth.medication` | `com.samsung.tizengear.app.shealth.medication` |
| 正念 | `com.samsung.mobile.app.shealth.mindfulness` | `com.samsung.tizengear.app.shealth.mindfulness` |
| 冥想 | `com.samsung.mobile.app.shealth.meditation` | `com.samsung.tizengear.app.shealth.meditation` |
| 周期 | `com.samsung.mobile.app.shealth.cycle` | — |
| 听力 | `com.samsung.mobile.app.shealth.hearing` | — |
| 抗氧化 | `com.samsung.mobile.app.shealth.antioxidant` | — |
| 情绪 | `com.samsung.mobile.app.shealth.mood` | — |
| 同伴测量 | `com.samsung.mobile.app.shealth.group.measure` | — |
| 框架 | `com.samsung.android.app.shealth.wearableframework` | `com.samsung.wear.app.shealth.wearableframework` |
| 受限框架 | `com.samsung.android.mobile.app.shealth.wearableframework.restrict` | `com.samsung.android.wear.app.shealth.wearableframework.restrict` |
| 监控 | `com.samsung.android.app.shealth.wearable.monitor` | `com.samsung.tizengear.app.shealth.wearable.monitor` |

`sender`/`receiver` 简写（能力交换用）：`shealth` / `wearable` / `wearable_manager`

---

## 4. `request_module_list` 全枚举（47 个，源码权威）

源码：`wearable/sync/RequestResult.java` → `RequestModule`

| 值 | 模块 | 值 | 模块 |
|---|---|---|---|
| 700000 | `UNKNOWN` | 702000 | `WEARABLE_FRAMEWORK_SCHEDULER` |
| 700100 | `WEARABLE` | 702100 | `DAILY_ACTIVITY` |
| 700101 | `WEARABLE_FRAMEWORK` | 702200 | `SLEEP_COACHING` |
| 700102 | `APPLICATION` | 702300 | `MEDICATION` |
| 700103 | `WEARABLE_FRAMEWORK_FOREGROUND_SCHEDULER` | 702400 | `FOOD` |
| 700104 | `WEARABLE_FRAMEWORK_REPEAT` | 702500 | `VITALITY` |
| 700105 | `WEARABLE_FRAMEWORK_SERVER_PUSH` | 702600 | `AGES` |
| 700200 | `HOME` | 702700 | `MINDFULNESS` |
| 700300 | `PEDOMETER` | 702800 | `VASCULAR_LOAD` |
| 700301 | `PEDOMETER_USER_PRESENT` | 702900 | `ANTIOXIDANT` |
| 700302 | `PEDOMETER_TRACK` | 703000 | `HEARING` |
| 700400 | `EXERCISE` | 703100 | `WEARABLE_SYNC_DATA` |
| 700500 | `HEART_RATE_MONITOR` | 703200 | **`WEARABLE_REQUEST_DATA`** ★ |
| 700600 | `WATER` | 703300 | `VITALS` |
| 700700 | `CAFFEINE` | 703400 | `HEART_HEALTH` |
| 700800 | `WEIGHT` | 703500 | `TRAINING_LOAD` |
| 700900 | `SERVER` | 703600 | `BP_TREND` |
| 701000 | `SOCIAL` | 701100 | `FLOOR` |
| 701200 | `SLEEP` | 701300 | **`WEIGHT_MANAGEMENT`** ★ |
| 701400 | `STRESS` | 701500 | `WEARABLE_SETTINGS` |
| 701600 | `PEDOMETER_RECOMMENDATION` | 701700 | `SMART_MESSAGE_PLATFORM` |
| 701800 | `USER_PROFILE` | 701900 | `CYCLE` |

★ = 实测中出现的值。

---

## 5. 同步状态机

### 5.1 七个阶段（源码：`WearableSyncFlowManager$Companion$SyncStages`）

```
0 REQUEST_DATA_SEND     ← 发起方发出 REQUEST_DATA
1 REQUEST_DATA_RECEIVE  ← 对方回 SUCCESS
2 M_DATA_SYNC_SEND      ← 手机发送 SYNC_DATA（手机数据下行）
3 M_DATA_SYNC_RECEIVE   ← 手表回 SUCCESS
4 W_DATA_SYNC_RECEIVE   ← 接收手表数据（手表上行）
5 W_DATA_SYNC_SEND      ← 回 SUCCESS
6 FINISH
```

### 5.2 实测完整时序

```
手表                                             手机
 │ REQUEST type=DATA seq=10028                      │
 │ action=REQUEST_DATA                              │
 │ device_info{device:"SM-L7050",device_type:0,v:4.51}
 │─────────────────────────────────────────────────>│
 │              RESPONSE type=DATA seq=10028         │
 │              {action:"REQUEST_DATA",result:"SUCCESS"}
 │<─────────────────────────────────────────────────│
 │              REQUEST type=DATA seq=46             │
 │              {action:"SYNC_DATA",                 │
 │               request_module_list:"WEARABLE_REQUEST_DATA"}
 │              device_info{start_time,end_time,is_last_chunk:true}
 │              + 全量数据容器                        │
 │<─────────────────────────────────────────────────│
 │ RESPONSE seq=46 {action:"SYNC_DATA",result:"SUCCESS"}
 │─────────────────────────────────────────────────>│
 │              REQUEST type=DATA seq=48             │
 │              {action:"SYNC_DATA",request_module_list:"WEIGHT_MANAGEMENT"}
 │<─────────────────────────────────────────────────│
 │ RESPONSE seq=48 {action:"SYNC_DATA",result:"SUCCESS"}
 │─────────────────────────────────────────────────>│
 │              REQUEST type=DATA seq=?  {action:"LOG_DATA"}
 │<─────────────────────────────────────────────────│
 │ RESPONSE seq=10030 {action:"LOG_DATA",result:"SUCCESS"}
 │─────────────────────────────────────────────────>│
```

### 5.3 `action` 全枚举

```
REQUEST_DATA            // 请求同步
SYNC_DATA               // 数据同步（带 request_module_list）
LOG_DATA                // 上报日志
UPDATE_DATABASE         // 更新本地库
DELETE_DATA_DIALOG      // 删除确认
REQUEST_FILE            // 文件传输
RESPONSE / RESPONSE_COMPLETED / RESPONSE_DENIED / RESPONSE_ERROR
RESPONSE_CHALLENGE_INVITATION
```

### 5.4 `result` / 错误码（源码：`ResultCode`）

```
SUCCESS
FAIL
THROTTLE  SYNC_THROTTLED  SCHEDULER_STARTED  SCHEDULER_WORKING
SYNC_START  SYNCING  SYNCING_WILL_START
ERROR_CONNECTION_FAIL  ERROR_UNKNOWN
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

`SyncRequestResultListener.ResultCode`（更细）：
`NOT_DEFINE` `SUCCESS` `FAIL` `THROTTLE`

### 5.5 `request_module_list` 校验失败值

`ERROR_MODULE` / `NOT_ALLOWED_MODULE` / `UNKNOWN_MODULE` / `VERSION_MODULE`
/ `EXCEPTION_ILLEGAL_REQUEST_MODULE`

---

## 6. `device_info` 结构

```jsonc
{"device": "SM-L7050",     // 手表型号；手机侧为 "BKQ-AN90"
 "device_type": 10062,     // 0 = 通用；见下表
 "version": 4.51,          // 能力协议版本
 "last_sync_time": 1792801884315,
 "start_time": 1792795903095,      // 本次同步窗口起点
 "end_time": 1792801884315,
 "is_last_chunk": true}            // 分块传输时最后一块
```

### device_type 取值（源码：`WearableDeviceCapability.r()`）

| 值 | 设备 | 能力协议版本 |
|---|---|---|
| 10019 | Gear Fit | 1.01 |
| 10020 | Gear 1 | 1.01 |
| 10022 | Gear 2 | 2.01 |
| 10023 | 早期 Activity Tracker | 1.01 |
| 10024 | Tizen Gear 1 | 2.01 |
| 10030 | Gear S | 3.01 |
| **10062** | **Wear OS 手表（Watch4+）** | 4.51 |
| 0 | 未分类 / 手机侧默认 | — |

> 源码中 `if (i > 10030) { "Device type is invalid"; return; }` 是针对 Tizen 分支的
> 旧校验；Wear OS 走的是 `query_info` 新路径（见 §10）。

---

## 7. 数据类型全景

源码扫描得到 **128 个顶层数据类型**、355 个字段键。完整清单见
`research/health-types.txt`。按域分组（节选）：

### 7.1 基础体征

| 类型键 | 含义 | 字段 |
|---|---|---|
| `com.samsung.health.step_count` | 步数 | `count` `calorie` `distance` `speed` `sample_position_type` `start_time` `end_time` `datauuid` `deviceuuid` `pkg_name` |
| `com.samsung.health.heart_rate` | 心率 | `heart_rate` `min` `max` `heart_beat_count` `binning_data` `start_time` `end_time` |
| `com.samsung.health.oxygen_saturation` | 血氧 | `spo2` `min` `max` `low_duration` `heart_rate` `binning` `raw` |
| `com.samsung.health.stress` / `com.samsung.shealth.stress` | 压力 | `histogram` `base_histogram` |
| `com.samsung.health.body_temperature` | 体温 | `temperature` `comment` `pkg_name` |
| `com.samsung.health.skin_temperature` | 皮温 | — |
| `com.samsung.health.respiratory_rate` | 呼吸率 | — |
| `com.samsung.health.movement` | 体动 | — |
| `com.samsung.health.blood_pressure` | 血压 | — |
| `com.samsung.health.blood_glucose` | 血糖 | — |
| `com.samsung.health.hrv` | HRV | — |
| `com.samsung.health.ecg` / `electrocardiogram` | 心电 | — |
| `com.samsung.health.sleep_apnea` | 睡眠呼吸暂停 | — |

### 7.2 运动与睡眠

| 类型键 | 含义 | 字段 |
|---|---|---|
| `com.samsung.health.exercise` | 单次运动 | 40 字段，见下 |
| `com.samsung.shealth.exercise` | 运动扩展 | `route` `photo` `hr_zone` `weather` `routine` `program` `pacesetter` `live_data` `location_data` `firstbeat_coaching_*` |
| `com.samsung.health.sleep` | 睡眠 | `start_time` `end_time` `custom` `modify_sh_ver` `comment` |
| `com.samsung.health.sleep_stage` | 睡眠分期 | — |
| `com.samsung.shealth.sleep` / `daily_sleep_v` / `sleep_combined` | 睡眠汇总 | — |
| `com.samsung.shealth.sleep_raw_data` / `sleep_snoring` | 睡眠原始数据/鼾声 | — |
| `com.samsung.health.floors_climbed` | 爬楼 | — |

`com.samsung.health.exercise` 完整字段：
```
start_time, end_time, duration, calorie, distance, count, count_type, exercise_type,
mean_heart_rate, min_heart_rate, max_heart_rate, mean_speed, max_speed,
mean_cadence, max_cadence, mean_power, max_power, mean_rpm, max_rpm,
mean_caloricburn_rate, max_caloricburn_rate, vo2_max, sweat_loss,
altitude_gain, altitude_loss, incline_distance, decline_distance,
min_altitude, max_altitude, auxiliary_devices, live_data, location_data,
additional, comment, create_time, update_time, time_offset,
datauuid, deviceuuid, pkg_name
```

### 7.3 身体成分与代谢

`com.samsung.health.weight`、`body_fat`、`body_muscle`、`height`、
`hip_circum`、`waist_circum`、`user_profile`、`device_profile`、
`com.samsung.shealth.body_composition_goal`

### 7.4 活动与卡路里

`com.samsung.shealth.calories_burned`（`active_calorie` `rest_calorie` `tef_calorie` `active_time` `day_time` `details`）、
`activity_level`、`activity.daily_goal`、`stand_day_summary`、`step_daily_trend`、
`com.samsung.shealth.floor_goal`、`caloric_balance_goal`

### 7.5 饮食／水分／药

`com.samsung.health.food_intake`、`food_info`、`nutrition`、`water_intake`、`caffeine_intake`、
`com.samsung.shealth.food_dish` / `food_favorite` / `food_frequent` / `food_image` / `food_goal`、
`com.samsung.shealth.medication`（`log` `schedule` `coupon` `image`）、`medication_info_v`、`medication_count_v`

### 7.6 女性健康

`com.samsung.health.cycle.{mood,flow,note,symptom,factor,cervical_mucus,lh_test,sexual_activity,profile}`、
`com.samsung.shealth.cycle.{daily_temperature,preferred}`

### 7.7 血液生化（需 Samsung Health Monitor / 实验室数据）

`albumin` `alp` `alt` `ast` `amylase` `bun` `cpk` `creatinine` `ggt` `globulin`
`hba1c` `hdlc` `ldlc` `ldh` `total_bilirubin` `direct_bilirubin` `total_cholesterol`
`total_protein` `triglyceride` `uph` `usg` `homocysteine`
`advanced_glycation_endproduct` `antioxidant`

### 7.8 心肺功能

`fitnessindex`（`test`）、`fev1`、`fvc`、`hrm`、`ihrn`、
`com.samsung.shealth.physical_fitness_score`（`mission_workout`）、
`heart_health_score`、`vascular_load_features`、`vascular_load_raw_data`、
`com.samsung.shealth.mean_arterial_pressure`、`blood_pressure_trends`、
`com.samsung.shealth.lvef_result`、`training_load`（`daily` `goal` `hourly`）

### 7.9 汇聚/统计视图（`_v` 后缀）

`daily_sleep_v` `sleep_detail_v` `medication_count_v` `medication_info_v`
`calorie_intake_daily_summary_v` `sleep_combined` `step_daily_trend`

### 7.10 平台/元数据

`com.samsung.shealth.{permission,preferences,service_preferences,wearable_settings,
health_analytics,goal,goal_history,goal_type,badge,best_records,report,rewards,
shm_device,health_document,insight_message}`

---

## 8. 实测数据样例

### 8.1 计步日汇总

```jsonc
{"datauuid":"6f748919-b265-4c6b-b820-2688d779cf0a",
 "source_info":"H4sIAAAAAAAA/6tWynVJLctMTg0tzUxRslJyNDPyCA8tdHcqVdJRyg3OLy1KTvXMS8v3ySwuUbKKrlYqBgs555fmAfmGOkrFJakFEGXuRfmlBZ5AQwxqY2sBJmpORlgAAAA=",
 "run_step_count":147, "walk_step_count":333,
 "update_time":1792801220038, "healthy_step":0, "recommendation":6000,
 "create_sh_ver":70100530, "distance":507.39752197265625,
 "pkg_name":"com.sec.android.app.shealth", "deviceuuid":"VfS0qUERdZ"}
```

- `source_info` 也是 **Base64(gzip(JSON))**（设备/应用来源元数据）
- `create_sh_ver` = Samsung Health 版本 × 100000（70100530 → 701.0.0530）
- `distance` 单位米

### 8.2 分桶步数（`pedometer` 模块）

```jsonc
{"binning_data":[
  {"index":0,  "steps":1,  "run_steps":0,  "walk_steps":1,  "calorie":0.0372, "distance":0.704},
  {"index":3,  "steps":14, "run_steps":10, "walk_steps":4,  "calorie":1.3154, "distance":19.956},
  {"index":445,"steps":4,  "run_steps":0,  "walk_steps":4,  "calorie":0.2224, "distance":2.766}
]}
```

`index` = 当天第 N 个 **10 分钟桶**（0–143）。

### 8.3 运动状态（`sport` 模块）

```jsonc
// 手机 → 手表
{"status":"stopped","message":"Exercising_Status","version":2000}
// 手表 → 手机（多 status_detail）
{"message":"Exercising_Status","status":"stopped","status_detail":"able_to_start","version":2000}
```

### 8.4 训练计划（`programbasesync`）

```jsonc
{"message":"PROGRAM_SYNC","program":[],"schedule":[],"session":[],"version":2000}
```

### 8.5 卡路里明细

```jsonc
{"active_calorie":40.92461395263672,"modify_sh_ver":70100530, …}
```

---

## 9. 能力交换 `/samsung_health/capability`

源码：`wearable/capability/WearableDeviceCapability.java`（24 个字段，数组 `B`）

```
status_sync_capability_protocol_version   shealth_version
model_name                                model_number
device_id                                 device_type
device_manufacturer                       device_feature
binning                                   gear_health_version
protocol_feature                          2way_data
compression                               device_category
sync_address                              wearable_tile
config                                    data_sync_support
wearable_message                          bluetooth_name
capability_type                           wearable_health_version
request_data_manifest                     ble_profiles
```

握手 JSON（手机侧构造）：

```jsonc
{"device_manufacturer":"Samsung",
 "device_id":"<蓝牙名>",
 "model_number":"SM-L7050",
 "device_type":10062,
 "device_feature":"…",
 "wearable_health_version":"…",
 "status_sync_capability_protocol_version":4.51,
 "model_name":"Galaxy Watch Ultra",
 "data_sync_support":true,
 "device_category":<int>}
```

`capability_type`：
- `com.samsung.shealth.REQUEST_CAPABILITY`
- `com.samsung.shealth.RESPONSE_CAPABILITY`

**版本上限**（源码 `t()`）：
```java
this.s = Math.min(4.51, status_sync_capability_protocol_version);
this.t = Math.min(5.03, wearable_message_version);
```

---

## 10. `request_data_manifest` — 增量同步清单（重要）

源码：`lqa0.java`（`WearableCommonMarkingTableHandler`）、`usa0.java`

```jsonc
{
  "<data_type>": {
    "query_info": {
      "type": 1,        // 1 或 3
      "duration": 2190, // 天/小时窗口
      "count": <int>
    }
  },
  ...
}
```

- `type` 只允许 `1` 或 `3`
- 有 `query_info` → 增量表（返回值 1000）；无 → 全量表（返回 1）
- 若手表报告了 `request_data_manifest`，则同步时长上限取 **2190**（源码 `fta0.java:230`），
  否则读 `protocol_feature.sync_duration`

---

## 11. 实现路线

### 11.1 只读手表数据（**不依赖 Samsung Health**）

最小的可行路径（实测已验证手表会主动发起）：

```
1. 手表发出  REQUEST  type=DATA  action=REQUEST_DATA  seq=10028
2. 我们回     RESPONSE type=DATA  action=REQUEST_DATA  result=SUCCESS
3. 后续手表会持续发送 SYNC_DATA / MESSAGE
4. 我们逐条回 RESPONSE ... result=SUCCESS
```

关键：`sender`/`receiver` 直接**回填请求里的值**（互换），`sequence_num` 用请求的值，
`source_node`/`destination_node` 互换，`device_info.device` 填本机型号。

`WatchRepository.respondHealth()` 已按此实现（`watchfree/.../WatchRepository.kt`）。

### 11.2 双向同步

额外需要：
- 手机侧发 `SYNC_DATA` + `request_module_list`
- 按 §5.1 的 7 个阶段推进
- 大包按 `is_last_chunk` 分块

### 11.3 落地到 Health Connect

| 手表类型 | Health Connect |
|---|---|
| `health.step_count` / `tracker.pedometer_day_summary` | `StepsRecord` |
| `health.heart_rate` | `HeartRateRecord` |
| `health.sleep` / `sleep_stage` | `SleepSessionRecord` |
| `health.exercise` | `ExerciseSessionRecord` |
| `health.oxygen_saturation` | `OxygenSaturationRecord` |
| `health.body_temperature` | `BodyTemperatureRecord` |
| `health.weight` / `body_fat` | `WeightRecord` / `BodyFatRecord` |
| `health.respiratory_rate` | `RespiratoryRateRecord` |
| `health.blood_pressure` | `BloodPressureRecord` |
| `health.blood_glucose` | `BloodGlucoseRecord` |

---

## 12. 待补充（需要设备或更多静态挖掘）

- [ ] `capability` 握手完整抓包（本次采集时手表已配对，跳过了该步）
- [ ] `com.samsung.shealth.sleep` / `heart_rate` 的实测样本（需触发一次测量）
- [ ] 分块大小与 `is_last_chunk` 阈值（`sync/m.java` 方法体过大，jadx 未导出；
      建议改用 Ghidra/JEB 或 `jadx --no-inline-methods` 重试）
- [ ] `request_data_manifest` 中各数据类型的 `duration`/`count` 默认值
- [ ] `sync_address` 字段语义（疑似 WebSocket/relay 地址，非蓝牙路径）
