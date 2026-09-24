# KnowToMigrate Android Crash Analysis

## Device

- **Device**: Samsung Galaxy S21 Ultra 5G (`SM-G998B`, product `p3sxxx`, device `p3s`)
- **Android version**: 15 (One UI 7)
- **API**: 35
- **CPU ABI**: `arm64-v8a`
- **Build ID**: `samsung/p3sxxx/p3s:15/AP3A.240905.015.A2/G998BXXSJHZC2:user/release-keys`

## APK

- **Version**: 1.0.0 (versionCode 1)
- **Build type**: release
- **APK size**: 11,343,808 bytes (10.8 MB)
- **SHA-256**: `F28F058B530A205068FC4C476344A67C6E17B3AEB55CE060399D0DCAEF855EA1`

## Original Crash

- **Exception**: `java.lang.IllegalArgumentException`
- **Message**: `ending radius must be > 0`
- **Thread**: `main` (Looper/Choreographer UI thread)
- **Stack trace**:
```
java.lang.IllegalArgumentException: ending radius must be > 0
	at android.graphics.RadialGradient.<init>(RadialGradient.java:162)
	at android.graphics.RadialGradient.<init>(RadialGradient.java:73)
	at androidx.compose.ui.graphics.AndroidShader_androidKt.ActualRadialGradientShader-8uybcMk(AndroidShader.android.kt:59)
	at androidx.compose.ui.graphics.ShaderKt.RadialGradientShader-8uybcMk(Shader.kt:81)
	at androidx.compose.ui.graphics.RadialGradient.createShader-uvyYCjk(Brush.kt:536)
	at androidx.compose.ui.graphics.ShaderBrush.applyTo-Pq9zytI(Brush.kt:661)
	at androidx.compose.ui.graphics.drawscope.CanvasDrawScope.configurePaint-swdJneE(CanvasDrawScope.kt:620)
	at androidx.compose.ui.graphics.drawscope.CanvasDrawScope.configurePaint-swdJneE$default(CanvasDrawScope.kt:611)
	at androidx.compose.ui.graphics.drawscope.CanvasDrawScope.drawCircle-V9BoPsw(CanvasDrawScope.kt:327)
	at androidx.compose.ui.node.LayoutNodeDrawScope.drawCircle-V9BoPsw(LayoutNodeDrawScope.kt:13)
	at androidx.compose.ui.graphics.drawscope.DrawScope.drawCircle-V9BoPsw$default(DrawScope.kt:676)
	at com.knowtomigrate.app.ui.components.KmComponentsKt.KmDiscoveryRadar$lambda$19$lambda$18$lambda$17$drawRing(KmComponents.kt:310)
	at com.knowtomigrate.app.ui.components.KmComponentsKt.KmDiscoveryRadar$lambda$19$lambda$18$lambda$17(KmComponents.kt:321)
	at androidx.compose.ui.draw.DrawBackgroundModifier.draw(DrawModifier.kt:127)
	at androidx.compose.ui.node.LayoutNodeDrawScope.drawDirect-eZhPAX0$ui_release(LayoutNodeDrawScope.kt:110)
	at androidx.compose.ui.node.LayoutNodeDrawScope.draw-eZhPAX0$ui_release(LayoutNodeDrawScope.kt:89)
	at androidx.compose.ui.node.LayoutNode.draw$ui_release(LayoutNode.kt:1000)
	at androidx.compose.ui.platform.AndroidComposeView.dispatchDraw(AndroidComposeView.android.kt:1564)
	at android.view.View.draw(View.java:26151)
	at android.view.ThreadedRenderer.draw(ThreadedRenderer.java:841)
	at android.view.ViewRootImpl.draw(ViewRootImpl.java:6755)
	at android.view.ViewRootImpl.performDraw(ViewRootImpl.java:6372)
	at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:5282)
	at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:3708)
	at android.view.Choreographer.doFrame(Choreographer.java:1142)
	at android.os.Handler.handleCallback(Handler.java:959)
	at android.os.Looper.loopOnce(Looper.java:257)
	at android.os.Looper.loop(Looper.java:342)
	at android.app.ActivityThread.main(ActivityThread.java:9634)
```

## Root Cause

In `HomeScreen.kt`, the discovery radar component `KmDiscoveryRadar` (`KmComponents.kt`) animates 3 radar expansion rings:
```kotlin
val ring1 = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, ...)
val ring2 = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, ...)
val ring3 = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, ...)
```
Inside the canvas drawing loop:
```kotlin
fun drawRing(progress: Float) {
    val radius = maxRadius * progress
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(KmOrange.copy(alpha = alpha * 0.1f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}
```
At frame 0 of the animation (immediately when `HomeScreen` first renders during cold start), `progress = 0f`.
Therefore, `radius = maxRadius * 0f = 0.0f`.
The Compose `Brush.radialGradient` implementation directly instantiates the Android framework shader:
`android.graphics.RadialGradient.<init>(float x, float y, float radius, ...)`
On Android (strictly enforced in Android 15), line 162 of `RadialGradient.java` checks:
```java
if (radius <= 0) {
    throw new IllegalArgumentException("ending radius must be > 0");
}
```
Because the exception occurred inside the Android Choreographer display frame loop on the main thread, it crashed the application process before the first frame completed rendering, prompting the Samsung/Android OS system dialog `"Something went wrong with KnowToMigrate / KnowToMigrate closed because this app has a bug"`.

## Fix

### 1. `apps/android/app/src/main/java/com/knowtomigrate/app/ui/components/KmComponents.kt`
- Added strict radius guard in `drawRing`:
```kotlin
fun drawRing(progress: Float) {
    val radius = maxRadius * progress
    if (radius <= 1f) return // Guard: RadialGradient requires ending radius > 0!
    val alpha = (1f - progress).coerceIn(0f, 1f)
    drawCircle(
        color = KmOrange.copy(alpha = alpha * 0.5f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(KmOrange.copy(alpha = alpha * 0.1f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}
```
- Added positive radius guard for center dot and glow shaders (`dotRadius > 0f` and `glowRadius > 0f`).

### 2. `apps/android/app/src/main/java/com/knowtomigrate/app/ui/screens/SplashScreen.kt`
- Added `if (radius > 1f)` guard around the radial gradient background pulse to prevent any 0-sized layout pass from triggering `RadialGradient` exceptions.

## Verification

Live physical testing performed on **Samsung Galaxy S21 Ultra 5G (Android 15 / API 35 / ARM64-v8a)**:

- **Fresh install**: `PASS` (`Performing Streamed Install` -> `Success`)
- **Cold start**: `PASS` (`am start` launches cleanly into `MainActivity`)
- **Repeated launch (Stress Test)**: `10/10 PASS`
  - Test 1/10: PASS (pid: 30702)
  - Test 2/10: PASS (pid: 30800)
  - Test 3/10: PASS (pid: 30878)
  - Test 4/10: PASS (pid: 31022)
  - Test 5/10: PASS (pid: 31109)
  - Test 6/10: PASS (pid: 31223)
  - Test 7/10: PASS (pid: 31329)
  - Test 8/10: PASS (pid: 31424)
  - Test 9/10: PASS (pid: 31516)
  - Test 10/10: PASS (pid: 31588)
- **Startup animation**: `PASS` (Smooth black -> logo fade & energy glow -> home transition)
- **Home screen**: `PASS` (Rendered cleanly with AMOLED black, orange buttons, discovery radar)
- **Discovery**: `PASS` (Auto-discovered host PC `KRISH-THINKPAD` at `192.168.31.33` on Wi-Fi port 54123)
- **Transfer / Send UI**: `PASS` (File staging and peer selection fully functional)

## Remaining Issues

**NONE**. Zero crashes across 10 repeated cold launches on physical Android 15 hardware.
Web release deployed and live at `https://knowtomigrate.web.app`.
