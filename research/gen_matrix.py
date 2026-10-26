#!/usr/bin/env python3
"""生成完整功能矩阵：路径 → 常量名 / JSON 键 / 所属文件 / 子系统"""
import os
import re
import json
import collections

ROOTS = [
    ("watchmanager", "research/decompiled/watchmanager/sources"),
    ("watch7plugin", "research/decompiled/watch7plugin/sources"),
    ("shealth", "research/decompiled/shealth/sources"),
]

PATH_RE = re.compile(r'"(/(?:w_|wcs_|c_|samsung_health|contacts|settings|timezone|indicator|batterystate|watch|wfota|adaptive|complication|tiles|w_apps|com\.google|peers|w_module)[A-Za-z0-9_./{}\-]*)"')
CONST_RE = re.compile(r'(?:static\s+final\s+String|public\s+static\s+final\s+String|String)\s+(\w+)\s*=\s*("(?:/w_|/wcs_|/c_|/samsung_health|/watch|/w_apps|/settings|/timezone|/indicator|/batterystate|/adaptive|/complication|/tiles|/contacts|/wfota|/peers)[A-Za-z0-9_./{}\-]*")')
JSONKEY_RE = re.compile(r'(?:static\s+final\s+String|public\s+static\s+final\s+String)\s+(\w*(?:KEY|Key|_key|_KEY|NAME|ID|_ID|Id)\w*)\s*=\s*"([A-Za-z0-9_\-]{1,40})"')
ENUM_RE = re.compile(r'new\s+(\w+)\("(\w+)",\s*\d+,\s*"(/[^"]*)",\s*([A-Za-z_][\w.]*)\)')


def group_of(path):
    rules = [
        (r'^/wcs_extension/internal', '01 核心握手'),
        (r'^/wcs_extension/sysui/notification', '02 通知'),
        (r'^/(wcs_extension/sysui/watchface|w_module_watchface|w_watchface_home|watchface)', '03 表盘'),
        (r'^/(wcs_extension/sysui/widget|tiles|complication_providers)', '04 小组件Tiles'),
        (r'^/(wcs_extension/sysui/apps|wcs_extension/internal/packages|wcs_extension/internal/channel/packages)', '05 应用管理'),
        (r'^/wcs_extension/quickaction', '06 快捷操作'),
        (r'^/(wcs_extension/sysui/tutorial|wcs_extension/bugreport|wcs_extension/internal/bugreport)', '07 教程诊断'),
        (r'^/w_module_sec_watch_setting_accessibility', '08 无障碍'),
        (r'^/w_module_sec_watch_setting', '09 手表设置'),
        (r'^/samsung_health', '10 三星健康'),
        (r'^/(c_devicecare|c_battery|indicator_battery|batterystate|indicator_buds)', '11 电池维护'),
        (r'^/(c_callservice|w_callservice|w_apps_incallui|w_apps_blocked_number|w_apps_callsettings|w_emergency_dialer)', '12 通话'),
        (r'^/(contacts|w_contacts)', '13 联系人'),
        (r'^/(c_alarm|w_alarm|c_calendar|w_calendar|c_timer|w_timer|c_worldclock|w_worldclock)', '14 闹钟日历计时'),
        (r'^/(c_gallery|w_gallery|w_apps_screen_capture|w_storage|c_storage)', '15 图库存储'),
        (r'^/(w_apps_music|adaptive_media_bridging|w_media_control)', '16 音乐媒体'),
        (r'^/(c_voicerecorder|w_voicerecorder)', '17 录音机'),
        (r'^/w_apps_galaxy_store', '18 应用商店'),
        (r'^/(watch_esim|w_simcardmanager|w_cmc|w_module/numbersync|watch_ns|w_wktksimchange)', '19 eSIM号码同步'),
        (r'^/(wfota|watch_ota_status)', '20 固件OTA'),
        (r'^/(w_apps_find_my|w_apps_safety_assistance)', '21 查找设备SOS'),
        (r'^/(c_reminder|w_reminder)', '22 提醒'),
        (r'^/(c_calculator|w_calculator)', '23 计算器'),
        (r'^/(w_module_keyboard|w_module_sec_setupwizard|w_module_sec_fota_setupwizard|w_module_sec_systemui)', '24 键盘OOBE'),
        (r'^/w_stf', '25 状态推送STF'),
        (r'^/watch_samsung|^/watch_gms|^/watch_self_diagnostics|^/watch_preloaded|^/watch_talkback', '26 账号云诊断'),
        (r'^/(c_runestone|w_runestone)', '27 Runestone上下文'),
        (r'^/w_walkietalkie', '28 对讲机'),
        (r'^/(settings|timezone|peers)', '29 GoogleWear基础'),
    ]
    for pat, name in rules:
        if re.match(pat, path):
            return name
    return '99 其他'


def main():
    paths = collections.defaultdict(lambda: {
        "files": set(), "consts": set(), "jsonkeys": set(), "enums": [], "pkgs": set()
    })
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
                used = set(PATH_RE.findall(txt))
                if not used:
                    continue
                consts = {}
                for m in CONST_RE.finditer(txt):
                    val = m.group(2)[1:-1]
                    consts[val] = m.group(1)
                jkeys = {}
                for m in JSONKEY_RE.finditer(txt):
                    jkeys[m.group(1)] = m.group(2)
                enums = ENUM_RE.findall(txt)
                for p in used:
                    e = paths[p]
                    e["files"].add(fp)
                    e["pkgs"].add(pkg)
                    if p in consts:
                        e["consts"].add(consts[p])
                    e["jsonkeys"].update(jkeys.keys())
                    for cls, name, ep, payload in enums:
                        if ep == p:
                            e["enums"].append([cls, name, payload])

    groups = collections.defaultdict(list)
    for p, info in paths.items():
        groups[group_of(p)].append((p, info))

    lines = []
    total = len(paths)
    lines.append(f"共 {total} 条协议路径，{len(groups)} 个子系统\n")
    for g in sorted(groups):
        items = sorted(groups[g])
        lines.append(f"\n## {g}（{len(items)} 条）\n")
        for p, info in items:
            consts = "/".join(sorted(info["consts"])) or "-"
            src = ", ".join(sorted({os.path.basename(f)[:-5] for f in info["files"]})[:3])
            lines.append(f"- `{p}`")
            lines.append(f"  - 常量: `{consts}`  来源: {src}")
            if info["enums"]:
                for cls, name, payload in info["enums"][:6]:
                    lines.append(f"  - 消息 `{name}` ({cls}) data=`{payload[:50]}`")
            if info["jsonkeys"]:
                ks = sorted(info["jsonkeys"])[:12]
                lines.append(f"  - JSON键: {', '.join(ks)}")

    out = "research/FEATURE-MATRIX.md"
    open(out, "w").write("# Galaxy Watch 全功能协议矩阵（自动提取）\n\n" + "\n".join(lines))
    print(f"-> {out}  ({total} 路径 / {len(groups)} 子系统)")

    json.dump({p: {"group": group_of(p),
                   "consts": sorted(v["consts"]),
                   "jsonkeys": sorted(v["jsonkeys"]),
                   "enums": v["enums"],
                   "files": sorted(v["files"]),
                   "pkg": sorted(v["pkgs"])}
               for p, v in paths.items()},
              open("research/feature_matrix.json", "w"), ensure_ascii=False, indent=1)
    for g in sorted(groups):
        print(f"  {g}: {len(groups[g])}")


if __name__ == '__main__':
    main()
