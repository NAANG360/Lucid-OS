# Lucid artwork and RisingOS identity cleanup

## Interim artwork selection
Use the existing Lucid dusk wallpaper as the first candidate for About-device hero art: `theme/assets/lucid-wallpaper-dusk.jpg`. Alternate: `theme/assets/lucid-wallpaper-alpine.jpg`. These are existing repository assets, not newly generated replacements.

Original repository file downloads:
- Dusk: https://raw.githubusercontent.com/NAANG360/Lucid-OS/lucid-ui-foundation/theme/assets/lucid-wallpaper-dusk.jpg
- Alpine: https://raw.githubusercontent.com/NAANG360/Lucid-OS/lucid-ui-foundation/theme/assets/lucid-wallpaper-alpine.jpg

Original Figma prototype: https://www.figma.com/proto/UPUZotxcIfcIdElWklOHFN/cosmosOS?node-id=0-1
Recorded original asset URLs from the connected design context:
- https://www.figma.com/api/mcp/asset/e8a5a225-b6ed-4e0a-b655-6c4e752b3eac.png
- https://www.figma.com/api/mcp/asset/d377ad66-d1ea-4367-81cb-d3085500e23a.png
- https://www.figma.com/api/mcp/asset/8c0dada8-6c3a-414c-b481-5b6c9d1fc5a9.png
- https://www.figma.com/api/mcp/asset/7054e541-d681-42e5-a088-4eac648d2cfc.png

Figma's connected export endpoint was rate-limited previously, so the links above are not claimed to be locally downloaded/verified. If a Figma asset URL requires authentication, open it while signed in or export it from the original prototype. No replacement image was generated.

## Platform-owned Settings work still requiring the exact APK/source
The extracted tree doesn't provide a verified editable matching Android Settings source checkout. The stock About phone hero image and Android-version submenu may live inside a prebuilt Settings APK. Do not blindly replace resources inside a signed APK. Identify the exact APK and resource IDs, scan text/properties/resources and archive contents for RisingOS branding, replace the identified image using matching source or a valid overlay, remove duplicated OS-version rows, rebuild/sign with compatible keys, then boot-test both screens. This document does not claim stock Settings is fixed.
