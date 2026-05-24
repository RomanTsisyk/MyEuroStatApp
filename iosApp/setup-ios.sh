#!/usr/bin/env bash
# =============================================================================
# setup-ios.sh — iOS toolchain readiness check for MyEuroStatApp (KMP)
#
# Run this ONCE after installing Xcode.app, before opening the project in Xcode.
# Idempotent: safe to run multiple times.
#
# Usage:
#   chmod +x iosApp/setup-ios.sh
#   cd <repo-root>
#   ./iosApp/setup-ios.sh
# =============================================================================

set -euo pipefail

XCODE_APP="/Applications/Xcode.app"
XCODE_DEV="${XCODE_APP}/Contents/Developer"

echo ""
echo "=== iOS Toolchain Setup Check ==="
echo ""

# -----------------------------------------------------------------------------
# 1. Verify Xcode.app is installed
# -----------------------------------------------------------------------------
echo "[1/4] Checking for Xcode.app at ${XCODE_APP} ..."
if [ ! -d "${XCODE_APP}" ]; then
    echo ""
    echo "  ERROR: Xcode.app not found at ${XCODE_APP}"
    echo ""
    echo "  --> Install Xcode from the App Store:"
    echo "      https://apps.apple.com/app/xcode/id497799835"
    echo ""
    echo "  After installation, re-run this script."
    exit 1
fi
echo "  OK — Xcode.app found."

# -----------------------------------------------------------------------------
# 2. Check current xcode-select path
# -----------------------------------------------------------------------------
echo ""
echo "[2/4] Checking xcode-select path ..."
CURRENT_PATH="$(xcode-select -p 2>/dev/null || true)"
echo "  Current path: ${CURRENT_PATH}"

if [ "${CURRENT_PATH}" != "${XCODE_DEV}" ]; then
    echo ""
    echo "  xcode-select is NOT pointing to Xcode.app."
    echo "  Run the following command to fix it (requires sudo):"
    echo ""
    echo "      sudo xcode-select -s ${XCODE_DEV}"
    echo ""
    echo "  After running that command, re-run this script."
    echo ""
    echo "  NOTE: This script intentionally does NOT run sudo automatically."
    echo "        Copy and run the command above in your terminal."
    exit 1
fi
echo "  OK — xcode-select points to Xcode.app."

# -----------------------------------------------------------------------------
# 3. Accept Xcode license (may be required after fresh install)
# -----------------------------------------------------------------------------
echo ""
echo "[3/4] Verifying Xcode license ..."
if ! xcrun --version &>/dev/null; then
    echo ""
    echo "  Xcode license has not been accepted yet. Run:"
    echo ""
    echo "      sudo xcodebuild -license accept"
    echo ""
    exit 1
fi
echo "  OK — Xcode license accepted."

# -----------------------------------------------------------------------------
# 4. Verify Simulator is accessible
# -----------------------------------------------------------------------------
echo ""
echo "[4/4] Checking available iOS Simulators ..."
if ! xcrun simctl list devices available 2>/dev/null | grep -q "iPhone"; then
    echo ""
    echo "  WARNING: No available iPhone simulators found."
    echo "  Open Xcode → Window → Devices and Simulators → Simulators"
    echo "  and add an iPhone simulator (e.g. iPhone 15, iOS 17)."
else
    echo "  OK — iPhone simulators available:"
    xcrun simctl list devices available 2>/dev/null | grep "iPhone" | head -5
fi

# -----------------------------------------------------------------------------
# Summary
# -----------------------------------------------------------------------------
echo ""
echo "=== Toolchain check PASSED ==="
echo ""
echo "Next steps to get a working iOS build:"
echo ""
echo "  1. Open Xcode."
echo "  2. File → New → Project → iOS → App"
echo "     - Product Name: iosApp"
echo "     - Bundle ID:    eu.eurostat.app"
echo "     - Interface:    SwiftUI"
echo "     - Language:     Swift"
echo "     - Deployment:   iOS 15.0"
echo "     - Save in:      <repo-root>/iosApp/  (overwrites placeholder pbxproj)"
echo ""
echo "  3. Delete auto-generated ContentView.swift and iOSApp.swift stubs."
echo "     Add the existing files from iosApp/iosApp/ (iOSApp.swift, ContentView.swift,"
echo "     Info.plist) and iosApp/Configuration/Config.xcconfig."
echo ""
echo "  4. Build Phases → New Run Script Phase (drag ABOVE 'Compile Sources'):"
echo "     cd \"\$SRCROOT/..\" && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode"
echo ""
echo "  5. Build Settings → Framework Search Paths (Debug & Release):"
echo "     \$(SRCROOT)/../composeApp/build/xcode-frameworks/\$(CONFIGURATION)/\$(SDK_NAME)"
echo ""
echo "  6. Open iosApp/Configuration/Config.xcconfig and set your TEAM_ID."
echo "     (Find it at https://developer.apple.com/account → Membership)"
echo ""
echo "  7. Select an iPhone Simulator and press ⌘R."
echo ""
echo "See iosApp/README.md for the full step-by-step guide."
echo ""
