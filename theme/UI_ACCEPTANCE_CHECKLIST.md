# Lucid UI acceptance checklist

Use this checklist for every UI build. A visual mockup or successful APK compilation is not equivalent to runtime validation.

## Control Center
- [ ] Matches the chosen Lucid Figma direction without unrelated branding or fake live values.
- [ ] Real Quick Settings tile appears after user adds it; tile opens the Lucid surface.
- [ ] Wi-Fi, Bluetooth, display, sound, and full Settings shortcuts open valid pages on the target ROM.
- [ ] Brightness slider reflects the current value and changes actual brightness.
- [ ] Media-volume slider reflects the current level and changes media volume.
- [ ] Flashlight finds a flash-capable camera, requests permission where needed, and reports failures accurately.
- [ ] Handles camera unavailable, permission denied, write-settings permission denied, and background restrictions.
- [ ] Responsive layout works on the Redmi Note 7 display, portrait/landscape, and large font sizes.

## Settings dashboard
- [ ] Every row maps to a valid platform intent on Android 15.
- [ ] Lucid Wallpapers and Control Center links resolve only when those packages are installed.
- [ ] Unsupported settings are reported clearly, not silently ignored.
- [ ] TalkBack names, focus order, touch targets, contrast, and system-bar insets are correct.
- [ ] No row suggests an option exists when it is only a link to a platform page.

## Packaging and release
- [ ] CI compiles, aligns, signs, and verifies both APKs.
- [ ] Package names and launch activities are verified from built APK metadata.
- [ ] Development APK signatures are not mistaken for the ROM platform signature.
- [ ] APKs are not added to the flashable ROM until permissions and install locations are validated.
- [ ] The custom QS shade is not claimed complete until the matching SystemUI source is patched and device-tested.
- [ ] The full ROM boots repeatedly from cold boot and reboot before a release is called bootable.
