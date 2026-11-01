# Filetor

Administrador de archivos y compresor para Android, moderno y completo. Hecho con Kotlin + Jetpack Compose (Material 3).

## Funciones

**Explorador**
- Almacenamiento interno, tarjeta SD y USB con barra de uso.
- Pestañas múltiples, migas de pan (breadcrumbs), vista de lista o cuadrícula, lista compacta.
- Orden por nombre (natural: `foto2` < `foto10`), fecha, tamaño o tipo; carpetas primero; archivos ocultos.
- Selección múltiple, seleccionar todo / invertir selección.
- Copiar, cortar y pegar con portapapeles persistente entre carpetas y pestañas.
- Copiar a… / Mover a… con selector de carpetas.
- Renombrar y **renombrado por lotes** (patrón `{n}`/`{name}`/`{ext}`, buscar y reemplazar, mayúsculas/minúsculas) con vista previa.
- Filtro rápido en la carpeta actual y búsqueda recursiva con comodines (`*`, `?`) y filtros por tipo.
- Favoritos, historial de archivos abiertos, propiedades con tamaño de carpetas y sumas MD5/SHA-1/SHA-256.
- Miniaturas de imágenes, vídeos e iconos de APK.

**Compresión (alternativa a ZArchiver)**
- Ver el contenido de ZIP, 7z, RAR, TAR, TAR.GZ, TAR.BZ2, TAR.XZ, GZ, BZ2, XZ, JAR y APK sin extraer.
- Abrir un archivo de dentro del comprimido directamente.
- Extraer todo o solo lo seleccionado: aquí, en una carpeta con el nombre del archivo o en cualquier ubicación.
- Archivos protegidos con contraseña (ZIP, 7z, RAR).
- Crear ZIP (con cifrado AES-256), 7z, TAR, TAR.GZ, TAR.XZ y TAR.BZ2 con nivel de compresión ajustable.
- Abre comprimidos desde otras apps (“Abrir con Filetor”).

**Herramientas**
- Análisis de almacenamiento: gráfico por tipo, carpetas y archivos más grandes, sugerencias de limpieza (descargas antiguas, carpetas vacías).
- Buscador de **duplicados** por contenido (tamaño → hash parcial → hash completo).
- **Papelera de reciclaje** con restauración y vaciado automático configurable.
- Editor de texto integrado (búsqueda, ajuste de línea, tamaño de fuente) y visor de imágenes con zoom y deslizamiento.
- Instalador / inspector de APK (versión, SDK, permisos, si ya está instalada).
- Categorías: imágenes, vídeos, música, documentos, APKs, comprimidos, descargas y recientes.
- Selector de archivos para otras apps (`GET_CONTENT`).
- Operaciones en segundo plano con progreso, cancelación y notificación.

**Diseño**
- Material 3 con 7 colores de acento, Material You (Android 12+), modo claro/oscuro/sistema y negro AMOLED.

## Compilar

El APK se compila automáticamente con GitHub Actions en cada push y se publica en **Releases**.
Localmente: `./gradlew assembleRelease` (requiere Android SDK 35).

Firma: `keystore/filetor.jks` (contraseña `filetor123`, alias `filetor`). Es una clave de desarrollo incluida
en el repositorio para que todas las compilaciones sean actualizables entre sí; cámbiala si vas a publicar la app.

Requiere Android 8.0 (API 26) o superior.
