# KnowToMigrate UI & UX Design Direction (v3.0)
**Prepared for:** Krish  
**Date:** October 22, 2026  
**Status:** Approved Specification & Prototype  

---

## 1. Core Principle: Layered Liquid Glass

Use glass **exclusively for the layer that floats above content** — such as navigation bars, action bars, bottom sheets, floating rails, and active transfer status cards. Keep the underlying content layer calmer, flatter, and mostly solid (`#000000` AMOLED black and `#0C0C0C` surface cards).

> [!IMPORTANT]
> If every surface on the screen is glass, visual hierarchy collapses, readability deteriorates, and the screen becomes muddy and computationally heavy. Glass controls must sit above content, letting calm background content blur gently beneath them.

---

## 2. Color, Contrast & Gloss System

### 2.1 The WCAG AA Contrast Fix
- **The Problem:** White text on `#FF5A00` achieves a contrast ratio of only **3.1:1**, which fails WCAG AA compliance for normal-size text.
- **The Solution:** Near-black labels (`#0A0A0A`) on the KM orange gradient reach a **6.7:1 contrast ratio**, comfortably surpassing WCAG AA standards.
- **Standard Rule:**
  - On orange gradient primary buttons, badges, and active tab lenses: **Use `#0A0A0A` text**.
  - Reserve `#FFFFFF` text for large bold headlines on dark backgrounds.

### 2.2 Gloss & Rim Lighting Recipe
Every glass control is built in four precise optical layers:
1. **Backdrop Blur:** `blur(24px) saturate(180%)` (`-webkit-backdrop-filter` for Web, `RenderEffect` on Android 12+, Mica/Acrylic on Windows).
2. **Tinted Glass Fill:** Low-opacity fill of `rgba(255, 255, 255, 0.035)`.
3. **Top Edge Gloss Highlight:** `inset 0 1px 0 rgba(255, 255, 255, 0.28)` to simulate light catching the top beveled edge.
4. **Perimeter Border:** `1px solid rgba(255, 255, 255, 0.12)` fading naturally into dark shadows.
5. **Elevation Shadow:** Soft dark drop shadow (`0 10px 30px rgba(0, 0, 0, 0.5)`).

---

## 3. Platform Navigation Specifications

### 3.1 Android Native: Floating Pill Bottom Bar
- **Form Factor:** Floating pill-shaped bar positioned 16dp from the bottom and side edges (`height: 68px; border-radius: 34px`).
- **Surface:** Glass fill `rgba(20, 20, 20, 0.55)` with 24px blur and top highlight.
- **Active Tab Lens:** A glossy orange pill lens (`#FF8A00` to `#FF4D00`) with `inset 0 1px 0 rgba(255, 255, 255, 0.5)` that physically slides horizontally between tabs using a spring curve (`cubic-bezier(0.3, 1.35, 0.5, 1)`).
- **Tab Inset:** 4 primary tabs: **Home**, **Send**, **Receive**, **History**. Content scrolls beneath the bar.

### 3.2 Windows Native: Floating Glass Rail
- **Form Factor:** Compact floating glass rail on the left side (`width: 72px; border-radius: 24px`).
- **Icons & Tooltips:** 48×48dp action buttons with Lucide vector icons (`stroke-width: 2`).
- **Actions:** **Home**, **Send**, **Receive**, **Migrate**, **History**, **Settings**.
- **Active State:** Orange gradient fill with `#0A0A0A` icon fill and inset gloss.

### 3.3 Web: Sticky Segmented Top Bar
- Sticky frosted header featuring a segmented pill switcher (`.seg button.on`) to quickly toggle between platform views.

---

## 4. Screen-by-Screen Layout Guide

| Screen | Primary Glass Elements | Content Layer Guidelines |
| :--- | :--- | :--- |
| **Home** | Discovery radar card (`#FF5A00` expanding pulse rings) + Two primary cards: **Send** (KM Orange Gradient) and **Receive** (Glass). | Nearby devices list with **green online dot** (`#22C55E`), never orange. Recent transfers list. |
| **Send** | Selection summary floating pill + Primary `Send [N] items` button. | Checkbox file list with orange check toggles. |
| **Receive** | Centered pairing glass card with pulsing radar and 6-digit PIN code (`482 915`). | "Keep screen open" status text and cancel action. |
| **Transfer** | Active circular progress ring (Android) or linear progress bar (Windows) with real-time MB/s and SHA-256 status. | Clean filename ticker and cancel button. |
| **Migration** | 10-step horizontal pill stepper (`Scan`, `Photos`, `Videos`, `Documents`, `Downloads`, `Music`, `Contacts`, `Apps`, `Preflight`, `Transfer`). | Pre-flight storage check gauge with required vs available disk headroom. |
| **History** | Flat solid table/list (`#0C0C0C`). | **No glass here** — avoids visual clutter. Green `Verified` badges. |
| **Settings** | Toggle switches with orange gradient active thumbs. | Clean rows with clear descriptions. |

---

## 5. Prototype & Assets Reference

An interactive HTML prototype demonstrating both the Android phone form factor and the Windows desktop client has been saved to:
- [`c:\KnowToMigrate\design\prototype\index.html`](file:///c:/KnowToMigrate/design/prototype/index.html)

---

## 6. Implementation Checklist & Priority Order

1. [x] **Tokens Update:** Update `tokens.json`, `tokens.css`, `tokens-android.kt`, and `tokens-windows.cs` with the WCAG AA text contrast (`#0A0A0A`), glass fill `0.035`, and border `0.12`.
2. [x] **Prototype Storage:** Check in the interactive prototype at [`design/prototype/index.html`](file:///c:/KnowToMigrate/design/prototype/index.html).
3. [x] **Remove Misleading Badges:** Remove hard-coded "TLS 1.3 verified" text from `apps/website/src/views/` (replace with `SHA-256 Verified` / `Direct Wi-Fi 6`).
4. [ ] **Navigation Bar Update (Android):** Implement the floating pill navigation bar with sliding lens in [`apps/android`](file:///c:/KnowToMigrate/apps/android).
5. [ ] **Navigation Bar Update (Windows):** Implement the floating rail in [`apps/windows-wpf`](file:///c:/KnowToMigrate/apps/windows-wpf).
