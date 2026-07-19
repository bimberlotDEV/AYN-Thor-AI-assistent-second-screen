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
3. Start GameSide and confirm it automatically opens on the lower display.
4. On the lower display, navigate through Ask, Wiki, Saved, Games, and More; enter text, scroll, and rotate/recreate the activity.
5. Start at least three different games on the upper display and record whether Thor removes GameSide below; current user testing says it does for every game.
6. Reopen GameSide and confirm every normal launch targets the lower display without a manual companion step.
7. Test the long Menu shortcut and confirm it targets the lower display.
8. Test Home, Recents, sleep/wake, and lower-display off/on.
9. Confirm the reverse layout—GameSide above and a game below—continues to work as reported.
10. Repeat after process termination and under memory pressure.

Record the Android build number and exact display arrangement for any failure. Do not include personal app data in bug reports.
