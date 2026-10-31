# SELinux integration notes

These are starter policy inputs, not a complete permissive policy. The device/platform policy build must define and validate the domain, socket labeling, service transition, client access, and executable transitions against the exact Android 15 base policy. Do not ship with permissive domains or blanket allow rules. Run sepolicy tests and boot enforcing before marking root available.
