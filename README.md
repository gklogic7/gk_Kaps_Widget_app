# KAPS — Custom Image, GIF & SVG Android Widget Studio

KAPS is a super-lightweight, 100% offline native Android application designed for tablets and phones. It allows users to select any static image, animated GIF/WebP, or vector SVG from their device via Android's Storage Access Framework (SAF) and convert it into a crisp, alpha-preserving Android home-screen widget.

## Key Features

1. **True Alpha Channel & Transparency Preservation**:
   - Supports transparent PNG, WebP, GIF, and SVG graphics using `ARGB_8888` bitmaps and a zero-padding transparent `RemoteViews` container.
   - Includes a toggleable **Checkerboard Transparency Grid** strictly inside the studio editor (never rendered on the home screen).
   - Pure Desktop Art Mode: Placed home-screen widgets have no tap interceptor attached so they sit cleanly over your wallpaper without accidental app launches.

2. **First-Class Static & Animated Vector SVG Engine**:
   - Dynamic resolution-aware rasterization: SVGs are rendered directly onto a hardware-compatible canvas at the exact pixel dimensions reported by `AppWidgetManager.getAppWidgetOptions()` whenever placed or resized.
   - Supports `<svg viewBox>`, `<g>`, `<path>` (`M, L, H, V, C, S, Q, T, A, Z`), `<rect>`, `<circle>`, `<ellipse>`, `<line>`, `<polyline>`, `<polygon>`, `<linearGradient>`, `<radialGradient>`, `<clipPath>`, and inline/embedded CSS styling.
   - **Animated SVG Support**: Evaluates `<animate>` (opacity, radius, stroke-width, coordinates) and `<animateTransform>` (`rotate`, `scale`, `translate`) across keyframe phases. Clearly reports any unsupported SVG tags (such as `<filter>` or `<script>`) in the studio diagnostics panel.

3. **Battery-Conscious Animated GIF & WebP Playback**:
   - Extracts individual alpha-preserving frames and frame delays using a native GIF89a LZW frame extractor with `Movie` fallback.
   - Uses native `RemoteViews` `ViewFlipper` frame cycling with configurable playback speed (`0.5×` to `2.0×`), starting frame selection, static-frame fallback, and automatic pause during Android Battery Saver / Doze mode.

4. **Flexible Widget Resizing & Geometry Controls**:
   - Size presets (`Small 2×2`, `Medium 3×2`, `Large 4×3`, `Extra Large 4×4`, `Custom Ratio`) plus full support for free launcher drag-handles (`resizeMode="horizontal|vertical"`).
   - Scale modes (`Fit Inside`, `Fill`, `Crop`, `Center`, `Stretch`), aspect-ratio lock, zoom multiplier (`50%–200%`), X/Y offset sliders, opacity (`10%–100%`), and 6 background modes (`Fully Transparent`, `Original Image BG`, `Solid Color`, `Custom Color`, `Rounded Card`, `No Background`).

5. **100% Offline & Privacy-First**:
   - No internet permission, no accounts, no cloud uploads, and no analytics.
   - Uses Android's zero-permission Storage Access Framework (`OpenDocument`) with persistable URI permissions and a local media sandbox (`filesDir/kaps_media`) so widgets survive device reboots (`BOOT_COMPLETED`).

## Architecture Overview

- **`app/`**: `KapsApplication`, `MainActivity` (adaptive `NavigationRail` for tablets / `NavigationBar` for phones).
- **`core/`**: `FileAccess` (SAF & source verification), `ImageCache` (bounded `LruCache`), `KapsImageDecoder`, `WidgetRepository`.
- **`data/`**: `WidgetDatabase` (Room entities & DAOs for `WidgetConfigEntity` and `MediaItemEntity`), `WidgetPreferences` (Jetpack DataStore), `MediaStorage`.
- **`domain/`**: `CreateWidgetUseCase`, `UpdateWidgetUseCase`, `DeleteWidgetUseCase`, `ResizeWidgetUseCase`, `ImportMediaUseCase`.
- **`features/`**: `StudioViewModel`, `HomeScreen` (My Widgets), `WidgetEditorScreen` (Create/Configure Studio), `ImageLibraryScreen`, `SettingsScreen`, `Components`.
- **`widgets/`**: `KapsWidgetProvider` (`AppWidgetProvider`), `KapsWidgetReceiver` (`BOOT_COMPLETED` & Battery Saver receiver), `WidgetConfigurationActivity`, `WidgetRenderer`, `StaticImageRenderer`, `GifRenderer`, `SvgRenderer`, `WidgetUpdater`.
- **`utils/`**: `AlphaUtils`, `BitmapUtils`, `DimensionUtils`, `ImageUtils`.

## Build & Test Instructions

1. **Compile Debug APK**:
   ```bash
   gradle assembleDebug
   ```
2. **Build Release APK**:
   ```bash
   gradle assembleRelease
   ```
3. **Run Unit & Robolectric Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
