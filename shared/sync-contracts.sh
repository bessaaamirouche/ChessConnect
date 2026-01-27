#!/bin/bash
# Sync contracts from shared to frontend

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE="$SCRIPT_DIR/contracts/index.ts"
DEST="$SCRIPT_DIR/../frontend-web/src/contracts/index.ts"

if [ -f "$SOURCE" ]; then
    mkdir -p "$(dirname "$DEST")"
    cp "$SOURCE" "$DEST"
    echo "✓ Contracts synced to frontend"
else
    echo "✗ Source file not found: $SOURCE"
    exit 1
fi
