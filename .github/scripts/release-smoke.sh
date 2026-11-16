#!/usr/bin/env bash
#
# Copyright 2026 Davils
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Downloads a published release the way a consumer would, without credentials, and checks that
# every file a build needs is there. The JARs are left in $KREATE_SMOKE_DIRECTORY so that the
# workflow can verify their provenance afterwards.

set -euo pipefail

repository="${KREATE_REPOSITORY_URL:?KREATE_REPOSITORY_URL is not set}"
version="${KREATE_RELEASE_VERSION:?KREATE_RELEASE_VERSION is not set}"
directory="${KREATE_SMOKE_DIRECTORY:?KREATE_SMOKE_DIRECTORY is not set}"

libraries=(
  "com/davils/kreate-detekt-rules|kreate-detekt-rules"
  "com/davils/kreate-settings|kreate-settings"
  "com/davils/kreate|kreate"
)

markers=(
  "com/davils/kreate/settings/com.davils.kreate.settings.gradle.plugin|com.davils.kreate.settings.gradle.plugin"
  "com/davils/kreate/com.davils.kreate.gradle.plugin|com.davils.kreate.gradle.plugin"
)

fetch() {
  local url="$1"
  local target="$directory/$(basename "$url")"

  if ! curl --silent --show-error --location --fail --max-time 60 --output "$target" "$url"; then
    echo "::error::$url is missing from the release."
    exit 1
  fi
  if [ ! -s "$target" ]; then
    echo "::error::$url is empty."
    exit 1
  fi
  echo "found  $url"
}

mkdir -p "$directory"

for library in "${libraries[@]}"; do
  IFS='|' read -r path artifact <<<"$library"
  base="$repository/$path/$version/$artifact-$version"

  for extension in pom pom.asc module module.asc jar jar.asc; do
    fetch "$base.$extension"
  done
done

for marker in "${markers[@]}"; do
  IFS='|' read -r path artifact <<<"$marker"
  base="$repository/$path/$version/$artifact-$version"

  for extension in pom pom.asc; do
    fetch "$base.$extension"
  done
done
