#!/data/data/com.termux/files/usr/bin/env bash
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

# 1. Extract current versionName & versionCode
VERSION_NAME=$(grep -oP 'versionName="\K[^"]+' AndroidManifest.xml || echo "1.5.0")
VERSION_CODE=$(grep -oP 'versionCode="\K[^"]+' AndroidManifest.xml || echo "10500")

echo "=== Automated Release Pipeline for W-adb v$VERSION_NAME (code: $VERSION_CODE) ==="

# 2. Compile, ZipAlign & Sign APK
./build_apk.sh

# 3. Copy release artifact to home root
cp -f W-adb.apk /data/data/com.termux/files/home/W-adb.apk

# 4. Commit and Push to W-adb GitHub repository
git add .
git commit -m "Automated Release v$VERSION_NAME: $VERSION_CODE" || true
git push origin master

# 5. Create GitHub Release
TAG_NAME="v$VERSION_NAME"
if ! gh release view "$TAG_NAME" >/dev/null 2>&1; then
    echo "Creating GitHub Release $TAG_NAME..."
    gh release create "$TAG_NAME" W-adb.apk --title "v$VERSION_NAME - W-adb Auto Release" --notes-file RELEASE_NOTES.md || true
else
    echo "Updating GitHub Release $TAG_NAME assets..."
    gh release upload "$TAG_NAME" W-adb.apk --clobber || true
fi

# 6. Publish to vdomov-apks repository
if [ -d /data/data/com.termux/files/home/vdomov-apks ]; then
    echo "Syncing release binary to vdomov-apks..."
    cp -f W-adb.apk /data/data/com.termux/files/home/vdomov-apks/W-adb.apk
    cd /data/data/com.termux/files/home/vdomov-apks
    git add W-adb.apk
    git commit -m "Auto Update v$VERSION_NAME W-adb.apk" || true
    git push origin main || true
fi

echo "=== RELEASE v$VERSION_NAME PUBLISHED SUCCESSFULLY ==="
