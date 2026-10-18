#!/usr/bin/env bash
# Runs the instrumented UI tests on a connected emulator and pulls the
# screenshots they take into android/build/screenshots. Used by CI; works
# locally too with an emulator running: ./scripts/emulator-ui-tests.sh
set -euo pipefail
cd "$(dirname "$0")/.."

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# Run via `am instrument` rather than Gradle so the app stays installed and
# its screenshots can be pulled afterwards.
mkdir -p build
adb shell am instrument -w wales.ryland.routine.test/androidx.test.runner.AndroidJUnitRunner | tee build/instrument.txt

mkdir -p build/screenshots
adb exec-out run-as wales.ryland.routine tar -c -C files screenshots | tar -x -C build || true
ls build/screenshots || true

grep -q "^OK (" build/instrument.txt
