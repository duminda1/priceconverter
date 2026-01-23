#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: generate_pins.sh [--out FILE]

Generates OkHttp certificate pins for PocketCurrency endpoints.
Outputs lines in KEY=VALUE format:
- EXCHANGE_RATE_API_PINS
- FRANKFURTER_API_PINS

If --out is provided, writes to that file. Otherwise prints to stdout.

Example:
  ./scripts/generate_pins.sh --out /tmp/pins.env
  PINS_FILE=/tmp/pins.env ./gradlew :app:bundleRelease
USAGE
}

out_file=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out)
      out_file="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      usage
      exit 1
      ;;
  esac
done

pin_for_host() {
  local host="$1"

  # Leaf certificate pin (first cert in the chain).
  local leaf
  leaf=$(echo | openssl s_client -servername "$host" -connect "$host:443" 2>/dev/null \
    | openssl x509 -pubkey -noout \
    | openssl pkey -pubin -outform der \
    | openssl dgst -sha256 -binary \
    | openssl enc -base64)

  # Intermediate certificate pin (second cert in the chain).
  local intermediate
  intermediate=$(echo | openssl s_client -showcerts -servername "$host" -connect "$host:443" 2>/dev/null \
    | awk 'BEGIN{c=0;p=0} /BEGIN CERTIFICATE/{c++; if(c==2) p=1} p{print} /END CERTIFICATE/{if(p){exit}}' \
    | openssl x509 -pubkey -noout \
    | openssl pkey -pubin -outform der \
    | openssl dgst -sha256 -binary \
    | openssl enc -base64)

  echo "sha256/${leaf},sha256/${intermediate}"
}

exchange_pins=$(pin_for_host "api.exchangerate.host")
frankfurter_pins=$(pin_for_host "api.frankfurter.app")

output="EXCHANGE_RATE_API_PINS=${exchange_pins}
FRANKFURTER_API_PINS=${frankfurter_pins}
"

if [[ -n "$out_file" ]]; then
  printf "%s" "$output" > "$out_file"
else
  printf "%s" "$output"
fi
