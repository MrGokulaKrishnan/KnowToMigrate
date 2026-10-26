# Issue: UI/UX bugs and debugging notes

Based on the three Android screenshots and a code review. Items marked (code) were confirmed in source. Items marked (screen) come from the screenshots and need a device check.

## Bugs
1. **False encryption label on the Receive screen** (screen). The screenshot shows "End-to-End Encrypted (AES-256-GCM)". Transfers are not encrypted. The current source has no such text (confirmed), so the screenshot came from an older build. Check that the released build matches the source. Fixed in source: "Not enabled yet".
2. **Clipboard pull is reachable without a paired device** (screen + code). The "Pull from PC Clipboard" button is shown while the status says "No Device". The underlying web-share endpoint has no authentication. Fix the endpoint first (pairing token), then hide the card until a paired device is present.
3. **Two competing primary actions on Home** (screen). "Launch Transfer" and "Full Device Migration" both look primary. Keep one gradient button and make Move device a secondary row.
4. **Discovery never ends** (screen). "Looking for devices on your network..." has no timeout and no failure state. Add a 30 s timeout with "No devices found. Check both devices are on the same Wi-Fi" and a retry action.
5. **Technical detail shown to everyone** (screen). The Receive screen shows the raw device ID `ktm-and-samsung-sm-s928b-337629`, and "Advanced: Direct IP" sits at the same level as discovery. Move the ID under Details and the IP connection under Advanced.
6. **Jargon navigation** (screen + code). Command, Launch, Landing, Ledger, Control. Renamed to Home, Send, Receive, History, Settings in the naming plan.
7. **"Pluto Engine" in status and settings** (code). Renamed to Transit Engine throughout.
8. **Save destination wraps badly** (screen). "Downloads /" breaks onto its own line. Show the path on one line with ellipsis and put the full path in Details.
9. **Icons without a content description** (code, accessibility). Six call sites:
   - `ui/screens/SendScreen.kt:369`
   - `ui/screens/SendScreen.kt:442`
   - `ui/screens/ReceiveScreen.kt:429`
   - `ui/screens/HistoryScreen.kt:259`
   - `ui/screens/SettingsScreen.kt:267`
   - `ui/screens/HomeScreen.kt:420`
   Decorative icons should use `contentDescription = null`. Icons that carry meaning need a description.
10. **Radar animates forever** (screen + code). The Scanning radar loops with no reduce-motion check. Stop it when the system asks for reduced motion, and pause it when the screen is not visible.
11. **Website claim that is not verified** (code). `TransferView.tsx:97` shows "Direct Wi-Fi 6". Nothing in the code measures this. Remove or qualify it.
12. **Website JS in one 340 KB chunk** (performance). Split each page with `React.lazy`, so the first load fetches only the home page.

## Debug notes
- Reproduce the Receive screen on a device running Android 8 and on one running Android 14, and check the 200% font-scale layout.
- Check the Windows Receive screen with Narrator and Tab-only navigation.
- Confirm the released APK's Receive screen has no encryption label.
