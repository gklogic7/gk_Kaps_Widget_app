# KAPS — Custom Image, GIF & SVG Android Widget Studio

KAPS is a super-lightweight, 100% offline native Android studio application that lets users transform any static image (PNG, JPG, WebP, BMP), animated GIF/WebP, or static/animated SVG graphic into a crisp, alpha-preserving Android home-screen widget optimized for tablets and phones.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design choices have been confirmed from your answers and are baked into this blueprint:

- **Built-In Starter Media Library**: KAPS ships with curated starter graphics (transparent PNG character illustration, animated pixel/geometric GIF, crisp vector SVG logo, and animated SVG emblem) alongside full Storage Access Framework (SAF) device file import so you can test and place every widget type immediately.
- **Pure Desktop Art Widget Behavior**: Placed home-screen widgets have no tap interceptor attached by default, ensuring they act as pure, distraction-free desktop artwork over your wallpaper without accidentally launching activities when swiping or tapping the home screen. All widget edits and management happen inside the KAPS studio app or Android's widget reconfiguration trigger.
- **Clean Light iPadOS Studio Canvas**: KAPS uses a bright, tactile iPadOS-inspired studio theme by default (`#F6F6F9` alabaster canvas, `#FFFFFF` frosted cards with subtle borders, `#111216` ink typography, Nothing OS technical dot-matrix badges, and `#E11D48` signal crimson / `#2563EB` studio cobalt accents), with an optional Dark Mode toggle in Settings.

---

### 1. Overview & Core Concept

- **What It Does**: Provides an intuitive studio environment to import images, animated GIFs, and vector SVGs, inspect and customize how they render (alpha transparency, checkerboard preview, sizing presets, aspect ratio, fit/crop/center/stretch scaling, custom backgrounds, opacity, and animation playback/speed), and deploy them as independent native Android App Widgets (`AppWidgetProvider` + `RemoteViews`).
- **Target Audience / Persona**: Android tablet and phone enthusiasts who want to personalize their home screen with transparent stickers, characters, brand logos, vector illustrations, and battery-conscious animated art without bloatware, cloud accounts, or background battery drain.
- **Key Value**: True alpha-channel preservation (zero unwanted borders, shadows, or white fills), crisp resolution-aware vector SVG rasterization on widget resize, honest and battery-safe frame scheduling for animated GIFs and SVGs, and strict offline privacy.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **My Widgets Dashboard**: Launches into a tablet-optimized studio workspace showing active and saved widget configurations with live checkerboard thumbnails, format badges (`PNG`, `GIF`, `SVG`, `ANIM-SVG`), dimensions, quick Edit/Delete/Duplicate controls, a prominent **Create New Widget** action, and a collapsible step-by-step Home Screen Placement Guide.
  2. **Create & Configure Widget Studio**:
     - *Step 1 (Select Media)*: Pick any device file via Android's zero-permission Storage Access Framework (`OpenDocument`) or choose a built-in starter sample from the Image Library.
     - *Step 2 (Interactive Live Preview)*: Inspect the graphic over a toggleable high-contrast **Checkerboard Transparency Canvas** (never rendered on the home screen), with live zoom/size simulation for Small (`2×2`), Medium (`3×2`), Large (`4×3`), Extra Large (`4×4`), or Custom aspect ratios.
     - *Step 3 (Appearance & Geometry Controls)*: Adjust Width/Height targets, Maintain Aspect Ratio toggle, Scale Type (`Fit Inside`, `Fill`, `Crop`, `Center`, `Stretch`), Image Position offset (`X/Y` alignment), Scale multiplier (`50%–200%`), Opacity (`10%–100%`), and Background Mode (`Fully Transparent`, `Original Image Background`, `Solid Color`, `Custom Color`, `Rounded Card Background`, `No Background`).
     - *Step 4 (Animation & Compatibility Controls)*: For GIFs and Animated SVGs, toggle Play/Pause, scrub or pick the starting frame, select Loop Mode (`Continuous Loop` or `Play Once`), adjust Animation Speed (`0.5×`, `1.0×`, `1.5×`, `2.0×`, or Battery-Saver interval), or switch to Static Frame Fallback. Displays a clear compatibility banner explaining supported SVG animation tags (`<animate>`, `<animateTransform>`, CSS rotation/opacity/scale/translate) and flagging any unsupported features in an imported SVG.
     - *Step 5 (Save & Place)*: Save the widget preset and either request direct pinning via `AppWidgetManager.requestPinAppWidget` (on supported launchers) or bind it through the native Android Home-Screen Widget Picker (`WidgetConfigurationActivity`).
  3. **Image Library**: Browse, inspect, re-verify access permissions, or remove imported device media and built-in starter assets, with automatic detection and recovery prompts if an external source file was moved or deleted.
  4. **Settings**: Configure global defaults (default studio theme posture: Clean Light iPadOS Canvas / Nothing OS Dark / System, default transparency checkerboard contrast, global GIF/SVG animation frame-rate cap, bitmap cache limit and one-tap cache purge, and battery-saver pause behavior).

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **iPadOS Pro Studio meets Nothing OS Precision** — airy, tactile studio surfaces paired with crisp technical metadata badges, monospace dimension readouts, and zero visual clutter.
  - *Color Palette & Mood*:
    - `--background`: `#F6F6F9` (Alabaster Studio Canvas)
    - `--surface`: `#FFFFFF` (Pure Frosted Card Surface with `#E4E5EC` 1dp hairline border)
    - `--surface-variant`: `#EDEEF3` (Segmented Control & Tool Well)
    - `--primary`: `#111216` (Obsidian Studio Ink for primary actions & headers)
    - `--accent`: `#E11D48` (Signal Crimson for active badges, dot-matrix accents, and recording/live indicators)
    - `--secondary-accent`: `#2563EB` (Blueprint Cobalt for resize handles and vector bounding boxes)
    - `--checkerboard-light` / `--checkerboard-dark`: `#FFFFFF` and `#DCDFE6` (Editor-only alpha grid)
  - *Typography & Hierarchy*:
    - **Display & Section Headers**: `Space Grotesk` (bundled locally) for geometric, engineered titles and widget cards.
    - **Body & Controls**: `Plus Jakarta Sans` (bundled locally) for clean, high-legibility labels and instructions.
    - **Technical Metadata**: Monospace numeric badges for pixel dimensions (`512×512 px`), grid cells (`3×2`), file sizes, and frame intervals.
  - *Component Styling & Layout*:
    - Adaptive canonical layout using `BoxWithConstraints`: on tablets/landscape (`Expanded` / `Medium`), uses a **Navigation Rail** on the left and a side-by-side **Split Studio Layout** (sticky live preview canvas on the left, tabbed configuration inspector on the right). On compact phones, transitions cleanly to a bottom `NavigationBar` and vertically stacked studio sheet.

- **Interactive Feedback & Motion**:
  - Real-time canvas updates as sliders for scale, opacity, corner radius, and X/Y offsets are dragged.
  - Smooth `spring()` transitions between inspector tabs (`Geometry`, `Background`, `Animation`, `SVG Info`) and staggered card reveals in the Widget Gallery.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Vector & SVG Rendering Pipeline (`AndroidVector` + Custom Lightweight SVG Parser/Rasterizer)**
  - *Chosen Approach*: Implement a dedicated, zero-bloat native SVG parser and Canvas renderer supporting `<svg>`, `<g>`, `<path>` (full SVG path commands `M, L, H, V, C, S, Q, T, A, Z`), `<rect>`, `<circle>`, `<ellipse>`, `<line>`, `<polyline>`, `<polygon>`, `<defs>`, `<linearGradient>`, `<radialGradient>`, `<clipPath>`, inline/embedded CSS styles, `viewBox` aspect-ratio preservation, and `<animate>` / `<animateTransform>` / CSS keyframe interpolation. SVGs are rasterized directly into an `ARGB_8888` hardware-compatible `Bitmap` at the exact pixel dimensions reported by `AppWidgetManager.getAppWidgetOptions()` whenever the widget is placed or resized.
  - *Why*: Android's `RemoteViews` `ImageView` does not natively accept raw `.svg` streams at runtime. Rasterizing the vector math dynamically at the exact target widget resolution preserves infinite vector sharpness and full alpha transparency without external WebView overhead or blurry low-res bitmaps.
  - *Alternatives Considered*: Using a background `WebView` to snapshot SVGs (rejected: high RAM footprint, slow initialization, and unreliable in background broadcast receivers).

- **Decision 2: Battery-Safe GIF & Animated SVG Home-Screen Playback**
  - *Chosen Approach*: Decode GIFs using Android's `ImageDecoder` / custom lightweight GIF frame extractor to extract individual alpha-preserving frames and delays into a bounded disk/memory cache. Use `RemoteViews` `ViewFlipper` (which runs natively inside the launcher process with zero wake-locks when frame counts fit within binder limits) combined with an on-demand scheduled frame-stepping fallback for custom frame selection, Play-Once mode, or manual pause/static frame mode, automatically pausing updates when the device enters Power Save / Doze or screen-off state.
  - *Why*: Continuous high-frequency `AppWidgetManager.updateAppWidget` calls drain battery and hit Android `TransactionTooLargeException` binder limits. Combining native `RemoteViews` `ViewFlipper` frame cycling with downsampled `ARGB_8888` frame budgets delivers smooth, transparent animation on the home screen while consuming almost zero background CPU.
  - *Alternatives Considered*: A persistent foreground service pushing bitmaps every 30ms (rejected: violates battery efficiency principles and adds an intrusive notification).

- **Decision 3: 100% Offline Persistence with Room + DataStore + Internal Media Sandbox**
  - *Chosen Approach*: Store structured widget configurations and imported media catalog metadata in a local **Room** database, user studio preferences in **Jetpack DataStore**, and copy/persist URI permissions via `ContentResolver.takePersistableUriPermission` paired with a local internal media cache (`filesDir/kaps_media`).
  - *Why*: Guarantees widgets continue rendering after device reboots (`ACTION_BOOT_COMPLETED`) and launcher restarts even if the user unmounts an SD card or clears recent document providers, while strictly honoring the 100% offline, zero-cloud privacy mandate.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                         KAPS STUDIO UI (Jetpack Compose)                      │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────┐  ┌────────────┐ │
│  │   My Widgets    │  │   Widget Studio  │  │ Image Library │  │  Settings  │ │
│  │   Dashboard     │  │  (Live Preview & │  │ (SAF Import & │  │ (Theme,    │ │
│  │ (Grid & Guide)  │  │   Inspector)     │  │  Starter Art) │  │  Cache,FPS)│ │
│  └────────┬────────┘  └────────┬─────────┘  └───────┬───────┘  └─────┬──────┘ │
└───────────┼────────────────────┼────────────────────┼────────────────┼────────┘
            │                    │                    │                │
            ▼                    ▼                    ▼                ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                     DOMAIN & VIEWMODEL LAYER (StateFlow)                      │
│   CreateWidgetUseCase │ UpdateWidgetUseCase │ ResizeWidgetUseCase │ Validator │
└──────────────────────────────────────┬────────────────────────────────────────┘
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            ▼                                                     ▼
┌───────────────────────────────────────┐   ┌───────────────────────────────────┐
│       DATA & STORAGE LAYER            │   │   NATIVE WIDGET RENDERING ENGINE  │
│ ┌───────────────────────────────────┐ │   │ ┌───────────────────────────────┐ │
│ │ Room Database (WidgetConfig,      │ │   │ │ KapsWidgetProvider            │ │
│ │ MediaItem Entities & DAOs)        │ │   │ │ (AppWidgetProvider + BootRcv) │ │
│ └───────────────────────────────────┘ │   │ └───────────────┬───────────────┘ │
│ ┌───────────────────────────────────┐ │   │                 ▼                 │
│ │ DataStore (Global Studio Settings)│ │   │ ┌───────────────────────────────┐ │
│ └───────────────────────────────────┘ │   │ │ WidgetRenderer Orchestrator   │ │
│ ┌───────────────────────────────────┐ │   │ │ ├─ StaticImageRenderer (PNG)  │ │
│ │ SAF FileAccess & Internal Cache   │ │   │ │ ├─ GifRenderer (Frames/Flip)  │ │
│ │ (Persistable URI + LRU Bitmaps)   │ │   │ │ └─ SvgRenderer (Vector/Anim)  │ │
│ └───────────────────────────────────┘ │   │ └───────────────────────────────┘ │
└───────────────────────────────────────┘   └───────────────────────────────────┘
```

#### Data Model & State

- **`WidgetConfigEntity` (Room)**:
  - `widgetId: Int` (Primary Key — matches Android `appWidgetId` for placed widgets or negative studio preset IDs before placement)
  - `title: String`, `mediaId: Long`, `sourceUri: String`, `mediaFormat: MediaFormat` (`PNG`, `JPEG`, `WEBP`, `BMP`, `GIF_STATIC`, `GIF_ANIMATED`, `WEBP_ANIMATED`, `SVG_STATIC`, `SVG_ANIMATED`)
  - `sizePreset: SizePreset` (`SMALL`, `MEDIUM`, `LARGE`, `EXTRA_LARGE`, `CUSTOM`), `targetWidthDp: Int`, `targetHeightDp: Int`, `maintainAspectRatio: Boolean`
  - `scaleMode: ScaleMode` (`FIT_INSIDE`, `FILL`, `CROP`, `CENTER`, `STRETCH`), `scaleFactor: Float`, `offsetX: Float`, `offsetY: Float`, `opacity: Float`
  - `backgroundMode: BackgroundMode` (`TRANSPARENT`, `ORIGINAL`, `SOLID_COLOR`, `CUSTOM_COLOR`, `ROUNDED`, `NONE`), `backgroundColorHex: Long`, `cornerRadiusDp: Int`, `preserveTransparency: Boolean`
  - `isAnimationEnabled: Boolean`, `animationSpeedMultiplier: Float`, `loopMode: LoopMode` (`CONTINUOUS`, `PLAY_ONCE`), `selectedFrameIndex: Int`
  - `lastUpdated: Long`, `isPlacedOnHomeScreen: Boolean`, `sourceMissing: Boolean`
- **`MediaItemEntity` (Room)**:
  - `id: Long`, `displayName: String`, `uriString: String`, `localCopyPath: String`, `mimeType: String`, `format: MediaFormat`, `widthPx: Int`, `heightPx: Int`, `frameCount: Int`, `hasAlpha: Boolean`, `isBuiltInSample: Boolean`, `svgCompatibilityNotes: String?`
- **`StudioPreferences` (DataStore)**:
  - `themeMode: ThemeMode` (`LIGHT_IPADOS`, `DARK_NOTHING`, `SYSTEM`), `checkerboardContrast: Float`, `maxAnimationFps: Int`, `pauseOnBatterySaver: Boolean`, `maxWidgetResolutionPx: Int`

#### Interactive Component & State Mapping

- **SAF Import & Built-in Samples**: Selecting a file via `ActivityResultContracts.OpenDocument` invokes `ContentResolver.takePersistableUriPermission`, inspects the header bytes/MIME to classify true format (preventing renamed SVG/GIF spoofing), copies a safe offline replica into internal storage, extracts dimensions/alpha/frame count/SVG animation compatibility notes, and opens the Widget Studio.
- **Widget Studio Live Editor**: Modifying any control (size preset, width/height sliders, scale mode, X/Y offset, opacity, background color/corner radius, or animation frame/speed) updates an immutable `WidgetDraftState` in `StudioViewModel` and re-renders the Compose live preview canvas at 60fps over the editor-only checkerboard backdrop.
- **Native Home-Screen Widget Lifecycle (`KapsWidgetProvider` + `WidgetConfigurationActivity`)**:
  - When a user adds KAPS from the Android launcher widget picker, `WidgetConfigurationActivity` launches, allowing the user to pick an existing saved widget preset or create a new one on the spot, then returns `RESULT_OK` with `EXTRA_APPWIDGET_ID`.
  - When the user drags launcher resize handles (`onAppWidgetOptionsChanged`), `KapsWidgetProvider` reads `OPTION_APPWIDGET_MAX_WIDTH` and `OPTION_APPWIDGET_MAX_HEIGHT`, re-rasterizes the SVG or scales the `ARGB_8888` bitmap/frames to match the new dimensions without quality loss, and updates `RemoteViews` with zero click interceptor so the widget remains pure desktop art.
  - On `ACTION_BOOT_COMPLETED` or `ACTION_MY_PACKAGE_REPLACED`, all active `appWidgetIds` are restored from Room and re-rendered automatically.
