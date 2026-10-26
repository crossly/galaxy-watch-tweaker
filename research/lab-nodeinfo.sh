#!/usr/bin/env bash
# NodeInfo 字段隔离实验
# 用法： ./research/lab-nodeinfo.sh "0 1 2 3" 每模式轮数
set -uo pipefail
cd "$(dirname "$0")/.."
SERIAL=$(adb devices | sed -n '2p' | cut -f1)
[ -z "$SERIAL" ] && { echo "无设备"; exit 1; }
MAC="A0:B0:BD:F8:AB:FD"
PKG=dev.ricky.watchfree
SVC=$PKG/.service.WatchConnectionService
MODES=${1:-"0 1 2 3"}
ROUNDS=${2:-2}
SETTLE=22
OBSERVE=13

declare_mode() {
  case "$1" in
    0) echo "原始帧(对照)";; 1) echo "换requestId";; 2) echo "换timestamp";;
    3) echo "两项都换";;      4) echo "换deviceName";; 5) echo "换nodeId";;
    6) echo "reqId改1字节";;  7) echo "时间戳+1ns";;   8) echo "全新身份";;
    9) echo "合成帧(沿用nodeId)";; *) echo "?";;
  esac
}

SUM=""
for r in $(seq 1 "$ROUNDS"); do
  for m in $MODES; do
    adb -s "$SERIAL" shell am force-stop $PKG >/dev/null 2>&1
    adb -s "$SERIAL" shell am start -n $PKG/.MainActivity >/dev/null 2>&1
    sleep "$SETTLE"
    adb -s "$SERIAL" logcat -c >/dev/null 2>&1
    adb -s "$SERIAL" shell am start-foreground-service -n $SVC \
      --es mac "$MAC" --ei lab "$m" --ez labOnly true --el backoff 60000 >/dev/null 2>&1
    sleep "$OBSERVE"
    LOG=$(adb -s "$SERIAL" logcat -d -s WcsSession 2>/dev/null)
    RET=$(adb -s "$SERIAL" logcat -d 2>/dev/null | grep -c "读循环结束")
    got=$(printf '%s' "$LOG" | grep -c "←")
    sent=$(printf '%s' "$LOG" | grep -c "LAB$m TX")
    printf "  R%s LAB=%s %-20s 发出=%s 断开=%s 收到=%s\n" \
      "$r" "$m" "$(declare_mode "$m")" "$sent" "$RET" "$got"
    SUM="$SUM$m $sent $RET $got\n"
    adb -s "$SERIAL" shell am force-stop $PKG >/dev/null 2>&1
  done
done

echo
echo "================ 汇总 ================"
printf "%-6s %-22s %6s %6s %6s\n" "LAB" "改动" "发出" "断开" "收到"
printf "$SUM" | awk '{a[$1]+=$2; b[$1]+=$3; c[$1]+=$4; n[$1]++}
END{for (k in a) printf "%-6s %-22s %6d %6d %6d\n", k, "", a[k], b[k], c[k]}' | sort -n
