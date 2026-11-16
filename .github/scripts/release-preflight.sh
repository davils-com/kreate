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

# Decides which publications of a release still have to be uploaded.
#
# The releases repository refuses to overwrite a file, so a release that failed half way cannot
# simply be run again. This script looks at every publication without credentials and sorts it:
#
#   free      nothing of the version exists          -> its upload task is printed
#   complete  the version is listed in the metadata  -> skipped
#   partial   files exist, the metadata does not     -> the script fails
#
# Gradle writes maven-metadata.xml after every file of a publication, so a listed version is a
# finished upload. A partial one has to be deleted by hand, see CONTRIBUTING.md.

set -euo pipefail

repository="${KREATE_REPOSITORY_URL:?KREATE_REPOSITORY_URL is not set}"
version="${KREATE_RELEASE_VERSION:?KREATE_RELEASE_VERSION is not set}"

publications=(
  "com/davils/kreate-detekt-rules|kreate-detekt-rules|:kreate-detekt-rules:publishMavenPublicationToDavilsRepository"
  "com/davils/kreate-settings|kreate-settings|:kreate-settings:publishPluginMavenPublicationToDavilsRepository"
  "com/davils/kreate/settings/com.davils.kreate.settings.gradle.plugin|com.davils.kreate.settings.gradle.plugin|:kreate-settings:publishKreateSettingsPluginMarkerMavenPublicationToDavilsRepository"
  "com/davils/kreate|kreate|:kreate-plugin:publishPluginMavenPublicationToDavilsRepository"
  "com/davils/kreate/com.davils.kreate.gradle.plugin|com.davils.kreate.gradle.plugin|:kreate-plugin:publishKreatePluginMarkerMavenPublicationToDavilsRepository"
)

status_of() {
  curl --silent --show-error --location --output /dev/null --write-out '%{http_code}' --max-time 30 "$1"
}

require_known() {
  case "$1" in
    200|404) ;;
    *)
      echo "::error::$2 answered HTTP $1, so the state of the release cannot be decided."
      exit 1
      ;;
  esac
}

is_listed() {
  local metadata
  metadata="$(curl --silent --show-error --location --fail --max-time 30 "$1")"
  grep --quiet --fixed-strings "<version>$version</version>" <<<"$metadata"
}

tasks=()
partial=()

for publication in "${publications[@]}"; do
  IFS='|' read -r path artifact task <<<"$publication"

  metadata_url="$repository/$path/maven-metadata.xml"
  pom_url="$repository/$path/$version/$artifact-$version.pom"

  metadata_status="$(status_of "$metadata_url")"
  require_known "$metadata_status" "$metadata_url"

  if [ "$metadata_status" = "200" ] && is_listed "$metadata_url"; then
    echo "complete  $artifact $version"
    continue
  fi

  pom_status="$(status_of "$pom_url")"
  require_known "$pom_status" "$pom_url"

  if [ "$pom_status" = "200" ]; then
    echo "partial   $artifact $version"
    partial+=("$path/$version")
    continue
  fi

  echo "free      $artifact $version"
  tasks+=("$task")
done

if [ "${#partial[@]}" -gt 0 ]; then
  echo "::error::A previous upload of $version stopped half way. Delete these directories in the repository, then run this job again: ${partial[*]}"
  exit 1
fi

echo "tasks=${tasks[*]:-}" >>"${GITHUB_OUTPUT:-/dev/stdout}"
