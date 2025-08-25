@echo off
chcp 65001 >nul
echo 🚗 Construyendo APKs de PavillTaxi...
echo.

REM Mostrar información de versión actual
echo 📱 Información de versión:
echo    - Version Name: 3.01
echo    - Version Code: 211
echo.

REM Construir APK de debug
echo 🔨 Construyendo APK de DEBUG...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo ✅ APK de DEBUG construido exitosamente
    echo 📁 Archivo generado: app\build\outputs\apk\debug\PavillTaxi-v3.01-b211-debug.apk
) else (
    echo ❌ Error al construir APK de DEBUG
    pause
    exit /b 1
)

echo.

REM Construir APK de release
echo 🔨 Construyendo APK de RELEASE...
call gradlew.bat assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo ✅ APK de RELEASE construido exitosamente
    echo 📁 Archivo generado: app\build\outputs\apk\release\PavillTaxi-v3.01-b211-release.apk
) else (
    echo ❌ Error al construir APK de RELEASE
    pause
    exit /b 1
)

echo.
echo 🎉 ¡Construcción completada!
echo.
echo 📋 Resumen de archivos generados:
echo    - Debug:  PavillTaxi-v3.01-b211-debug.apk
echo    - Release: PavillTaxi-v3.01-b211-release.apk
echo.
echo 📍 Ubicación: app\build\outputs\apk\{debug^|release}\
echo.
pause
