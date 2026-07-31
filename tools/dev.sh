#!/usr/bin/env bash
# Comandi di sviluppo della tastiera T9, in un punto solo.
#
# Il Gradle wrapper non è nel repo, quindi ogni comando dovrebbe ripetere la stessa
# preparazione d'ambiente (JDK di Android Studio, SDK, ricerca del gradle scaricato).
# Averla qui rende i comandi brevi e uguali ogni volta — e permette di autorizzarli
# una volta sola in `.claude/settings.local.json` invece che uno per uno.
#
# Uso, dalla root del progetto:
#   bash tools/dev.sh test              # unit test JVM (nessun emulatore)
#   bash tools/dev.sh install           # build + installa su emulatore/telefono
#   bash tools/dev.sh apk               # genera l'APK debug e stampa il percorso
#   bash tools/dev.sh gradle <task...>  # qualsiasi altro task Gradle
#   bash tools/dev.sh boot              # avvia l'emulatore e aspetta il boot
#   bash tools/dev.sh ime               # abilita e seleziona la tastiera T9
#   bash tools/dev.sh shot <file.png>   # screenshot dell'emulatore
#   bash tools/dev.sh adb <args...>     # adb con l'ambiente già impostato

set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
export ANDROID_HOME="${ANDROID_HOME:-/c/Users/Antonio/AppData/Local/Android/Sdk}"

ADB="$ANDROID_HOME/platform-tools/adb.exe"
EMULATOR="$ANDROID_HOME/emulator/emulator.exe"
IME="com.daux.t9keyboard/.service.T9ImeService"

gradle_bin() {
    ls /c/Users/Antonio/.gradle/wrapper/dists/gradle-*-bin/*/gradle-*/bin/gradle 2>/dev/null | head -1
}

run_gradle() {
    local gradle
    gradle="$(gradle_bin)"
    if [ -z "$gradle" ]; then
        echo "Gradle non trovato: aprire il progetto in Android Studio una volta." >&2
        exit 1
    fi
    "$gradle" "$@" --console=plain
}

cmd="${1:-help}"
shift || true

case "$cmd" in
    test)    run_gradle :app:testDebugUnitTest ;;
    install) run_gradle :app:installDebug ;;
    apk)
        run_gradle :app:assembleDebug
        # Copia con la versione nel nome: sul telefono si accumulano più APK, e
        # "app-debug.apk" non dice quale si sta installando.
        version=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
        src="app/build/outputs/apk/debug/app-debug.apk"
        out="app/build/outputs/apk/debug/t9-$version-debug.apk"
        cp "$src" "$out"
        echo
        echo "Versione: $version   (l'app e la tastiera si chiamano \"T9 $version\")"
        echo "APK: $(pwd)/$out"
        ;;
    gradle)  run_gradle "$@" ;;

    boot)
        if "$ADB" devices | grep -q "device$"; then
            echo "Emulatore già avviato."
        else
            "$EMULATOR" -avd "${1:-Pixel_10_Pro}" -no-snapshot-save > /dev/null 2>&1 &
            "$ADB" wait-for-device
            "$ADB" shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
            echo "Emulatore pronto."
        fi
        ;;

    ime)
        # Senza questo, sull'emulatore la tastiera hardware del PC nasconde quella
        # software e non compare nulla (vedi il gotcha in DEVELOPMENT.md).
        "$ADB" shell settings put secure show_ime_with_hard_keyboard 1
        "$ADB" shell ime enable "$IME"
        "$ADB" shell ime set "$IME"
        ;;

    shot)
        out="${1:?uso: dev.sh shot <file.png>}"
        "$ADB" exec-out screencap -p > "$out"
        echo "$out"
        ;;

    adb)     "$ADB" "$@" ;;

    *)
        sed -n '2,20p' "$0"
        ;;
esac
