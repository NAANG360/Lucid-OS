# Lucid Root Provider

Lucid Root is a ROM component, not a setuid shell script. The implementation is split into a native broker, a small `su` client, init service configuration, SELinux policy, and the Lucid Root Manager UI.

## Security model
- Broker starts as root from init and owns a private Unix-domain socket.
- Every request is bound to the kernel-supplied peer UID (SO_PEERCRED); a caller cannot choose its own UID.
- Authorization is deny-by-default and keyed by Android app UID.
- The general Developer Options switch is independent from named app grants. Lucid Desktop has its own grant.
- Revocation blocks subsequent commands. The manager must also stop any active desktop session when revoking Desktop.
- Never install a setuid shell script, chmod a system-wide shell, or trust a caller-provided package name.

## Build integration
This directory contains source and policy inputs. A working build must compile the daemon/client for the target ABI, merge the policy into the platform/device sepolicy build, install the binaries and file contexts, and include the init rc. Merely copying text policy into a prebuilt system image does not update its compiled SELinux policy.

The current Build-3 repack workflow is system-partition-only and does not compile platform sepolicy or rebuild boot/vendor_boot. It must therefore keep root marked unavailable until a full ROM build consumes these inputs and a device test verifies the provider.

## Protocol
The client connects to `/dev/socket/lucid-root`. The broker reads a bounded request containing an argv vector, checks peer credentials and the UID grant file, then forks/execs the requested command. No shell-string interpolation is used. The client forwards stdin/stdout/stderr and returns the child exit status.
