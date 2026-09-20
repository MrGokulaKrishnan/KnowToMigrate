# KnowToMigrate Design System

> **Move Anything. Anywhere. Seamlessly.**  
> Version 1.0.0 — Cross-platform unified design language

---

## Table of Contents

1. [Overview](#overview)
2. [Color System](#color-system)
3. [Typography](#typography)
4. [Spacing](#spacing)
5. [Border Radius](#border-radius)
6. [Shadows & Elevation](#shadows--elevation)
7. [Glass Morphism](#glass-morphism)
8. [Gradients](#gradients)
9. [Animation](#animation)
10. [Icons](#icons)
11. [Components](#components)
12. [Cross-Platform Guidelines](#cross-platform-guidelines)
13. [Accessibility](#accessibility)

---

## Overview

KnowToMigrate is a cross-platform, peer-to-peer file transfer application targeting **Android**, **Windows**, and **Web**. The design system is a single source of truth that ensures visual consistency and an extraordinary user experience across all platforms.

### Design Philosophy

| Principle | Description |
|-----------|-------------|
| **Dark-first** | The UI lives entirely in deep black tones — bright content pops with maximum contrast. |
| **Orange energy** | A vivid `#FF5A00` brand orange signals speed, action, and transfer progress. |
| **Glass morphism** | Frosted, translucent surfaces create depth without heavy chrome. |
| **Motion storytelling** | Animations narrate the data journey — discovery, connection, transfer, completion. |
| **Accessibility** | All text meets WCAG 2.1 AA (4.5:1 contrast), minimum 44px tap targets throughout. |

### Token Files

| Platform | File |
|----------|------|
| JSON (source of truth) | `design/tokens/tokens.json` |
| Web / CSS | `design/tokens/tokens.css` |
| Android (Compose) | `design/tokens/tokens-android.kt` |
| Windows (WinUI 3) | `design/tokens/tokens-windows.cs` |

---

## Color System

### Backgrounds

> [!NOTE]
> Backgrounds use pure black and near-black values to maximize OLED screen savings on Android and deliver a crisp cinema-quality look on Windows.

| Token | Hex | Use |
|-------|-----|-----|
| `background.primary` | `#000000` | App root / screen backgrounds |
| `background.secondary` | `#050505` | Section backgrounds |
| `background.tertiary` | `#080808` | Nested group backgrounds |
| `background.card` | `#0C0C0C` | Opaque cards when glass is disabled |
| `background.elevated` | `#111111` | Sheets, modals, elevated surfaces |

### Brand Colors

| Token | Hex | Use |
|-------|-----|-----|
| `brand.primary` | `#FF5A00` | Primary accent, CTAs, brand marks |
| `brand.gradient[0]` | `#FF4D00` | Gradient start |
| `brand.gradient[4]` | `#FFB066` | Gradient highlight / tinted text |

**Brand Gradient:**
```
linear-gradient(135deg, #FF4D00 0%, #FF5A00 30%, #FF6A00 60%, #FF8A00 100%)
```

### Text Colors

| Token | Hex | Contrast on `#000` | Use |
|-------|-----|---------------------|-----|
| `text.primary` | `#FFFFFF` | 21:1 ✅ | Headings, body content |
| `text.secondary` | `#D4D4D4` | 14.7:1 ✅ | Labels, metadata |
| `text.muted` | `#8A8A8A` | 5.9:1 ✅ | Placeholders, descriptions |
| `text.disabled` | `#555555` | 3.5:1 ⚠️ | Disabled controls only |

### Status Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `status.success` | `#22C55E` | Transfer complete, online devices |
| `status.warning` | `#F59E0B` | Slow connection, battery low |
| `status.error` | `#EF4444` | Transfer failed, disconnected |
| `status.info` | `#38BDF8` | Sync status, informational |

### Glass Colors

Translucent overlays are specified with `rgba()` — these produce the glass morphism effect when combined with `backdrop-filter`.

| Token | Value | Use |
|-------|-------|-----|
| `glass.background` | `rgba(255,255,255,0.04)` | Glass surface fill |
| `glass.border` | `rgba(255,255,255,0.08)` | Card/panel borders |
| `glass.highlight` | `rgba(255,255,255,0.12)` | Gloss top highlight stripe |
| `glass.orangeGlow` | `rgba(255,90,0,0.15)` | Active device tint |
| `glass.orangeGlowStrong` | `rgba(255,90,0,0.25)` | Selected device tint |

---

## Typography

### Font Families

| Role | Font Stack |
|------|-----------|
| **Primary** | `'Inter'` → `'SF Pro Display'` → `'Segoe UI Variable'` → `system-ui` |
| **Monospace** | `'JetBrains Mono'` → `'Cascadia Code'` |

> [!TIP]
> **Platform fallbacks**: On Android, use the system default Roboto/Google Sans until Inter is bundled. On Windows, `Segoe UI Variable` is a first-class fallback.

### Type Scale

| Token | Size | Use |
|-------|------|-----|
| `text.xs` | 11px | Badges, timestamps, legal footnotes |
| `text.sm` | 13px | Secondary labels, metadata rows |
| `text.base` | 15px | Body text, button labels |
| `text.md` | 17px | Subheadings, active tab labels |
| `text.lg` | 20px | Section titles |
| `text.xl` | 24px | Page subtitles |
| `text.2xl` | 30px | Screen titles |
| `text.3xl` | 36px | Large headings |
| `text.4xl` | 48px | Display / feature headings |
| `text.hero` | 64px | Onboarding / marketing hero text |

### Font Weights

| Token | Value | Use |
|-------|-------|-----|
| `regular` | 400 | Body text |
| `medium` | 500 | Labels, nav items |
| `semibold` | 600 | Buttons, subheadings |
| `bold` | 700 | Headings |
| `extrabold` | 800 | Hero, brand text |

### Line Heights

| Token | Value | Use |
|-------|-------|-----|
| `tight` | 1.2 | Large display text |
| `snug` | 1.35 | Headings |
| `normal` | 1.5 | Body text |
| `relaxed` | 1.65 | Multi-line descriptions |

---

## Spacing

All spacing follows a **4px base grid** — every value is a multiple of 4.

| Token | Value | Use |
|-------|-------|-----|
| `space-1` | 4px | Icon gaps, micro-spacing |
| `space-2` | 8px | Tight row spacing |
| `space-3` | 12px | List item padding |
| `space-4` | 16px | Standard inner padding |
| `space-5` | 20px | Card padding |
| `space-6` | 24px | Section gaps |
| `space-8` | 32px | Large section gaps |
| `space-10` | 40px | Vertical section rhythm |
| `space-12` | 48px | Component vertical spacing |
| `space-16` | 64px | Large layout gaps |
| `space-20` | 80px | Page section margins |
| `space-24` | 96px | Maximum section whitespace |

---

## Border Radius

| Token | Value | Use |
|-------|-------|-----|
| `radius.sm` | 8px | Chips, inline badges |
| `radius.md` | 12px | Buttons, inputs, small cards |
| `radius.lg` | 18px | Cards, panels, device tiles |
| `radius.xl` | 24px | Sheets, large modals |
| `radius.2xl` | 32px | Full-width hero cards |
| `radius.full` | 9999px | Pills, avatars, progress bars |

---

## Shadows & Elevation

### Standard Shadows

| Token | Value | Elevation |
|-------|-------|-----------|
| `shadow.sm` | `0 2px 8px rgba(0,0,0,0.4)` | Subtle depth |
| `shadow.md` | `0 4px 16px rgba(0,0,0,0.5)` | Cards |
| `shadow.lg` | `0 8px 32px rgba(0,0,0,0.6)` | Popovers, dropdowns |
| `shadow.xl` | `0 16px 48px rgba(0,0,0,0.7)` | Modals |

### Brand Glow Shadows

> [!IMPORTANT]
> Orange glow shadows should only be applied to **active**, **selected**, or **in-progress** states. Overusing glow degrades its signaling value.

| Token | Value | Use |
|-------|-------|-----|
| `shadow.orangeGlow` | `0 0 24px rgba(255,90,0,0.3), 0 0 48px rgba(255,90,0,0.15)` | Active device cards |
| `shadow.orangeGlowLg` | `0 0 48px rgba(255,90,0,0.4), 0 0 96px rgba(255,90,0,0.2)` | Transfer-in-progress panels |
| `shadow.gloss` | `inset 0 1px 0 rgba(255,255,255,0.12)` | Glass top gloss highlight |

---

## Glass Morphism

KnowToMigrate uses **glass morphism** as its primary surface treatment, creating depth through translucency and blur rather than solid fills.

### Glass Parameters

| Parameter | Value |
|-----------|-------|
| Blur | `16px` |
| Heavy blur | `32px` |
| Saturation boost | `1.2` |

### Glass Surface

```css
background:      rgba(255, 255, 255, 0.04);
border:          1px solid rgba(255, 255, 255, 0.08);
box-shadow:      0 8px 32px rgba(0, 0, 0, 0.5);
backdrop-filter: blur(16px) saturate(1.2);
```

### Glass Card

```css
background:      rgba(255, 255, 255, 0.05);
border:          1px solid rgba(255, 255, 255, 0.10);
box-shadow:      0 4px 16px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.08);
backdrop-filter: blur(16px) saturate(1.2);
border-radius:   18px;
```

### Gloss Highlight

Every glass card should include a top-edge gloss stripe for realism:

```css
/* Pseudo-element overlay */
::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0) 50%);
  pointer-events: none;
}
```

> [!NOTE]
> **Android**: Use `Brush.verticalGradient` overlay inside a `Box` with `graphicsLayer { alpha = ... }`.  
> **Windows**: Use a `LinearGradientBrush` overlay `Rectangle` on top of the glass panel.

---

## Gradients

| Token | Value | Use |
|-------|-------|-----|
| `gradient.brand` | `linear-gradient(135deg, #FF4D00 → #FF8A00)` | Primary buttons, active states |
| `gradient.brandSubtle` | `linear-gradient(135deg, #FF4D0022 → #FF8A0022)` | Background tints, hover overlays |
| `gradient.surface` | `linear-gradient(180deg, #111111 → #080808)` | Screen background texture |
| `gradient.card` | `linear-gradient(145deg, rgba(255,255,255,0.05) → rgba(255,255,255,0.02))` | Card fill |
| `gradient.gloss` | `linear-gradient(180deg, rgba(255,255,255,0.12) → transparent)` | Glass gloss stripe |
| `gradient.successGlow` | `linear-gradient(135deg, #22C55E22 → #22C55E11)` | Success state overlays |

---

## Animation

### Duration Scale

| Token | Value | Use |
|-------|-------|-----|
| `instant` | 80ms | Checkbox toggles, immediate feedback |
| `fast` | 150ms | Hover transitions, icon swaps |
| `normal` | 250ms | Element enter/exit, tab changes |
| `slow` | 400ms | Page transitions, card reveals |
| `slower` | 600ms | Modal open/close |
| `discovery` | 2000ms | Radar pulse rings |
| `pulse` | 1500ms | Heartbeat / liveness pulses |

### Easing Functions

| Token | Value | Feel |
|-------|-------|------|
| `ease.default` | `cubic-bezier(0.4, 0, 0.2, 1)` | Material standard — natural deceleration |
| `ease.spring` | `cubic-bezier(0.34, 1.56, 0.64, 1)` | Slight overshoot — playful, energetic |
| `ease.smooth` | `cubic-bezier(0.25, 0.46, 0.45, 0.94)` | Gentle ease-out — content reveals |
| `ease.decelerate` | `cubic-bezier(0.0, 0.0, 0.2, 1)` | Elements entering the screen |

### Animation Keyframes

All keyframes live in `design/animations/animations.css`. See the file for full definitions.

| Keyframe | Use |
|----------|-----|
| `ktm-pulse-ring` | Discovery radar expanding rings |
| `ktm-lightning-bolt` | Energy transfer animation |
| `ktm-progress-glow` | Progress bar moving glow sweep |
| `ktm-fade-up` | Element entrance from below |
| `ktm-success-burst` | Completion checkmark burst |
| `ktm-device-appear` | Device card appearing in discovery |
| `ktm-data-stream` | Data particle streaming between devices |

---

## Icons

- **Library:** [Lucide Icons](https://lucide.dev)
- **Stroke width:** `1.75` (consistent across all platforms)
- **Style:** Outline only — no filled variants

### Size Scale

| Token | Size | Use |
|-------|------|-----|
| `icon.xs` | 14px | Inline badges, micro indicators |
| `icon.sm` | 16px | List item accessory icons |
| `icon.md` | 20px | Standard UI icons |
| `icon.lg` | 24px | Navigation, toolbar icons |
| `icon.xl` | 32px | Feature icons, empty states |
| `icon.2xl` | 48px | Hero / onboarding icons |

See `design/icons/README.md` for platform-specific integration guides.

---

## Components

### Button

#### Primary

```
Background:    linear-gradient(135deg, #FF4D00, #FF8A00)
Height:        48px
Padding X:     24px
Border Radius: 12px
Font:          600 / 15px
Gloss:         inset 0 1px 0 rgba(255,255,255,0.25), inset 0 -1px 0 rgba(0,0,0,0.2)
Shadow:        0 4px 16px rgba(255,90,0,0.35)
```

#### Secondary

```
Background:    rgba(255,255,255,0.06)
Border:        1px solid rgba(255,255,255,0.12)
Height:        48px
Border Radius: 12px
```

#### Danger

```
Background:    rgba(239,68,68,0.15)
Border:        1px solid rgba(239,68,68,0.3)
```

### Card

```
Background:     rgba(255,255,255,0.04)
Border:         1px solid rgba(255,255,255,0.08)
Border Radius:  18px
Padding:        20px
Backdrop Filter: blur(16px)
Shadow:         0 4px 16px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.06)
```

### Device Card

Device cards represent discovered peers. They pulse orange when active.

```
Border Radius:  18px
Active Border:  1px solid rgba(255,90,0,0.4)
Active Glow:    0 0 24px rgba(255,90,0,0.25)
Animation:      ktm-device-appear on mount
```

### Progress Bar

```
Height:        6px
Border Radius: 9999px
Track:         rgba(255,255,255,0.08)
Fill:          linear-gradient(90deg, #FF4D00, #FF8A00)
Glow:          0 0 12px rgba(255,90,0,0.5)
```

### Input Field

```
Background:    rgba(255,255,255,0.05)
Border:        1px solid rgba(255,255,255,0.10)
Border Radius: 12px
Height:        48px
Focus Border:  1px solid rgba(255,90,0,0.5)
Focus Ring:    0 0 0 3px rgba(255,90,0,0.15)
```

### Badge

```
Border Radius: 9999px
Padding:       3px 10px
Font:          600 / 12px
```

---

## Cross-Platform Guidelines

### Web (HTML/CSS/React/Vue)

- Import `design/tokens/tokens.css` globally.
- Use CSS custom properties (`var(--ktm-*)`) everywhere.
- Apply `backdrop-filter: blur(16px) saturate(1.2)` for glass effects; add `-webkit-backdrop-filter` for Safari.
- Import animations from `design/animations/animations.css`.
- Use `@media (prefers-reduced-motion: reduce)` override block.

### Android (Jetpack Compose)

- Import `com.knowtomigrate.design.tokens.*` from `tokens-android.kt`.
- Use `KtmColors.*` for all `Color` values.
- Apply glass with `Modifier.background(KtmColors.GlassBackground).blur(...)` — note Compose's blur API requires `RenderEffect` on API 31+; fall back to solid `GlassBackground` for older API levels.
- Use `KtmDuration.*` constants with `tween()` animationSpec.
- All touch targets: `Modifier.size(KtmAccessibility.MinTapTarget)` minimum.

### Windows (WinUI 3 / XAML)

- Import `KnowToMigrate.Design.Tokens` namespace from `tokens-windows.cs`.
- Use `AcrylicBrush` with `TintColor = KtmColors.BackgroundElevated` and `TintOpacity = 0.85` for glass surfaces.
- Use `Microsoft.UI.Composition` visual layers for glow shadows.
- Duration helpers: `KtmDuration.Normal` → `TimeSpan` for `Storyboard` animations.
- Set `AutomationProperties.Name` on all interactive controls for accessibility.

---

## Accessibility

> [!CAUTION]
> `text.disabled` (`#555555`) has a contrast ratio of 3.5:1 — **below WCAG AA**. It is intentionally below threshold and must **only** be used for non-interactive, visually disabled controls where the disabled state is communicated via additional means (label, icon, or ARIA attribute).

| Requirement | Value |
|-------------|-------|
| Minimum contrast ratio | 4.5:1 (AA) |
| Focus ring width | 3px |
| Focus ring color | `rgba(255,90,0,0.6)` |
| Minimum tap/click target | 44px × 44px |
| Reduced motion | Respect `prefers-reduced-motion: reduce` |

### Focus Management

All interactive elements must show a focus indicator when navigated by keyboard or switch access:

```css
:focus-visible {
  outline: 3px solid rgba(255, 90, 0, 0.6);
  outline-offset: 2px;
}
```

---

*KnowToMigrate Design System — maintained by the core team. Update `tokens.json` first, then regenerate platform files.*
