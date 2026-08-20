#!/usr/bin/env bash
# Collecter Linux 一键启动脚本
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$DIR/../libs/Collecter-Desktop-3.5.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE="$DIR/Collecter-Desktop-3.5.0.jar"
fi

if ! command -v java &> /dev/null; then
    echo "未检测到 Java 运行环境，请先安装 Java 17 或更高版本 (如: sudo apt install openjdk-17-jre)"
    exit 1
fi

echo "🚀 正在启动 Collecter 桌面端 (Linux)..."
java -jar "$JAR_FILE" "$@"
