# Cosmos Setup Wizard — Reference Prototype

**Status: reference-only; not wired into the launcher build and not merged to `main`.**

This folder preserves the supplied setup wizard code for review and later iteration. It is not yet production-ready or verified against the Cosmos Figma design.

## Review notes before integration

- The Wi-Fi list is hard-coded demo data; tapping a row does not scan, select, or connect to a real network.
- Username/password fields are UI-only. There is no authentication service, validation, or secure credential handling.
- `Settings.Global.DEVICE_PROVISIONED` and `Settings.Secure.USER_SETUP_COMPLETE` generally require privileged/system permissions or suitable platform signing. The catch-all exception handling can silently make setup appear complete when it is not.
- Disabling the activity component is a one-way operation for ordinary users and needs a deliberate recovery path for development builds.
- The Home intent does not guarantee that Cosmos is selected as the default launcher; system role/default-home handling should be verified on the target ROM.
- `FLAG_LAYOUT_NO_LIMITS` can place controls under system bars and cause device-specific layout problems. Prefer edge-to-edge APIs and insets unless immersive overflow is explicitly required.
- The animated screen flow should be tested with process recreation, hardware back, rotation, keyboard visibility, accessibility/reduced motion, and low-memory devices.
- The setup module should only be packaged as a privileged ROM component after its permissions, lifecycle, failure recovery, and provisioning contract are defined.

Keep this prototype isolated until those points and the visual design are reviewed.
