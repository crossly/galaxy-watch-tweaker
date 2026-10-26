#!/usr/bin/env python3
"""从反编译源码中系统性提取 WCS 功能矩阵：路径 + msgId + 常量枚举"""
import os
import re
import json
import collections

ROOTS = [
    ("watchmanager", "research/decompiled/watchmanager/sources"),
    ("watch7plugin", "research/decompiled/watch7plugin/sources"),
    ("shealth", "research/decompiled/shealth/sources"),
]

# enum pattern: new Foo("A", 0, "/path", bytesVar)
ENUM_RE = re.compile(
    r'new\s+(\w+)\("(\w+)",\s*(\d+),\s*"(/[^"]*)",\s*([A-Za-z_][\w.]*)\)'
)
# static final String NAME = "/path"
CONST_RE = re.compile(
    r'(?:static\s+final\s+String|public\s+static\s+final\s+String)\s+(\w+)\s*=\s*"(/[^"]*)"'
)
# msgId literals: "msgId": "..." or put("msgId", "...")
MSGID_RE = re.compile(r'["\']msgId["\']\s*[,:]\s*["\']([^"\']{2,60})["\']')
MSG_ID_RE = re.compile(r'["\']msg_id["\']\s*[,:]\s*["\']([^"\']{2,60})["\']')
PATH_RE = re.compile(r'"(/(?:w_|wcs_|c_|samsung_health|contacts|settings|timezone|indicator|batterystate|watch|wfota|adaptive|complication|tiles|w_apps|com\.google)[A-Za-z0-9_./{}\-]*)"')


def scan():
    paths = collections.defaultdict(lambda: {"files": set(), "msgIds": set(), "enums": []})
    enum_defs = collections.defaultdict(list)  # class -> [(name, path, payload)]
    for pkg, root in ROOTS:
        if not os.path.isdir(root):
            continue
        for dp, _, fns in os.walk(root):
            for fn in fns:
                if not fn.endswith(".java"):
                    continue
                fp = os.path.join(dp, fn)
                try:
                    txt = open(fp, errors="ignore").read()
                except OSError:
                    continue
                cls = fn[:-5]
                for m in ENUM_RE.finditer(txt):
                    _, name, _, path, payload = m.groups()
                    enum_defs[cls].append((name, path, payload))
                    paths[path]["enums"].append((cls, name, payload))
                    paths[path]["files"].add(fp)
                for m in CONST_RE.finditer(txt):
                    paths[m.group(2)]["files"].add(fp)
                used = PATH_RE.findall(txt)
                ids = set(MSGID_RE.findall(txt)) | set(MSG_ID_RE.findall(txt))
                for p in used:
                    paths[p]["files"].add(fp)
                    paths[p]["msgIds"] |= ids
    return paths, enum_defs


def grp(path):
    """按子系统分组"""
    if path.startswith("/wcs_extension/internal"):
        return "01 核心/连接/握手"
    if path.startswith("/wcs_extension/sysui/notification"):
        return "02 通知"
    if path.startswith("/wcs_extension/sysui/watchface") or path.startswith("/w_module_watchface") or path.startswith("/w_watchface_home") or path.startswith("/watchface"):
        return "03 表盘"
    if path.startswith("/wcs_extension/sysui/widget") or path.startswith("/tiles") or path.startswith("/complication_providers"):
        return "04 小组件/Tiles"
    if path.startswith("/wcs_extension/sysui/apps") or path.startswith("/wcs_extension/internal/packages") or path.startswith("/wcs_extension/internal/channel/packages"):
        return "05 应用管理"
    if path.startswith("/wcs_extension/quickaction"):
        return "06 快捷操作"
    if path.startswith("/wcs_extension/sysui/tutorial") or path.startswith("/wcs_extension/bugreport") or path.startswith("/wcs_extension/internal/bugreport"):
        return "07 教程/诊断"
    if path.startswith("/w_module_sec_watch_setting_accessibility"):
        return "08 设置-无障碍"
    if path.startswith("/w_module_sec_watch_setting"):
        return "09 设置-手表设置"
    if path.startswith("/samsung_health"):
        return "10 三星健康"
    if path.startswith("/c_devicecare") or path.startswith("/c_battery") or path.startswith("/indicator_battery") or path.startswith("/batterystate") or path.startswith("/indicator_buds"):
        return "11 电池/设备维护"
    if path.startswith("/c_callservice") or path.startswith("/w_callservice") or path.startswith("/w_apps_incallui") or path.startswith("/w_apps_blocked_number") or path.startswith("/w_apps_callsettings") or path.startswith("/w_emergency_dialer"):
        return "12 通话"
    if path.startswith("/contacts") or path.startswith("/w_contacts"):
        return "13 联系人"
    if path.startswith("/c_alarm") or path.startswith("/w_alarm") or path.startswith("/c_calendar") or path.startswith("/w_calendar") or path.startswith("/c_timer") or path.startswith("/w_timer") or path.startswith("/c_worldclock") or path.startswith("/w_worldclock"):
        return "14 闹钟/日历/计时/世界时钟"
    if path.startswith("/c_gallery") or path.startswith("/w_gallery") or path.startswith("/w_apps_screen_capture") or path.startswith("/w_storage") or path.startswith("/c_storage"):
        return "15 图库/截图/存储"
    if path.startswith("/w_apps_music") or path.startswith("/adaptive_media_bridging") or path.startswith("/w_media_control"):
        return "16 音乐/媒体"
    if path.startswith("/c_voicerecorder") or path.startswith("/w_voicerecorder"):
        return "17 录音机"
    if path.startswith("/w_apps_galaxy_store"):
        return "18 应用商店"
    if path.startswith("/watch_esim") or path.startswith("/w_simcardmanager") or path.startswith("/w_cmc") or path.startswith("/w_module/numbersync") or path.startswith("/watch_ns"):
        return "19 eSIM/号码同步"
    if path.startswith("/wfota") or path.startswith("/watch_ota_status"):
        return "20 固件 OTA"
    if path.startswith("/w_apps_find_my") or path.startswith("/w_apps_safety_assistance"):
        return "21 查找设备/SOS"
    if path.startswith("/c_reminder") or path.startswith("/w_reminder"):
        return "22 提醒"
    if path.startswith("/c_calculator") or path.startswith("/w_calculator"):
        return "23 计算器"
    if path.startswith("/w_module_keyboard") or path.startswith("/w_module_sec_setupwizard") or path.startswith("/w_module_sec_fota_setupwizard"):
        return "24 键盘/OOBE"
    if path.startswith("/w_stf"):
        return "25 状态推送(STF)"
    if path.startswith("/watch_samsung") or path.startswith("/watch_gms") or path.startswith("/watch_self_diagnostics") or path.startswith("/watch_preloaded") or path.startswith("/watch_talkback"):
        return "26 账号/云/诊断"
    if path.startswith("/c_runestone") or path.startswith("/w_runestone"):
        return "27 Runestone(上下文)"
    if path.startswith("/w_walkietalkie"):
        return "28 对讲机"
    if path.startswith("/settings") or path.startswith("/timezone"):
        return "29 Google Wear 基础服务"
    return "99 其他"


def main():
    paths, enum_defs = scan()
    groups = collections.defaultdict(list)
    for p, info in paths.items():
        groups[grp(p)].append((p, info))
    total_enum = sum(len(v) for v in enum_defs.values())
    print(f"# 提取结果：{len(paths)} 条路径 / {len(groups)} 个子系统 / {total_enum} 个枚举消息定义\n")
    for g in sorted(groups):
        items = sorted(groups[g])
        print(f"## {g}  ({len(items)} 路径)")
        for p, info in items:
            ids = sorted(info["msgIds"])[:6]
            print(f"  {p}")
            if info["enums"]:
                for cls, name, payload in info["enums"][:4]:
                    print(f"      [{cls}] {name}  data={payload[:40]}")
            if ids:
                print(f"      msgIds: {', '.join(ids)}")
        print()
    json.dump({p: {"msgIds": sorted(v["msgIds"]),
                   "enums": [[c, n, d] for c, n, d in v["enums"]],
                   "group": grp(p)}
               for p, v in paths.items()},
              open("research/feature_matrix.json", "w"), ensure_ascii=False, indent=1)
    print("-> research/feature_matrix.json")


if __name__ == "__main__":
    main()
