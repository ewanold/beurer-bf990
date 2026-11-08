#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
profile="$project_dir/database/test-user-2-profile.json"
state="$project_dir/database/test-user-2-state.json"
mkdir -p "$project_dir/database"
if [[ ! -e "$profile" ]]; then
  (set -o noclobber; printf '%s\n' '{"date_of_birth":"1990-01-02","height_cm":170,"sex":"female"}' > "$profile")
fi
exec bash "$project_dir/scripts/setup-profile.sh" --profile "$profile" --state "$state" --expected-index 2 "$@"
