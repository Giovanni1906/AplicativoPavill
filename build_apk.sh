#!/bin/bash

# Script para construir APKs de PavillTaxi
# Este script construye tanto debug como release APKs

echo "🚗 Construyendo APKs de PavillTaxi..."
echo ""

# Mostrar información de versión actual
echo "📱 Información de versión:"
echo "   - Version Name: 3.01"
echo "   - Version Code: 211"
echo ""

# Construir APK de debug
echo "🔨 Construyendo APK de DEBUG..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ APK de DEBUG construido exitosamente"
    echo "📁 Archivo generado: app/build/outputs/apk/debug/PavillTaxi-v3.01-b211-debug.apk"
else
    echo "❌ Error al construir APK de DEBUG"
    exit 1
fi

echo ""

# Construir APK de release
echo "🔨 Construyendo APK de RELEASE..."
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    echo "✅ APK de RELEASE construido exitosamente"
    echo "📁 Archivo generado: app/build/outputs/apk/release/PavillTaxi-v3.01-b211-release.apk"
else
    echo "❌ Error al construir APK de RELEASE"
    exit 1
fi

echo ""
echo "🎉 ¡Construcción completada!"
echo ""
echo "📋 Resumen de archivos generados:"
echo "   - Debug:  PavillTaxi-v3.01-b211-debug.apk"
echo "   - Release: PavillTaxi-v3.01-b211-release.apk"
echo ""
echo "📍 Ubicación: app/build/outputs/apk/{debug|release}/"
