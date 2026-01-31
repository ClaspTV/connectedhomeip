#!/usr/bin/env bash

#
#    Copyright (c) 2024 Project CHIP Authors
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

set -e

# Configuration
PROJECT_NAME="MatterTvCastingBridge"
SCHEME="MatterTvCastingBridge"
OUTPUT_DIR="out"
IPHONE_OS_DIR="${OUTPUT_DIR}/Release-iphoneos"
IPHONE_SIM_DIR="${OUTPUT_DIR}/Release-iphonesimulator"
XCFRAMEWORK_PATH="${OUTPUT_DIR}/${PROJECT_NAME}.xcframework"

# Ensure we are in the script's directory
cd "$(dirname "$0")/MatterTvCastingBridge"

# Cleanup
rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}"

echo "Building for iOS Device..."
xcodebuild archive \
    -project "${PROJECT_NAME}.xcodeproj" \
    -scheme "${SCHEME}" \
    -archivePath "${IPHONE_OS_DIR}" \
    -sdk iphoneos \
    SKIP_INSTALL=NO \
    BUILD_LIBRARY_FOR_DISTRIBUTION=YES

echo "Building for iOS Simulator..."
xcodebuild archive \
    -project "${PROJECT_NAME}.xcodeproj" \
    -scheme "${SCHEME}" \
    -archivePath "${IPHONE_SIM_DIR}" \
    -sdk iphonesimulator \
    SKIP_INSTALL=NO \
    BUILD_LIBRARY_FOR_DISTRIBUTION=YES

echo "Creating XCFramework..."
xcodebuild -create-xcframework \
    -framework "${IPHONE_OS_DIR}.xcarchive/Products/Library/Frameworks/${PROJECT_NAME}.framework" \
    -framework "${IPHONE_SIM_DIR}.xcarchive/Products/Library/Frameworks/${PROJECT_NAME}.framework" \
    -output "${XCFRAMEWORK_PATH}"

echo "Done! XCFramework created at: ${XCFRAMEWORK_PATH}"
