#!/data/data/com.termux/files/usr/bin/env bash
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

echo "=== Building W-adb APK ==="

# 1. Clean previous build output & create build directories
rm -rf build/
mkdir -p build/gen build/obj build/bin libs

# 2. Check for android.jar
if [ ! -f libs/android.jar ]; then
    echo "[1/7] Downloading android.jar..."
    curl -sSL -o libs/android.jar "https://github.com/Sable/android-platforms/raw/master/android-30/android.jar"
fi

# 3. Generate R.java with AAPT
echo "[2/7] Generating R.java resources..."
aapt package -f -m \
    -J build/gen \
    -S res \
    -M AndroidManifest.xml \
    -I libs/android.jar

# 4. Compile Java sources
echo "[3/7] Compiling Java source files..."
javac -d build/obj \
    -classpath libs/android.jar \
    -sourcepath "src:build/gen" \
    $(find src build/gen -name "*.java")

# 5. Convert bytecode to DEX using d8
echo "[4/7] Converting bytecode to DEX..."
d8 --output build/bin --classpath libs/android.jar $(find build/obj -name "*.class")

# 6. Package unaligned APK with AAPT including assets & DEX
echo "[5/7] Packaging unaligned APK..."
aapt package -f \
    -M AndroidManifest.xml \
    -S res \
    -A assets \
    -I libs/android.jar \
    -F build/bin/app-unaligned.apk

(cd build/bin && aapt add app-unaligned.apk classes.dex)

# 7. ZipAlign alignment (4-byte alignment mandatory for Android installation)
echo "[6/7] Aligning APK with zipalign..."
zipalign -f -v 4 build/bin/app-unaligned.apk build/bin/app-aligned.apk

# 8. Generate debug key if missing and sign APK
if [ ! -f debug.keystore ]; then
    echo "Generating persistent debug keystore..."
    keytool -genkey -v \
        -keystore debug.keystore \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

echo "[7/7] Signing APK with apksigner..."
apksigner sign \
    --ks debug.keystore \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out W-adb.apk \
    build/bin/app-aligned.apk

cp W-adb.apk AdbScrcpyConnect.apk

echo "=== BUILD & ALIGNMENT SUCCESSFUL ==="
ls -lh W-adb.apk
