#!/usr/bin/env bash
# Collecter macOS 双击直启脚本
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$DIR/../libs/Collecter-Desktop-3.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE="$DIR/Collecter-Desktop-3.0.0.jar"
fi

if ! command -v java &> /dev/null; then
    osascript -e 'display alert "缺少 Java 环境" message "请先安装 Java 17 或更高版本 (推荐使用 brew install openjdk@17 或从 Adoptium 下载)"'
    exit 1
fi

java -Xdock:name="Collecter" -jar "$JAR_FILE"
