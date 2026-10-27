# Lucid Control Centre — implementation notes

## Product direction
A unified Lucid control surface taking interaction cues from Apple Control Center, Xiaomi HyperOS Control Center, and HarmonyOS Control Panel / Super Device. This is an original Lucid design, not a copied skin.

- Compact, readable controls and a clear quick-access hierarchy.
- Larger cards for connectivity, media, brightness, and sound.
- Lucid Connect radial device discovery and interaction.
- Local service discovery via Android Network Service Discovery (mDNS/DNS-SD).
- Device visibility and trust must be separate: discovery is never authorization.
- No cloud account required for local discovery.

## Components
- `theme/lucid-control-centre`: standalone interaction prototype and discovery surface.
- `packages/SystemUI`: ROM-side Quick Settings and notification shade integration.
- `packages/apps/Settings`: Lucid Settings screens, preferences, and device trust controls.
- Future Lucid companion client: authenticated file transfer and collaboration protocols.

## Important integration boundary
An ordinary APK cannot replace Android's privileged SystemUI shade. The prototype opens real Android settings panels for system controls and uses Android's NSD API for local service discovery. Full replacement of the actual Quick Settings UI requires changes to the exact Android base's SystemUI source, default tile specs, resource overlays, permissions, and ROM build configuration.

## Discovery protocol
Initial discovery queries common service types such as HTTP(S), SMB, AirPlay, Google Cast, and companion-link. A result means only that a service advertised itself. It does not guarantee interoperability. Each supported service needs a matching protocol adapter and explicit trust/authentication before exchanging private data. Discovery is limited to the local network; no scanning of public IP ranges.

## Device-circle interaction
The radial layout is the first visual prototype. A production build should model devices and services as typed nodes, support touch drag-and-drop, and route drops to a protocol-specific connector. Unsupported devices should show discovery details rather than a fake connected state.

## Build
GitHub Actions builds a debug-signed APK for review. It is not a privileged/system app and is not a substitute for the integrated ROM build. The APK is a prototype until the SystemUI integration is implemented and tested on the actual target base.