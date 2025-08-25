# 🚗 PavillTaxi - Convención de Nombres de APK

## 📱 Convención de Nombres

Los APKs generados ahora siguen una convención de nombres consistente que incluye:

- **Nombre de la app**: `PavillTaxi`
- **Versión**: `v{versionName}`
- **Build number**: `b{versionCode}`
- **Tipo de build**: `{debug|release}`

### 📋 Formato de Nombres

```
PavillTaxi-v{versionName}-b{versionCode}-{buildType}.apk
```

### 🔍 Ejemplos

- **Debug**: `PavillTaxi-v3.01-b211-debug.apk`
- **Release**: `PavillTaxi-v3.01-b211-release.apk`

## 🛠️ Configuración

La configuración está en `app/build.gradle.kts`:

```kotlin
applicationVariants.all {
    val variant = this
    variant.outputs
        .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
        .forEach { output ->
            val buildType = variant.buildType.name
            val versionName = variant.versionName
            val versionCode = variant.versionCode
            
            val outputFileName = when {
                buildType == "debug" -> "PavillTaxi-v${versionName}-b${versionCode}-debug.apk"
                buildType == "release" -> "PavillTaxi-v${versionName}-b${versionCode}-release.apk"
                else -> "PavillTaxi-v${versionName}-b${versionCode}-${buildType}.apk"
            }
            
            output.outputFileName = outputFileName
        }
}
```

## 🚀 Construcción de APKs

### Usando Gradle directamente

```bash
# APK Debug
./gradlew assembleDebug

# APK Release
./gradlew assembleRelease

# Ambos APKs
./gradlew assembleDebug assembleRelease
```

### Usando scripts de construcción

#### Linux/macOS
```bash
./build_apk.sh
```

#### Windows
```cmd
build_apk.bat
```

## 📁 Ubicación de Archivos

Los APKs generados se encuentran en:

- **Debug**: `app/build/outputs/apk/debug/`
- **Release**: `app/build/outputs/apk/release/`

## 🔄 Actualización de Versiones

Para cambiar la versión, modifica en `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 212        // Incrementar build number
    versionName = "3.02"     // Nueva versión
}
```

## ✅ Beneficios

1. **Identificación clara**: Fácil identificación de versiones y builds
2. **Organización**: Nombres consistentes y organizados
3. **Trazabilidad**: Rastreo de versiones por build number
4. **Profesionalismo**: Nombres de archivos más profesionales
5. **Automatización**: Fácil integración con CI/CD

## 🐛 Solución de Problemas

### Error de compilación
```bash
./gradlew clean
./gradlew assembleDebug
```

### Verificar configuración
```bash
./gradlew tasks --all
```

### Limpiar build
```bash
./gradlew clean
```

## 📞 Soporte

Para problemas o preguntas sobre la construcción de APKs, revisa:

1. Logs de Gradle
2. Configuración en `build.gradle.kts`
3. Versiones de Android Gradle Plugin
4. Dependencias del proyecto

---

**Nota**: Esta configuración es compatible con Android Gradle Plugin 7.0+ y Gradle 7.0+
