# Vizbee - Matter Fork Maintenance and Build Instructions

This file contains instructions for maintaining the Vizbee fork of the `connectedhomeip` (Matter) repository and building the specific libraries used by Vizbee.

## Maintaining the Fork

To keep this fork in sync with the upstream Project CHIP repository, follow these steps:

### 1. Add Upstream Remote (One-time)
```bash
git remote add upstream https://github.com/project-chip/connectedhomeip.git
```

### 2. Sync with Upstream
```bash
# Fetch latest changes
git fetch upstream

# Merge upstream master into your current branch
git rebase upstream/master

# Update submodules (Critical for Matter)
./scripts/checkout_submodules.py --force --recursive
```

---

## iOS / Darwin (MatterTvCastingBridge)

The `MatterTvCastingBridge` is located under `examples/tv-casting-app/darwin`. 

### Build XCFramework
We provide a script to build a universal `.xcframework` that supports both physical iOS devices (`arm64`) and the iOS Simulator (`arm64` and `x86_64`).

**Location:** `examples/tv-casting-app/darwin/build_xcframework.sh`

**Usage:**
```bash
cd examples/tv-casting-app/darwin
chmod +x build_xcframework.sh
./build_xcframework.sh
```

The output will be generated in `examples/tv-casting-app/darwin/output/MatterTvCastingBridge.xcframework`.

---

## Android

Instructions for building Android libraries will be added here.
