# Galaxy Watch 全功能协议目录（Galaxy Wearable / Watch7 Plugin / Samsung Health）

来源：3 个正版 APK 全量反编译 + 实机 HCI Snoop 抓包实证
- WatchManager 2.2.72.26092361（tUHM，宿主 App）
- Watch7Plugin 2.2.15.26071051（设备插件，协议主体）
- Samsung Health（`com.sec.android.app.shealth`，健康数据对端）

自动提取的 736 条路径明细见 [`FEATURE-MATRIX.md`](./FEATURE-MATRIX.md)。
传输层与帧格式见 [`LIVE-VERIFICATION.md`](./LIVE-VERIFICATION.md)。
健康协议见 [`HEALTH-PROTOCOL.md`](./HEALTH-PROTOCOL.md)。

---

## 0. 通用消息信封（所有 WCS 功能共用）

```jsonc
// RFCOMM ch8 (UUID 5e8945b0-9525-11e3-a5e2-0800200c9a66) → protobuf 信封 → 明文 JSON
{
  "msgId":   "feature_exchange",   // 功能内唯一的指令名
  "msgType": 2,                    // 见下表
  "deviceId":"9940fa14",           // 对端节点 id（手表 8 位 hex）
  "sender":  "mobile",             // 可选："mobile" / "watch"
  "data":    { ... },              // 业务负载（结构因 msgId 而异）
  "result":  0,                    // 0=成功
  "reason":  0
}
```

### msgType 语义（实测归纳）

| 值 | 方向 | 含义 |
|---|---|---|
| 0 | mobile→watch | 单向通知 / 推送（不等回复） |
| 1 | mobile→watch | 请求（等手表 msgType=3 回应） |
| 2 | mobile→watch | 状态/能力交换（等手表 msgType=4 回应） |
| 3 | watch→mobile | 对 msgType=1 的响应 |
| 4 | watch→mobile | 对 msgType=2 的响应 |

> 例外：`result`/`reason` 仅在响应中出现；部分旧功能用 `msg_id`（下划线）而非 `msgId`。

---

## 1. 会话建立（必须最先执行）

### 1.1 连接时序（实测帧序）

```
手机                                   手表
 │  RFCOMM connect(UUID 5e8945b0…)      │
 │─────────────────────────────────────>│
 │  NodeInfo    (f7: nodeId, deviceName)│  ← Google Wear 节点广播
 │  NodeAlloc   (f8: id, local, remote) │
 │  WearPath    wear://<node>/peers     │  ← 对等发现
 │<─────────────────────────────────────>
 │  /wcs_extension/internal/connection  │
 │    {msgId:"set_device_activation",   │
 │     msgType:1, data:{enable:true}}   │
 │─────────────────────────────────────>│
 │  /wcs_extension/internal/status      │
 │    {msgId:"status_exchange",msgType:2│
 │     data:{locale*,fbeMode,mcc,mnc,   │
 │      pluginVersion,swVersion,SDK…}}  │
 │─────────────────────────────────────>│
 │  /wcs_extension/internal/feature     │
 │    {msgId:"feature_exchange",msgType:2
 │     data:{modelNumber,vender,csc,…}} │
 │─────────────────────────────────────>│
 │<─── feature_exchange (msgType:4) ────│  ← 手表回传全量设备能力
 │<─── status_exchange  (msgType:4) ────│  ← 含 wearingState/电量/eid
 │  Heartbeat (inner f11) 每 ~15s        │
```

### 1.2 `feature_exchange` — 手机侧字段（实测）

```jsonc
{"msgId":"feature_exchange","msgType":2,"deviceId":"<watchNode>","data":{
  "deviceBtName":"A0:B0:BD:F8:AB:FD",   // 手机蓝牙 MAC
  "screenWidth":"1256","screenHeight":"2606",
  "deviceName":"荣耀Magic8 Pro",
  "modelNumber":"SM-S938U",   // ★ Xposed 模块改写点
  "vender":"SAMSUNG",         // ★
  "isTablet":"false","csc":"XAA","locale":"zh_CN",
  "timezone":"Asia/Shanghai","time":"<epoch ms>",
  "sepLiteDevice":"false","defaultAppMessage":"com.hihonor.mms"
}}
```

### 1.3 `feature_exchange` — 手表侧回传字段（实测，极其完整）

```jsonc
{"msgId":"feature_exchange","msgType":4,"deviceId":"A0:B0:BD:F8:AB:FD","data":{
  "device":"projectx2ul","brandName":"samsung",
  "androidVersion":"16","androidSecurityPatchLevel":"2026-07-05",
  "buildId":"BP2A.250325.020","fingerprint":"samsung/projectx2ulzc/…",
  "batteryCapacity":"590.0","batteryRatedCapacity":"578.0",
  "batteryRatedVoltage":"DC 5V","batteryRatedCurrent":"2.0A",
  "batteryLevel":96,
  "actionButton":true,
  "esimSupport":true,"esimLockSettingsSupport":true,
  "eid":"89033023426100000000021945705446",
  "fccCertification":"FCC ID: A3LSML705"
  // … 共 ~80 个 capability 字段
}}
```

### 1.4 `status_exchange` — 手表侧回传

```jsonc
{"msgId":"status_exchange","msgType":4,"data":{
  "countryCode":"CN","wearingState":false,
  "wearos":"6.0","googlePlayServices":"25.17.31 (240300-753339714)",
  "wearCoreServices":"1.23.87.732814568",
  "imei":"…","eid":"…","iccid":"","phoneNumber":"","mcc":"","mnc":"",
  "isFotaDone":2,"isPackageUpdate":false,
  "isReactivationlocked":false,"isWatchfaceUpdate":false,
  "isActiveInfoDataItemSupported":true
}}
```

---

## 2. 通知（最核心功能）

路径：`/wcs_extension/sysui/notification/bridge` 与 `/settings`

| msgId | msgType | 方向 | data |
|---|---|---|---|
| `notification_feature_exchange` | 0 | →表 | `{version:"1.0.0", voip:true, mutePending:true, multiUser:true, phoneActiveStatus:false}` |
| `notification_settings` | 1 | →表 | 见下 |
| `notification_settings` | 0 | →表 | `{}`（拉取当前设置） |
| `notification_allowed_packages` | 1 | →表 | `{list:[{userId:0, packageName:"com.tencent.mm", packageType:"mobile"}, …]}` |
| `notification_allowed_packages` | 0 | →表 | `{}`（拉取） |
| `phone_setting_top_priority_features` | 1 | →表 | `{}` |
| `set_watch_bixby_enable_state` | 4 | ←表 | `{value:false}` |

`notification_settings` 完整字段：
```jsonc
{"alerts_on_watch":0,          // 0=开, 1=关
 "alerts_on_phone":1,
 "notification_indicator":true,
 "notification_aloud":false,
 "read_notification_aloud_v2":0,
 "show_with_details":true,
 "turn_on_screen":false,
 "turn_on_for_new_apps":false}
```

`notification_feature_exchange` 完整字段：
```jsonc
{"version":"1.0.0","enhancedPhoneAlertMute":2,"showDetails":true,
 "showAppsManage":true,"enhancedReadNotificationAloud":true}
```

> **通知本体投递**走的是 Google Wear 的 notification bridge（`NotificationListenerService` → Wear MessageClient）。
> 独立实现时需要在 RFCOMM 上加发通知载荷；抓包中未捕获到单条通知（采集期间无新通知），
> 需要在下一轮抓包时触发一条通知来锁定 `msgId`。

---

## 3. 电量 / 设备维护

| 路径 | msgId | 方向 | 说明 |
|---|---|---|---|
| `/w_stf` | `stf_battery_rsp` | ←表 | `{deviceId, result:true, data:"{\"batteryLevel\":96}"}` |
| `/indicator_battery` | `stop_battery_status_data` | ←表 | 停止电量轮询 |
| `/indicator_battery/get_battery_status_data_rsp` | — | ←表 | 电量响应 |
| `/indicator_battery/battery_chg_ind` | — | ←表 | 充电状态变化 |
| `/batterystate/rpc/batterystate_request` | (proto) | →表 | 电量请求 |
| `/batterystate/data/batterystate` | — | ←表 | 电量数据 |
| `/c_devicecare/battery/base_info` | `209` (int) | ⇄ | `{extraValue:"", msgId:209}` |
| `/c_devicecare/battery/psm` | — | ⇄ | 省电模式 |
| `/c_devicecare/memory/clean` | — | →表 | 清理内存 |
| `/c_battery/requestSetPSM` | — | →表 | 设置省电 |
| `/c_ram/requestRamInfo` | — | →表 | 内存信息 |

---

## 4. 手表设置（225 条路径，最大子系统）

统一前缀 `/w_module_sec_watch_setting/`。模式：`.../xxx` 设值，`.../xxx/list` 取候选，`.../selected_item` 取当前值。

按分组列出关键项：

### 4.1 显示 `display/`
`brightness`、`brightness_max_level`、`always_on_display`(+`/disabled`)、`font_size`(+`_max`)、
`font_style/list`+`selected_item`、`screen_time_out/list`+`selected_item`、`outdoor_mode`、
`touch_sensitivity`、`wake_on_wrist_raised`、`wake_on_touch_screen`、`wake_on_turning_bezel`、
`bold_font`、`auto_low_brightness`、`show_charging_info`、`show_last_app/*`、`auto_launch_media_control`

### 4.2 声音与振动 `sound_settings/`
`volume/{ringtone,media,notifications,system,accessibility,bixby,ai_assistant}`、
`vibration_intensity`(+`/list`)、`vibration_type`(+`/list`)、`vibration_with_sound`、`vibration_feedback`、
`sound_mode`、`during_call`、`prelisten`、`long_vibration`、
`ringtone/{selected_item,selected_name,usertone/list,highlights_only,name/list}`、
`ringtone_vibration/*`、`notification_sound/*`、`notification_vibration/*`、
`system_sounds/{touch_sound,dialing_keypad_tone,samsung_keyboard,charging_sound,unlock_sound,hourly_chime,ticking_sound}`、
`system_vibration/{charging,dialing_keypad,samsung_keyboard}`

### 4.3 电池与就寝 `bedtime/`
`on_off`、`schedule_info`、`watch_schedule_info`、`lifestyle_info`、`sync_bedtime_mode`、
`turn_on_now`、`turn_on_scheduled`；另有 `psm`、`battery_changed`

### 4.4 高级功能 `advanced/`
- `action_button/{list,selected_item,selected_name,selected_health_id,siren/enabled,how_to_start_action}`
- `bixby/enabled`、`digital_assistant/{list,selected_name}`
- `double_press/{list,feature_list,selected_package}`
- `press_and_hold/{list,selected_item,selected_name}`
- `recents_key/{selected_item,selected_name}`
- `gestures/{answer_calls,dismiss_alerts_and_reject_calls,quick_action/*}`
- `labs/quicklaunch/{enabled,support,list_apps,list_features,launch_package,selected_health_id}`
- `media_control/{auto_launch_media_controls,show_ongoing_icon}`
- `status_indicator/{list,selected_item}`
- `disconnection_alert/{selected_item,message_to_gw,dont_get_alarm,sa_place_address}`
- `theater_mode/enabled`、`tutorial`

### 4.5 通用 `general/`
- 输入：`input/{default_keyboard_list,request_default_keyboard_info,selected_default_keyboard,message_suggestion,suggested_replies,default_voice_input_list,request_default_voice_input_list,default_voice_input_selected_position}`
- 键盘：`keyboard_settings/*`（约 25 项：predictive_text、continuous_input、languages_list、download_language、reorder_languages、personal_data_reset…）
- 语言/TTS：`languages/{tts_language_list,tts_selected_language,tts_speech_rate,tts_preferred_engine_list,select_voice,tts_downloadable_language_list}`
- 语音输入：`voice_input/{languages,is_tos_accepted,is_tos_supported,send_voice_as/*}`
- 腕向：`orientation/{selected_wrist,selected_button_position}`
- 快捷回复：`quick_responses/{custom_responses,smart_reply}`
- 触控边框：`touch_bezel`、`touch_bezel_feedback`
- 换机：`transfer_watch/phone_switching_available`
- 重置：`reset`、`settings_restore_req`、`input_settings_restore_req`
- 锁：`is_pattern`、`change_lock_type`

### 4.6 与手机联动 `phone/`
`dnd/{sync_do_not_disturb,sync_do_not_disturb/enabled,dnd_turn_on_now,duration,policy,policy/app_bypass_dnd_list}`

### 4.7 其他顶层
`info/setting_info_req`、`info/setting_info_support_feature`、`info/setting_info_nal`、
`info/application_info`、`info/setting_info/watch_name`、
`securitysetting/wrist_detection`、`bluetooth`、`btinfosync`、
`wificonfigsync`、`phone_ai_agent_sync`、
`create_dataitem_req`/`create_dataitem_res`/`dataitem_value_req`（通用 DataItem 通道）

---

## 5. 无障碍（99 条路径）

前缀 `/w_module_sec_watch_setting_accessibility/`。
`talkback`、`grayscale`、`negative_colors`、`color_correction_enabled`+`selected_color_correction_type`、
`color_lens_{enabled,opacity,color_type_list}`、`high_contrast_fonts`、`bold_font`、
`mono_audio`、`mute_all_sound`、`lr_sound_balance`、`reduce_transparency_and_blur`、`remove_animations`、
`vibration_watch_{enabled,type_list}`、`tap_duration`、`touch_and_hold_delay_list`、`ignore_repeated_touches`、
`cursor_{color,speed}`、`focus_indicator_color_list`、`auto_scanning`、`scanning_speed_list`、
`universal_gestures`（pinch/double_pinch/clench/double_clench/in_and_out/top_left/top_right/bottom_left/bottom_right）、
`triple_press_*`、`two_finger_triple_tap_*`、`open_vision_enhancement`、`voice_assistant`、`tutorial`

---

## 6. 表盘

| 路径 | msgId | 说明 |
|---|---|---|
| `/wcs_extension/sysui/watchface` | `watchface_order` | ⇄ 表盘顺序（实测：`{count:1, favoriteIdOrderList:[1], WatchFaceOrderType:"recent"}`） |
| `/w_module_watchface/msg` | (proto) | 表盘特性协商（gw_wf_feature_version / image_clipper_available / os_version / supportfeature / watchface_bnr_status / watchface_keyboard_emoji） |
| `/w_module_watchface/device_msg` | (proto) | 手表回传同上 + `watchface_phone_status_response` |
| `/w_module_watchface/channel/` + `{id}` | — | 表盘文件传输通道 |
| `/w_module_watchface/channel/customized_resource/{id}` | — | 自定义资源（用户照片表盘） |
| `/w_module_watchface/channel/restore_resource` | — | 资源恢复 |
| `/w_watchface_home/watchface/msg` | `watchface_favorite_file_response` / `watchface_phone_status` | 手表主页表盘交互 |
| `/w_watchface_home/watchface_to_restore` | — | 待恢复表盘 |
| `/watchface/{pkg}/{cls}` | — | 单个表盘安装/下载 |
| `/fallback/data/fallback_watch_face/…` | — | 兜底表盘 |

表盘包格式 = `.watchface`（zip，含 `theme.json` + res）；通过 Flow 通道（RFCOMM ch4/ch7）传大文件。

---

## 7. 小组件 / Tiles / 复杂功能

| 路径 | 说明 |
|---|---|
| `/tiles/capability/home` | Tile 能力协商 |
| `/tiles/data/providers` | Tile provider 列表 |
| `/tiles/data/configs/{node}` | Tile 布局配置（实测手表上报 `{iDT:true,pC:0,pId:0,pR:0,pT:"健康"}`） |
| `/complication_providers/data/provider_app/{pkg}/` | 复杂功能数据（实测 22 个 provider：日历/outlook/联系人/提醒/bixby/天气/世界时钟/shealth…） |
| `/wcs_extension/sysui/widget` | `addable_tile_info` |

---

## 8. 应用管理

`/wcs_extension/sysui/apps` — `apps_order`

手机侧下发（msgType 0）；手表侧回传（msgType 4）完整桌面布局：
```jsonc
{"data":{"apps":[
  {"isFolder":false,"orderNum":0,"items":[{"packageName":"com.samsung.android.dialer","className":"…CallLogListActivity","orderNum":0}]},
  {"isFolder":false,"orderNum":1,"items":[{"packageName":"com.samsung.android.app.contacts","className":"…PeopleActivity","orderNum":0}]},
  {"isFolder":false,"orderNum":2,"items":[{"packageName":"com.samsung.android.wear.shealth",…}]}
]}}
```

`/wcs_extension/internal/packages`、`/wcs_extension/internal/channel/packages/{pkg}` — 应用安装包下发通道。
`/wcs_extension/quickaction/info` — `quick_settings` + `/channel/icon`。

---

## 9. 通话

| 路径 | 说明 |
|---|---|
| `/w_callservice/{answer,reject,dial,disconnect,mute,silence}` | 手表侧动作（接听/拒接/拨号/挂断/静音） |
| `/w_callservice/reject_msg` + `/reject_msg_list` | 拒接短信模板 |
| `/c_callservice/reject_msg_list` / `reject_msg_respond` | 手机侧拒接短信列表 |
| `/c_callservice/notification_setting` | 来电通知设置 |
| `/c_callservice/{info,dialog,voice_mail,silence,mute,phoneaccount}` | 通话状态 |
| `/c_callservice/answer_and_end_call_setting` | 按键接听/挂断 |
| `/c_callservice/cmc_pd_external_call` | CMC（多设备协同）外部通话 |
| `/w_apps_incallui/{primary_caller_info,control_volume,smartcall,scam_guard_state,call_assist}` | 通话 UI |
| `/w_apps_blocked_number` | 黑名单 |
| `/w_apps_callsettings/bnr/{backup_request,backup_file/{id},restore_file/{id},restore_response}` | 通话设置备份恢复 |
| `/w_emergency_dialer` | 紧急号码列表（实测：`{"msgId":"eccList","data":["119:[4]","120:[2]",…]}`） |

---

## 10. 联系人

| 路径 | 说明 |
|---|---|
| `/contacts_mobile/` | 手机→手表入口（实测 msgId：`Push_Update_Key`、`contacts-country-iso-changed-ind`、`contacts-showblockedlogschanged-ind`、`contacts-myprofiledeleted-ind`） |
| `/contacts3/contacts_full_sync_request` | 全量同步请求 |
| `/contacts3/full_sync_contact/{id}` | 单联系人全量 |
| `/contacts3/contact/{id}` | 增量 |
| `/contacts3/info`、`/contacts3/companion_info` | 能力/伴侣信息 |
| `/contacts3/node_migration_done` | 换机完成 |
| `/contacts_sync/rpc/request_companion_sync` | RPC 触发同步 |
| `/contacts_watch` | 手表→手机 |

密钥交换：`{"key":"<base64>","msgId":"Push_Update_Key"}`

---

## 11. 闹钟 / 日历 / 计时 / 世界时钟（80 条）

### 11.1 闹钟
手机侧 `/c_alarm/*`：`item/{id}`、`groupItem/{id}`、`noti_map`、`sort_order`、
`clockpackage_info`、`started`、`stopped`、`muted`、`snoozeChanged`、`del_item`、
`for_watch`、`restore_watch_alarm`、`resp_china`、`resp_korea_japan`

手表侧 `/w_alarm/*`：`{addwear,editwear,deletewear,set,edit,delete,snooze,dismiss}`、
`capability`、`alarmwatch_info`、`noti_set`、`install_calendar`、`resetweardb`、
`req_china`/`req_china_holiday_update`/`req_korea_japan`、`for_clock`

JSON 键（`DataLayerUtil`）：`alarmItem`、`groupEntity`、`AlarmGroupEntity`/`AlarmItemModel`/`AlarmItemWatch`/`AlarmJsonDataModel`

### 11.2 日历
`/c_calendar/*`：`perform_sync`、`request_sync`、`setting_sync`、`events_sync_completed`、
`holiday_sync`、`perform_alarm`、`dismiss_alarm`、`dismiss_group_notification`、
`group_deleted`、`group_member_updates`、`request_group_invitation`、
`calendar_package`、`wearable_manager_version`、`alert_escape`
`/w_calendar/*`：`request_{sync,holiday_sync,launch_calendar,launch_calendar_edit,launch_calendar_main,launch_china_map,launch_email,launch_url}`、`response_group_invitation`、`dismiss_snooze_alert`

### 11.3 计时器 / 秒表
`/c_timer/{for_watch,started,stopped,muted}`、`/w_timer/{capability,for_phone,restart,dismiss,noti_map}`

### 11.4 世界时钟
`/c_worldclock/{clockpackage_info,new_list,weather_data,is_clock_app_installed,manufacturer_data,worldclock_preset_state}`
`/w_worldclock/capability`

---

## 12. 图库 / 截图 / 存储

| 路径 | 说明 |
|---|---|
| `/c_gallery/sender/*` | 手机→手表：`auto_transfer_start`、`manual_transfer_start`、`get_image_list/{albums,images,story}`、`request_observer_media_change`、`request_delete_image_list`、`request_backup`、`request_restore`、`channel_path/{id}`、`transfer_stop`、`file_name`、`welcome_message`、`auto_enable` |
| `/c_gallery/response/*` | 手表→手机：`transfer_start`、`transfer_count`、`transfer_cancel`、`file_index`、`open_page_done`、`delete/album_id` |
| `/w_gallery/request/*` | 手表→手机：`auto_sync`、`auto_sync_cancel`、`welcome_message` |
| `/w_gallery/response/*` + `/w_gallery/sender/*` | 反向 |
| `/w_apps_screen_capture` | 远程截图 |
| `/w_storage/{storage_info,file_list,file_image,thumbnail_list,storage_manage_on_phone}` | 存储管理 |
| `/c_storage/{requestStorageInfo,requestStorageFileList,requestStorageFileImage,requestStorageRemoveFiles,resetData}` | 手机侧存储 |

实测 `auto_transfer_start`：
```jsonc
{"map":{"plugin_version":126071051,"album-ids":[…],"list-filepaths":["check watch support story"],
 "is-samsung-device":false,"story-file-ids":[…],"album-file-ids":[…]},
 "path":"/c_gallery/sender/auto_transfer_start"}
```
注意 `is-samsung-device:false` —— **非三星路径实证**。

---

## 13. 音乐 / 媒体

| 路径 | 说明 |
|---|---|
| `/w_apps_music_transfer/*` | 音乐传输：`message/syncData`、`message/sync_thumbnails`、`requestOperation`、`operationResult`、`path/{id}` |
| `/w_apps_music_setting/*` | 音乐设置备份：`backup`、`restore`、`sync`、`message/*` |
| `/adaptive_media_bridging/rpc/media_update` | 媒体状态更新（protobuf：`{mediaId, …}`） |
| `/adaptive_media_bridging/rpc/watch_request` | 手表请求媒体控制 |
| `/w_media_control/{id}` | 媒体控制通道 |

实测 `watch_request`（protobuf）内含 `mediaId`（`624b7e0a-b554-4ec7-907b-ea3f3af965e8`）+ 播放控制位。

---

## 14. 录音机

`/c_voicerecorder/*`（手机侧）：`get_vr_watch_info`、`transfer_now`、`ready_receive`、`not_ready_receive`、
`receive_file_completed`、`receive_all_file_completed`、`cancel_transfer`、`auto_{always,off,only_charging}`、
`connection_change`、`connect_success`、`phone_wifi_p2p_{available,not_available}`、
`plugin_info`、`grant_storage_permission`、`deny_storage_permission`、
`check_location_permission_on_phone_result`、`manual_sync_check_list_file_result`
`/w_voicerecorder/*`（手表侧）：`get_plugin_info`、`get_setting`、`file_transfer`、`file_name_transfer`、`file_size`、
`manual_sync_list_file`、`all_file_already_copied`、`no_file`、`low_battery`、`pair_timeout`、
`retry_connect`、`socket_exception`、`vr_watch_info`、`watch_wifi_p2p_available`、
`request_show_permission_dialog`、`request_check_location_permission_on_phone`、`open_privacy`、
`change_auto_sync_setting_to_off`、`notice_not_samsung_m4a_file_format`

> 录音文件走 **Wi-Fi P2P**（`/wcs_extension/internal/p2p` 交换 `address`，实测 `12:4e:e4:4d:cc:a2`），非蓝牙。

---

## 15. 应用商店

`/w_apps_galaxy_store/*`：`install_req/res`、`uninstall_req/res`、`install_cancel_req/res`、
`app_status_req/res`、`execute_req/res`、`galaxy_store_mode_req/res`、
`privilege_info_req/res`、`message_delivery_req/res`、`show_on_phone`
`/w_apps_galaxy_store` — 入口

---

## 16. eSIM / 号码同步

| 路径 | 说明 |
|---|---|
| `/watch_esim` | `mgr_esim2ProfileList_req/res`、下载/激活/删除 profile |
| `/watch_esim_ui` | eSIM UI 状态 |
| `/w_simcardmanager` | SIM 卡管理 |
| `/w_cmc` | CMC 多设备协同 |
| `/w_wktksimchange` | KT SIM 变更 |
| `/w_module/numbersync` | 号码同步 |
| `/watch_ns/{info,help}` | NumberSync |

---

## 17. 固件 OTA

`/wfota` — `msgId:"menu"`，`data` 为字符串化 JSON：
```jsonc
{"msgId":"menu","msgType":"4","deviceId":"A0B0BDF8ABFD","data":"{\"menu_operation\":\"get_device_status\"}"}
{"msgId":"menu","msgType":"4","data":"{\"menu_operation\":\"resume_by_bt_connect\",\"bt_address\":\"02:00:00:00:00:00\"}"}
```
`menu_operation` 取值含：`get_device_status`、`resume_by_bt_connect`、`download`、`update`、`cancel` 等。

`/watch_ota_status/rpc/{watch_ota_status_request,watch_ota_status_update}` — OTA 状态
`/w_module_sec_fota_setupwizard/tutorial`

固件包通过 Flow 通道传，校验用 `/wfota` 内的 md5 + 服务器 `/fota` 接口。

---

## 18. 查找设备 / SOS

| 路径 | 说明 |
|---|---|
| `/w_apps_find_my_watch` | 手机找手表（实测 `{"connection_type":0,"msg_id":"fmw-currentstate-req"}`） |
| `/w_apps_find_my_phone` | 手表找手机 |
| `/w_apps_safety_assistance` | SOS（实测 `{"deviceid":"02:00:00:00:00:00","msgid":"sos-getsossettings-req","requestid":60101312}`） |

---

## 19. 提醒 / 计算器

- `/c_reminder/*`、`/w_reminder/*`：提醒同步（`condition_preset_v2`、`smart_switch_backup_v2`）
- `/c_calculator/{get_history,restore_history}`、`/w_calculator/{history_data,no_data}`
  实测 `get_history` payload = `"no_data"`（hex `6e6f5f64617461`）

---

## 20. 键盘 / OOBE

`/w_module_keyboard_{user_data,user_data/userwords,user_contents,user_contents/request_selected_languages,
user_contents/selected_languages,textshortcuts,contents_rich_content}`
`/w_module_sec_setupwizard/oobe_setup/{account_block,complete,font_setting,frp,set_lock_start,set_lock_stop,set_lock_finished}`
`/w_module_sec_setupwizard/oobe_setup_b2b`
`/w_module_sec_systemui/systemui`
`/w_module_sec_watch_setting/oobe_setup/wrist_preference_setting`

---

## 21. 状态推送 STF

`/w_stf` — 通用状态框架（Status Transfer Framework）。
实测：`{"msg_id":"stf_battery_rsp","deviceId":"…","result":true,"data":"{\"batteryLevel\":96}"}`
`data` 是**字符串化 JSON**，需二次解析。

---

## 22. 账号 / 云 / 诊断

`/watch_samsungaccount`、`/watch_samsung_cloud`、`/watch_gms_checkin_status/rpc/*`、
`/watch_self_diagnostics`、`/watch_preloaded_assistant/rpc/watch_preloaded_assistants_request`、
`/watch_talkback/rpc/turn_off_watch_talkback_request`
`/w_module_sec_watch_setting/account/auto_backup` + `/message/auto_backup`

---

## 23. Runestone（上下文/场景）

`/c_runestone/{analytics,config,context}`、`/w_runestone/*`
含 `analytics`（用户行为）、`config`（配置下发）、`context`（场景上下文）。

---

## 24. 天气 / 其他

`/samsung_weather/message/weather_message` — 实测 `{"msgType":"REQUEST","dataType":"weather_message_standalone","value":""}`
`/camera_controller_oobe` — 实测 payload = `"app_disable"`（hex `6170705f64697361626c65`）
`/w_walkietalkie` — 对讲机
`/timezone/rpc`、`/settings/rpc` — Google Wear 基础（protobuf，含 `settings.CURRENT_UTC_OFFSET` 等）

---

## 25. Google Wear 基础服务（protobuf，非 JSON）

`/settings/rpc`、`/timezone/rpc`、`/peers`、`wear://{node}/{path}`

`settings/rpc` 实测（protobuf 解码后语义）：
```proto
{"settings.COMMAND":      {6: <varint>}}          // 6 = ?
{"settings.COMPANION_TIME": {5: {40: <epoch ms>}}} // 伴侣时间同步
{"settings.HOME_TIME":      {5: {40: <epoch ms>}}}
{"settings.CURRENT_UTC_OFFSET": {6: {48: <offset>}}}
{"settings.OFFSET_AFTER_TIME_CHANGE": …}
{"settings.OFFSET_CALC_DATE": …}
{"settings.RAW_UTC_OFFSET": …}
{"settings.TIME_CHANGE_DATE": …}
{"settings.TIME_ZONE": {2: {13: "Asia/Shanghai"}}}
```

`wear://<node>/<path>` 形式表示走 Google Wear 路由（含节点前缀），
区别于 `/path` 直接投递。实测两者混用：
```
wear://9940fa14/w_module_sec_watch_setting/info/setting_info
/wcs_extension/internal/feature
```

---

## 26. 带宽与分块

- RFCOMM 单帧最大约 **1011 字节**（L2CAP MTU），WCS 帧长度前缀支持更大消息但需分片
- 实测最大单消息：`/samsung_health/wearable_message` 5216 b64 字符（约 3.9KB）
- 大文件走 **Flow 通道**（RFCOMM ch4/ch7，UUID `fafbdd20-83f0-4389-addf-917ac9dae5b2`）
- 表盘/固件/录音/Galaxy Store 均用 Flow + 分块 + ack

---

## 27. 实现优先级建议

| 优先级 | 功能 | 复杂度 | 依赖 |
|---|---|---|---|
| **P0** | 会话建立（connection/status/feature exchange） | 低 | 无 |
| **P0** | 电量读取（`/w_stf`、`feature_exchange` 已含 batteryLevel） | 极低 | P0 |
| **P0** | 手表信息（型号/固件/佩戴状态/eSIM/存储） | 低 | P0 |
| **P1** | 通知转发（NotificationListenerService） | 中 | P0 |
| **P1** | 通知设置双向同步 | 低 | P1 |
| **P1** | 查找手表 | 极低 | P0 |
| **P1** | 媒体控制 | 中 | P0 |
| **P2** | 健康数据同步（步数/心率/睡眠/运动） | 中高 | 见 HEALTH-PROTOCOL.md |
| **P2** | 闹钟/计时器同步 | 中 | P0 |
| **P2** | 联系人同步 | 中 | P0 |
| **P2** | 手表设置读写（显示/声音/电池/高级） | 中（量大） | P0 |
| **P3** | 通知/应用列表同步 | 中 | P1 |
| **P3** | 表盘管理 | 高（需 Flow 通道） | P0 |
| **P3** | 图库传输 | 高（Wi-Fi P2P + Flow） | P0 |
| **P4** | 固件 OTA | 很高 | Flow + 服务器 |
| **P4** | 应用商店 / eSIM / 录音 | 很高 | 三星服务端 |
