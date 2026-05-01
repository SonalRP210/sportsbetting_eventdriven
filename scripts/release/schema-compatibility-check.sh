#!/usr/bin/env bash
set -euo pipefail

SCHEMA_DIR="platform/event-support/src/main/resources/domain-schemas"
TOPIC_FILE="messaging/kafka/topics/topic-definitions.yaml"
DLQ_FILE="messaging/kafka/dlq/dlq-topics.yaml"

echo "Validating JSON schema files are parseable..."
python - <<'PY'
import json
from pathlib import Path

schema_dir = Path("platform/event-support/src/main/resources/domain-schemas")
files = sorted(schema_dir.glob("*.json"))
if not files:
    raise SystemExit("No schema files found")
for f in files:
    try:
        json.loads(f.read_text(encoding="utf-8"))
    except Exception as exc:
        raise SystemExit(f"Invalid JSON in {f}: {exc}")
print(f"Parsed {len(files)} schema files")
PY

echo "Checking every domain topic has a schema file..."
python - <<'PY'
from pathlib import Path
import re

topic_file = Path("messaging/kafka/topics/topic-definitions.yaml")
schema_dir = Path("platform/event-support/src/main/resources/domain-schemas")
topics = re.findall(r"^\s*-\s*name:\s*([a-zA-Z0-9._-]+)\s*$", topic_file.read_text(encoding="utf-8"), flags=re.M)
missing = []
for topic in topics:
    schema = schema_dir / f"{topic}.json"
    if not schema.exists():
        missing.append(topic)
if missing:
    raise SystemExit("Missing schema files for topics: " + ", ".join(missing))
print(f"Schema coverage OK for {len(topics)} topics")
PY

echo "Checking DLQ topics match topic catalog (+ .dlq suffix)..."
python - <<'PY'
from pathlib import Path
import re

topics_text = Path("messaging/kafka/topics/topic-definitions.yaml").read_text(encoding="utf-8")
dlq_text = Path("messaging/kafka/dlq/dlq-topics.yaml").read_text(encoding="utf-8")
topics = set(re.findall(r"^\s*-\s*name:\s*([a-zA-Z0-9._-]+)\s*$", topics_text, flags=re.M))
dlqs = set(re.findall(r"^\s*-\s*([a-zA-Z0-9._-]+)\s*$", dlq_text, flags=re.M))
expected = {f"{t}.dlq" for t in topics}
missing = sorted(expected - dlqs)
extra = sorted(dlqs - expected)
if missing or extra:
    msg = []
    if missing:
        msg.append("missing DLQ topics: " + ", ".join(missing))
    if extra:
        msg.append("unexpected DLQ topics: " + ", ".join(extra))
    raise SystemExit("; ".join(msg))
print(f"DLQ mapping OK for {len(topics)} domain topics")
PY

echo "Schema compatibility checks passed."
