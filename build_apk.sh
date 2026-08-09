#!/data/data/com.termux/files/usr/bin/env bash
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

echo "=== Building AdbScrcpyConnect APK ==="

# 1. Create build directories
mkdir -p build/gen build/obj build/bin libs

# 2. Check for android.jar
if [ ! -f libs/android.jar ]; then
    echo "[1/6] Downloading android.jar..."
    curl -sSL -o libs/android.jar "https://github.com/Sable/android-platforms/raw/master/android-30/android.jar"
fi

# 3. Generate R.java with AAPT
echo "[2/6] Generating R.java resources..."
aapt package -f -m \
    -J build/gen \
    -S res \
    -M AndroidManifest.xml \
    -I libs/android.jar

# 4. Compile Java sources
echo "[3/6] Compiling Java source files..."
javac -d build/obj \
    -classpath libs/android.jar \
    -sourcepath "src:build/gen" \
    $(find src build/gen -name "*.java")

# 5. Convert bytecode to DEX using d8
echo "[4/6] Converting bytecode to DEX..."
d8 --output build/bin --classpath libs/android.jar $(find build/obj -name "*.class")

# 6. Package APK with AAPT including assets
echo "[5/6] Packaging APK file..."
aapt package -f \
    -M AndroidManifest.xml \
    -S res \
    -A assets \
    -I libs/android.jar \
    -F build/bin/app-unsigned.apk \
    build/bin

# 7. Generate debug key and sign APK
if [ ! -f debug.keystore ]; then
    echo "Generating debug keystore..."
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

echo "[6/6] Signing APK with apksigner..."
apksigner sign \
    --ks debug.keystore \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out AdbScrcpyConnect.apk \
    build/bin/app-unsigned.apk

echo "=== BUILD SUCCESSFUL ==="
ls -lh AdbScrcpyConnect.apk
