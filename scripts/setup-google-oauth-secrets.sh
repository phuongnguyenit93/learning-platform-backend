#!/usr/bin/env bash
# Optional provisioning for Google OAuth: no credentials in history or env vars.
set -euo pipefail
set +x

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Run with sudo on the Ubuntu A1 host." >&2
  exit 1
fi

secret_dir="${LEARNING_SECRETS_DIR:-/opt/learning-platform/secrets}"
if [[ "$secret_dir" != /* ]]; then
  echo "LEARNING_SECRETS_DIR must be an absolute path." >&2
  exit 1
fi
install -d -o 0 -g 10001 -m 0750 -- "$secret_dir"
umask 077

write_secret() {
  local filename="$1" prompt="$2" value temp
  if ! IFS= read -r -s -p "$prompt: " value </dev/tty; then
    echo >&2
    echo "Could not read $filename" >&2
    return 1
  fi
  echo >&2
  if [[ -z "$value" ]]; then
    echo "Empty value rejected for $filename" >&2
    return 1
  fi
  temp="$(mktemp "$secret_dir/.secret.XXXXXXXX")"
  printf '%s' "$value" > "$temp"
  chown 0:10001 "$temp"
  chmod 0440 "$temp"
  mv -f -- "$temp" "$secret_dir/$filename"
  unset value
  echo "Saved $filename (value hidden)."
}

write_secret spring.security.oauth2.client.registration.google.client-id "Google OAuth Client ID"
write_secret spring.security.oauth2.client.registration.google.client-secret "Google OAuth Client Secret"
echo "OAuth secrets ready. Use the opt-in compose.oauth.yaml overlay to enable login."
