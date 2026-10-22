# LucidOS Figma branding and RisingOS cleanup

## Branding constraint
Use only the original Lucid/Cosmos Figma assets and exact branding. Do not substitute generated art, stock imagery, or alternate wallpaper concepts. The selected source is the existing Figma prototype:

- Prototype: https://www.figma.com/proto/UPUZotxcIfcIdElWklOHFN/cosmosOS?node-id=0-1
- Original Figma asset URL recorded from the connected design context: https://www.figma.com/api/mcp/asset/e8a5a225-b6ed-4e0a-b655-6c4e752b3eac.png
- Original Figma asset URL recorded from the connected design context: https://www.figma.com/api/mcp/asset/d377ad66-d1ea-4367-81cb-d3085500e23a.png
- Original Figma asset URL recorded from the connected design context: https://www.figma.com/api/mcp/asset/8c0dada8-6c3a-414c-b481-5b6c9d1fc5a9.png
- Original Figma asset URL recorded from the connected design context: https://www.figma.com/api/mcp/asset/7054e541-d681-42e5-a088-4eac648d2cfc.png

The connected Figma export endpoint was rate-limited, so the URLs above are references, not a claim that those image files have been successfully downloaded and bundled. No replacement image has been generated. The Figma export must be retrieved from the original source before inserting artwork into platform Settings.

## Duplicate version label
The custom Lucid Settings About page previously showed Android version alongside the LucidOS version. The Android version row has now been removed from the custom About card so the OS version isn't duplicated there.

## Stock Android Settings cleanup still required
The extracted tree does not contain a verified editable checkout of the matching Android Settings source. The stock About phone hero image and Android-version submenu may be resources inside a prebuilt APK. Identify the exact Settings APK and resource IDs, audit all text/properties/resources/archives for RisingOS branding, replace only identified branding with the original Figma artwork using matching source or a valid overlay, remove duplicate version summaries, rebuild/sign with compatible keys, and boot-test both screens. Do not claim stock Settings is fixed before that build and device test pass.
