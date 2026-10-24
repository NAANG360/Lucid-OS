# Release and OTA checklist

Use this before publishing a ROM or updater package.

## ROM release

- [ ] Confirm target is `lavender` and the intended Android base.
- [ ] Confirm the workflow produced the integrated ROM artifact—not just a product partition, APK, or add-on ZIP.
- [ ] Run ZIP integrity checks and inspect expected payloads.
- [ ] Calculate SHA-256 from the exact final ZIP.
- [ ] Record the exact byte size.
- [ ] Upload the artifact to a public, non-draft release and verify the asset can be downloaded.
- [ ] Update `ota/stable.json` with the public ROM asset URL, matching SHA-256, exact byte size, version, and concise human-readable changelog.
- [ ] Validate the JSON and fetch the public asset metadata again after publishing.
- [ ] State whether installation and physical-device boot were actually tested.

## Updater release

- [ ] Build the APK and record its version/versionCode.
- [ ] Validate the recovery installer archive and ensure it contains the intended executable `META-INF/com/google/android/update-binary` and installer payload.
- [ ] Run shell syntax checks on shell-based installer logic.
- [ ] Publish the APK, recovery ZIP, and checksum files with clear names.
- [ ] Do not point the ROM OTA manifest at the updater APK or recovery installer.

## Release-note style

Write like a maintainer telling users what they need to know:
- What changed, in plain language.
- What devices/builds it applies to.
- What was tested.
- Known issues and what has not been verified.

Avoid generic claims like “seamless experience,” “robust enhancements,” or “revolutionary performance” unless there is specific evidence. Be direct about failures and limitations.
