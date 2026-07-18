# Device testing

## Safety and limitations

The Huawei P30 Lite and Android simulated displays are useful development targets, but neither proves that the AYN Thor firmware supports two simultaneously resumed, independently touchable activities. Only the physical Thor can close that release gate.

## Prepare a physical Android device

1. Open **Settings → About phone** and tap **Build number** seven times.
2. Enable **Developer options → USB debugging**.
3. Connect with a USB data cable and approve the computer fingerprint on the device.
4. Verify the connection:

   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb devices -l
   ```

5. Install the debug APK:

   ```powershell
   & $adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

## Huawei P30 Lite checks

Use the Huawei for:

- installation and upgrade;
- the single-screen companion flow;
- rotation and process recreation;
- touch interaction and small-screen layout;
- offline storage, provider, Wiki, privacy, backup, and performance tests.

For a partial multi-display check, enable **Developer options → Simulate secondary displays** and choose an overlay resolution. Confirm that GameSide lists the overlay as a non-primary display and attempts to launch the companion there. An overlay does not validate physical secondary-screen touch routing.

## AYN Thor acceptance gate

1. Start GameSide AI on the primary display.
2. Confirm diagnostics show both physical displays with plausible dimensions and density.
3. Select the lower display and launch the companion.
4. On the lower display, navigate through Ask, Wiki, Saved, Games, and More; enter text, scroll, and rotate/recreate the activity.
5. Add the exact package name of a known installed game to its profile, then use the launch action in Games.
6. Confirm the game appears on the primary display while the companion remains visible and touchable below.
7. Switch focus between both displays and confirm neither task is unexpectedly moved.
8. Disable/re-enable the lower display and confirm GameSide either restores there or offers the single-screen fallback without losing durable state.
9. Repeat after process termination and under memory pressure.

Record the display diagnostics and Android build number for any failure. Do not include personal app data in bug reports.
