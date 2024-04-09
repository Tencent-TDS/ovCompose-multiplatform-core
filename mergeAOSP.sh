#!/bin/bash
set -e

if [ -z "$1" ]; then
echo "Specify the commit. For example: ./mergeAOSP.sh jetpack-compose/1.5.1"
exit 1
fi

COMMIT=$1

git merge $COMMIT --no-commit --no-ff || true

git checkout --no-overlay HEAD --ours -- './buildSrc' || true
git checkout --no-overlay HEAD --ours -- './compose/*/build.gradle' || true
git checkout --no-overlay HEAD --ours -- './.github' || true
git checkout --no-overlay HEAD --ours -- './.idea' || true
git checkout --no-overlay HEAD --ours -- './.run' || true
git checkout --no-overlay HEAD --ours -- 'CONTRIBUTING.md' || true
git checkout --no-overlay HEAD --ours -- 'README.md' || true
git checkout --no-overlay HEAD --ours -- 'gradlew' || true
git checkout --no-overlay HEAD --ours -- 'gradlew.bat' || true
git checkout --no-overlay HEAD --ours -- 'gradle.properties' || true
git checkout --no-overlay HEAD --ours -- './gradle/*' || true
git checkout --no-overlay HEAD --ours -- 'build.gradle' || true
git checkout --no-overlay HEAD --ours -- 'settings.gradle' || true
git checkout --no-overlay HEAD --ours -- './collection/*/jvmMain/*' || true
git checkout --no-overlay HEAD --ours -- './collection/*/jvmTest/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/skikoMain/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/desktopMain/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/skikoTest/*' || true
git checkout --no-overlay HEAD --ours -- './compose/*/desktopTest/*' || true
git checkout --no-overlay HEAD --ours -- './compose/desktop/*' || true
