# KnowToMigrate Icons

## Icon Library: Lucide

All KnowToMigrate platforms use **[Lucide Icons](https://lucide.dev)** — a clean, consistent, open-source icon library.

### Core Rules

| Rule | Value |
|------|-------|
| **Library** | Lucide |
| **Stroke width** | `1.75` (always — do not deviate) |
| **Style** | Outline only — never use filled variants |
| **Color** | Inherit from text color or apply explicitly via `currentColor` |

> Using a consistent `strokeWidth = 1.75` across **Web**, **Android**, and **Windows** ensures that icons feel identical on all platforms. This value sits between Lucide's default (2.0) and thin (1.5), giving KnowToMigrate's icons an elegant, premium quality.

---

## Size Scale

| Token | Size | Context |
|-------|------|---------|
| `icon.xs` | 14px / 14dp | Inline badges, status dots |
| `icon.sm` | 16px / 16dp | Compact list rows, secondary actions |
| `icon.md` | 20px / 20dp | Standard UI icons (default) |
| `icon.lg` | 24px / 24dp | Navigation bars, toolbars |
| `icon.xl` | 32px / 32dp | Feature icons, section headers |
| `icon.2xl` | 48px / 48dp | Empty-state illustrations, onboarding |

---

## Platform Integration

### Web (React / Vue / HTML)

Install the Lucide package:

```bash
# React
npm install lucide-react

# Vue
npm install lucide-vue-next

# Vanilla (SVG sprite)
npm install lucide
```

Usage (React):

```tsx
import { Wifi, Send, CheckCircle, AlertCircle } from 'lucide-react';

// Always set strokeWidth to 1.75
<Wifi size={20} strokeWidth={1.75} color="currentColor" />
<Send size={24} strokeWidth={1.75} color="#FF5A00" />
```

Usage (Vue):

```vue
<script setup>
import { WifiIcon } from 'lucide-vue-next'
</script>

<template>
  <WifiIcon :size="20" :stroke-width="1.75" />
</template>
```

Usage (Vanilla SVG):

```html
<svg xmlns="http://www.w3.org/2000/svg"
     width="20" height="20"
     viewBox="0 0 24 24"
     fill="none"
     stroke="currentColor"
     stroke-width="1.75"
     stroke-linecap="round"
     stroke-linejoin="round">
  <!-- Paste lucide path data here -->
</svg>
```

### Android (Jetpack Compose)

Use the `lucide-android` library or import SVG paths manually as `ImageVector`:

```kotlin
// Option A: lucide-android (community library)
// https://github.com/lucide-icons/lucide/tree/main/packages/lucide-android
implementation("com.lucide:lucide-android:0.x.x")

// Usage
import com.lucide.compose.icons.LucideIcons
import com.lucide.compose.icons.lucideicons.Wifi

Icon(
    imageVector = LucideIcons.Wifi,
    contentDescription = "Wi-Fi",
    modifier = Modifier.size(KtmIcons.SizeMd),  // 20.dp
    tint = KtmColors.TextPrimary
)
```

> **Stroke width in Compose:** Lucide-android icons are pre-rendered SVG vectors at strokeWidth=1.75. If you are importing raw SVG paths as `PathData`, set `strokeWidth = 1.75f` in the `VectorPainter`.

### Windows (WinUI 3 / C#)

Convert Lucide SVGs to XAML `PathGeometry` using [Inkscape](https://inkscape.org) or the [SVG-to-XAML converter](https://github.com/BerndK/SvgToXaml):

```xml
<!-- App.xaml — define icon geometries as resources -->
<PathGeometry x:Key="Icon.Wifi"
              Figures="M5 12.55a11..." />

<!-- Usage in XAML -->
<Path Data="{StaticResource Icon.Wifi}"
      Stroke="{StaticResource KtmBrandBrush}"
      StrokeThickness="1.75"
      Width="20" Height="20"
      Stretch="Uniform" />
```

Or use the `WinUI` icon font with a custom icon set compiled from Lucide SVGs:

```xml
<FontIcon FontFamily="{StaticResource KtmIconFont}"
          Glyph="&#xE001;"
          FontSize="20" />
```

---

## Frequently Used Icons

| Purpose | Lucide Icon Name |
|---------|----------------|
| Send file / transfer | `Send` |
| Receive file | `Download` |
| Discover devices | `Wifi`, `Radar` |
| Device — phone | `Smartphone` |
| Device — laptop | `Laptop` |
| Device — desktop | `Monitor` |
| Transfer progress | `ArrowRightLeft` |
| Transfer complete | `CheckCircle` |
| Transfer failed | `AlertCircle` |
| Pause transfer | `PauseCircle` |
| Cancel | `XCircle` |
| Settings | `Settings` |
| Connection | `Link` |
| Disconnect | `Unlink` |
| File | `File` |
| Folder | `Folder` |
| Image | `Image` |
| Video | `Video` |
| Audio | `Music` |
| Speed | `Zap` |
| Security / locked | `Lock` |
| Security / unlocked | `Unlock` |
| History | `History` |
| QR code | `QrCode` |

---

## Do & Don't

| ✅ Do | ❌ Don't |
|------|---------|
| Always use `strokeWidth={1.75}` | Use default `strokeWidth={2}` |
| Use `currentColor` for icon color | Hard-code dark colors on dark backgrounds |
| Scale with token sizes (`icon.md` = 20px) | Use arbitrary pixel sizes |
| Keep icons monochrome unless status-coded | Use multi-color icons in standard UI |
| Use `aria-label` or `aria-hidden` | Leave icons without accessibility attributes |
