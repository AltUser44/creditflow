#!/usr/bin/env bash
# Creates topic transaction-events. Requires kafka-topics on PATH.
set -euo pipefail
BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:9092}"
TOPIC="${KAFKA_TOPIC:-transaction-events}"
kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists --topic "$TOPIC" --partitions 1 --replication-factor 1
echo "Topic '$TOPIC' ready on $BOOTSTRAP"
