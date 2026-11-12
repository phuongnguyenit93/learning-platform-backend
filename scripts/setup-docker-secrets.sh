#!/usr/bin/env bash
# Create local file-backed Docker Compose secrets without writing values to
# shell history, Compose YAML, Git or container environment variables.
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

# The container image runs as UID:GID 10001:10001.
# For file-backed Compose secrets, Docker does not remap file ownership;
# root:10001 + mode 0440 permits that non-root process to read them.
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
  echo "Saved $filename (content not displayed)."
}

echo "Enter the Oracle TLS JDBC URL, database username and password."
echo "Input is hidden. Secrets are saved outside the Git checkout."
write_secret spring.datasource.url "Full JDBC URL (jdbc:oracle:thin:@...)"
write_secret spring.datasource.username "Oracle username (e.g. LEARNING_READ)"
write_secret spring.datasource.password "Oracle password"

echo "Complete. Each file is root:10001 with mode 0440."
echo "Docker Compose mounts them read-only under /run/secrets/."
