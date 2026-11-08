#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
export PYTHONPATH="$project_dir/src${PYTHONPATH:+:$PYTHONPATH}"
exec /usr/bin/python3 -B -m beu_connect.setup_profile "$@"
