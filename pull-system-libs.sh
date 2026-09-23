#!/usr/bin/env bash

set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
default_out_dir="$(cd "$repo_dir/../../.." && pwd)/out"
out_dir="${1:-${OUT_DIR:-$default_out_dir}}"
system_libs_dir="$repo_dir/system_libs"
debug_runtime_libs_dir="$system_libs_dir/debug-runtime"
settingslib_intermediates="$out_dir/soong/.intermediates/frameworks/base/packages/SettingsLib"

declare -A jars=(
  ["framework.jar"]="$out_dir/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/combined/framework.jar"
)

declare -A debug_runtime_jars=(
  ["SettingsLibSettingsTheme.jar"]="$out_dir/soong/.intermediates/frameworks/base/packages/SettingsLib/SettingsTheme/SettingsLibSettingsTheme/android_common/kotlin/SettingsLibSettingsTheme.jar"
  ["SettingsThemeFlags.jar"]="$out_dir/soong/.intermediates/frameworks/base/aconfig_settingstheme_exported_flags_java_lib/android_common/javac/aconfig_settingstheme_exported_flags_java_lib.jar"
)

missing=0
for name in "${!jars[@]}"; do
  if [[ ! -f "${jars[$name]}" ]]; then
    echo "Missing source jar for $name: ${jars[$name]}" >&2
    missing=1
  fi
done
for name in "${!debug_runtime_jars[@]}"; do
  if [[ ! -f "${debug_runtime_jars[$name]}" ]]; then
    echo "Missing source jar for $name: ${debug_runtime_jars[$name]}" >&2
    missing=1
  fi
done
if [[ ! -d "$settingslib_intermediates" ]]; then
  echo "Missing SettingsLib intermediates directory: $settingslib_intermediates" >&2
  missing=1
fi

if [[ "$missing" -ne 0 ]]; then
  echo "Build framework-minus-apex and SettingsLib first, then rerun this script." >&2
  exit 1
fi

mkdir -p "$system_libs_dir"
mkdir -p "$debug_runtime_libs_dir"

# Keep every SettingsLib output jar available to the standalone Gradle build.
# The outputs include Java API stubs, Kotlin classes, resource R classes, and
# implementation jars from the individual SettingsLib modules. Preserve the
# source-relative path in the filename so different Soong variants cannot
# overwrite each other while all jars remain visible to fileTree("*.jar").
find "$system_libs_dir" -maxdepth 1 -type f -name 'SettingsLib-*.jar' -delete
while IFS= read -r -d '' source; do
  relative="${source#"$settingslib_intermediates/"}"
  destination_name="SettingsLib-${relative//\//__}"
  install -m 0644 "$source" "$system_libs_dir/$destination_name"
done < <(find "$settingslib_intermediates" -type f -name '*.jar' -print0)

for name in "${!jars[@]}"; do
  install -m 0644 "${jars[$name]}" "$system_libs_dir/$name"
done
for name in "${!debug_runtime_jars[@]}"; do
  install -m 0644 "${debug_runtime_jars[$name]}" "$debug_runtime_libs_dir/$name"
done

echo "Populated $system_libs_dir with all SettingsLib jars"
