#!/system/bin/sh
# Root is not safely toggleable by chmod'ing a legacy su binary on /system.
# The old implementation could leave su permanently mode 0000, and remounting
# a verified/dynamic system partition is not a reliable runtime control path.
# Fail closed until a complete root manager/boot-image integration is installed.

TAG="LucidRootGate"
log -t "$TAG" "Root toggle is disabled: no complete, verified root-manager integration is installed."
exit 1
