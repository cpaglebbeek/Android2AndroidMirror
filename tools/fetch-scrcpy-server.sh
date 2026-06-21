#!/usr/bin/env bash
# Haalt de scrcpy-server.jar op (Apache-2.0) — wordt NIET in de repo gecommit.
# Moet matchen met ScrcpyServer.SERVER_VERSION.
set -euo pipefail

VERSION="3.1"
DEST="$(dirname "$0")/../app/src/main/assets/scrcpy-server/scrcpy-server.jar"
URL="https://github.com/Genymobile/scrcpy/releases/download/v${VERSION}/scrcpy-server-v${VERSION}"

echo "Downloading scrcpy-server v${VERSION} ..."
curl -fsSL "$URL" -o "$DEST"
echo "Saved to: $DEST"
echo "SHA256: $(sha256sum "$DEST" | cut -d' ' -f1)"
