# OpenCut for Android

OpenCut is a free, open-source, local-first video editor rewritten natively for Android using Kotlin, Jetpack Compose, Room, and Jetpack Media3.

## Features

- **Workspace & Project Manager**: Create, rename, duplicate, and organize projects stored locally with Room (`16:9`, `9:16`, `1:1`, `4:5`, or custom dimensions) and toggle between Light and Dark studio themes.
- **4-Panel Studio Shell**: Adaptive layout matching OpenCut's Browser, Preview, Inspector, and Timeline workspace across phones and tablets.
- **Browser Panel**: Zero-permission Android Photo Picker (`PickVisualMedia`) for videos and photos, Document Picker for audio, plus Text overlays, Vector Shapes/Stickers, Blur effects, and Timeline Bookmarks.
- **Real-Time Preview**: Synchronized Media3 `ExoPlayer` playback with live keyframe transform interpolation, mask clipping, blur, blend modes, canvas backgrounds, direct drag positioning, preview zoom, and SMPTE frame-accurate transport controls.
- **Tabbed Inspector**: Scrubbable numeric controls (`Pos X/Y`, `Scale X/Y`, `Rotation`, `Opacity`), keyframe toggles, typography & color swatches, volume & speed (with pitch preservation), and mask/blur controls.
- **Frame-Accurate Timeline**: `120,000` ticks/sec precision (`MediaTime`), typed `SceneTracks` (`Overlay`, `Main`, `Audio`), external trim handles, split at playhead, ripple delete, undo/redo history, audio waveform visualization, and zoom.
- **MP4 Export**: Hardware-accelerated H.264/AAC MP4 rendering via Jetpack Media3 `Transformer` with progress tracking and `FileProvider` share support.

## Building

Open in Android Studio or build via Gradle:

```sh
gradle assembleDebug
```
