#!/usr/bin/env bash
# 恢复在协议调试期间临时禁用的三星 / Google Wear 组件。
#
# 背景：Honor 蓝牙栈不允许同一 RFCOMM SCN 上存在第二个客户端 socket。
# 为了让自研 app 独占 Galaxy Watch 的数据通道（RFCOMM ch8 / UUID 5e8945b0），
# 调试时执行了 `pm disable-user` 禁用下列包。手表要恢复正常使用，必须恢复。
#
# 用法： ./research/restore-samsung-packages.sh

set -uo pipefail

PACKAGES=(
  com.samsung.wearable.watch7plugin
  com.samsung.android.app.watchmanager
  com.google.android.wearable.app.cn      # 中国区 Wear OS companion
  com.google.android.wearable.app         # 国际版（若存在）
)

if ! command -v adb >/dev/null 2>&1; then
  echo "找不到 adb，请先安装。" >&2
  exit 1
fi

if [ -z "$(adb devices | sed -n '2,$p' | grep -w device)" ]; then
  echo "没有已连接的设备。请插上 USB 并开启 USB 调试后重试。" >&2
  exit 1
fi

echo "==> 当前被禁用的相关包："
adb shell pm list packages -d 2>/dev/null | grep -E "watch7plugin|watchmanager|wearable" || echo "   （无）"

echo
echo "==> 恢复启用："
for p in "${PACKAGES[@]}"; do
  if adb shell pm list packages 2>/dev/null | grep -q "^package:${p}$"; then
    out=$(adb shell pm enable --user 0 "$p" 2>&1 | tail -1)
    printf "  %-45s %s\n" "$p" "$out"
  else
    printf "  %-45s (未安装，跳过)\n" "$p"
  fi
done

echo
echo "==> 重启三星组件（可选，加速恢复连接）："
for p in com.google.android.wearable.app.cn com.samsung.wearable.watch7plugin com.samsung.android.app.watchmanager; do
  adb shell am force-stop "$p" 2>/dev/null && printf "  force-stop %s\n" "$p"
done

echo
echo "==> 恢复结果："
adb shell pm list packages -d 2>/dev/null | grep -E "watch7plugin|watchmanager|wearable" \
  && echo "  ⚠ 仍有被禁用的包，请手动检查" \
  || echo "  ✓ 全部已启用"

echo
echo "建议随后在手机蓝牙设置里断开/重连一次手表，或重启蓝牙。"
