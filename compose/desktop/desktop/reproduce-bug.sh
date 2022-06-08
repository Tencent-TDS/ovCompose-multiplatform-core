#!/bin/bash
# Script works on MacOS, maybe on Windows need to clear gradle cache in different way

cd "$(dirname "$0")"

export OUT_DIR=/tmp/compose_out
rm -rf /tmp/compose_out

../../../../../scripts/runGradle clean --no-daemon :support:compose:desktop:desktop:desktop-samples:runBug
